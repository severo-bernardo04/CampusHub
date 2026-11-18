package com.example.campushub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.domain.CapacityPolicy
import com.example.campushub.domain.EnrollmentPolicy
import com.example.campushub.domain.EnrollmentState
import com.example.campushub.domain.PostPolicy
import com.example.campushub.domain.RatingAccess
import com.example.campushub.domain.RatingPolicy
import com.example.campushub.domain.RatingSummary
import com.example.campushub.domain.RegistrationStatus
import com.example.campushub.model.AppUser
import com.example.campushub.model.Event
import com.example.campushub.model.Registration
import com.example.campushub.model.RegistrationUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String

    private lateinit var mapView: MapView
    private lateinit var mapCard: View
    private lateinit var directionsButton: Button
    private lateinit var favoriteButton: Button
    private lateinit var enrollButton: Button
    private lateinit var qrButton: Button
    private lateinit var capacityText: TextView
    private lateinit var detailImage: ImageView
    private lateinit var ratingSummary: TextView
    private lateinit var ratingHint: TextView
    private lateinit var ratingInput: LinearLayout
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingSubmitButton: Button

    private var event: Event? = null
    private var registration: Registration? = null
    private var userLoaded = false
    private var myCourse = ""
    private var myName = ""
    private var favorite = false
    private var myStars = 0
    private var ratingSummaryData = RatingSummary(0.0, 0)

    private val enrolled: Boolean
        get() = registration?.status == RegistrationStatus.CONFIRMED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureMap()
        setContentView(R.layout.activity_event_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        bindViews()

        eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        directionsButton.setOnClickListener { openDirections() }
        favoriteButton.setOnClickListener { toggleFavorite() }
        enrollButton.setOnClickListener { onEnrollClicked() }
        ratingSubmitButton.setOnClickListener { submitRating() }

        qrButton.setOnClickListener {
            startActivity(Intent(this, QrCodeActivity::class.java).putExtra("eventId", eventId))
        }
        findViewById<Button>(R.id.commentsButton).setOnClickListener {
            startActivity(Intent(this, CommentsActivity::class.java).putExtra("eventId", eventId))
        }
        findViewById<Button>(R.id.mediaButton).setOnClickListener {
            startActivity(Intent(this, MediaActivity::class.java).putExtra("eventId", eventId))
        }

        loadEvent()
        loadUser()
        loadRegistration()
        loadFavorite()
        loadRatings()
    }

    private fun configureMap() {
        val config = Configuration.getInstance()
        config.load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        // O OpenStreetMap exige um User-Agent que identifique o app e como contatar o responsável.
        // Troque SEU_EMAIL pelo seu e-mail real.
        config.userAgentValue = "CampusHub/1.0 (projeto academico URI Santiago; contato: 109827@urisantiago.br)"

        // Pasta nova de cache: descarta os blocos "Access blocked" já gravados
        config.osmdroidBasePath = File(cacheDir, "osmdroid_v2")
        config.osmdroidTileCache = File(cacheDir, "osmdroid_v2/tiles")
    }

    private fun bindViews() {
        mapView = findViewById(R.id.mapView)
        mapCard = findViewById(R.id.mapCard)
        directionsButton = findViewById(R.id.directionsButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        enrollButton = findViewById(R.id.enrollButton)
        qrButton = findViewById(R.id.qrButton)
        capacityText = findViewById(R.id.detailCapacity)
        detailImage = findViewById(R.id.detailImage)
        ratingSummary = findViewById(R.id.ratingSummary)
        ratingHint = findViewById(R.id.ratingHint)
        ratingInput = findViewById(R.id.ratingInput)
        ratingBar = findViewById(R.id.ratingBar)
        ratingSubmitButton = findViewById(R.id.ratingSubmitButton)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        // Permite arrastar o mapa dentro do ScrollView
        mapView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    // ---------- Carregamento ----------

    private fun loadEvent() {
        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            val loaded = doc.toObject(Event::class.java) ?: return@addOnSuccessListener
            loaded.id = doc.id
            event = loaded
            bindEvent(loaded)
            refreshActions()
            refreshRating()
        }
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val user = doc.toObject(AppUser::class.java)
            myCourse = user?.course ?: ""
            myName = user?.name ?: ""
            userLoaded = true
            refreshActions()
        }
    }

    private fun loadRegistration() {
        val uid = auth.currentUser?.uid ?: return
        RegistrationRepository.loadOwn(db, uid, eventId) {
            registration = it
            refreshActions()
            refreshRating()
        }
    }

    private fun loadFavorite() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites").document(eventId)
            .get().addOnSuccessListener { doc ->
                favorite = doc.exists()
                refreshActions()
            }
    }

    private fun loadRatings() {
        val uid = auth.currentUser?.uid
        db.collection("events").document(eventId).collection("ratings").get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.map { it.id to (it.getLong("stars") ?: 0L).toInt() }
                ratingSummaryData = RatingPolicy.summarize(ratings.map { it.second })
                myStars = ratings
                    .firstOrNull { it.first == uid && RatingPolicy.isValid(it.second) }
                    ?.second ?: 0
                refreshRating()
            }
    }

    private fun reload() {
        loadEvent()
        loadRegistration()
    }

    // ---------- Exibição ----------

    private fun bindEvent(ev: Event) {
        findViewById<TextView>(R.id.detailTitle).text = ev.title
        findViewById<TextView>(R.id.detailDescription).text = ev.description
        findViewById<TextView>(R.id.detailDate).text = ev.formattedDate()
        findViewById<TextView>(R.id.detailLocation).text = ev.location
        findViewById<TextView>(R.id.detailAddress).text = ev.address
        findViewById<TextView>(R.id.detailCourse).text = ev.hostCourse

        val bitmap = ImageCodec.decode(ev.imageBase64, 900)
        detailImage.visibility = if (bitmap != null) View.VISIBLE else View.GONE
        detailImage.setImageBitmap(bitmap)

        val category = findViewById<TextView>(R.id.detailCategory)
        category.text = ev.category
        category.visibility = if (ev.category.isEmpty()) View.GONE else View.VISIBLE

        val access = findViewById<TextView>(R.id.detailAccess)
        if (ev.isPublic) {
            access.text = "Aberto ao público"
            access.setBackgroundResource(R.drawable.bg_badge_public)
            access.setTextColor(getColor(R.color.success))
        } else {
            access.text = "Fechado ao curso"
            access.setBackgroundResource(R.drawable.bg_badge_closed)
            access.setTextColor(getColor(R.color.warning))
        }

        val status = findViewById<TextView>(R.id.detailStatus)
        if (ev.isFinished()) {
            status.text = "Encerrado"
            status.setBackgroundResource(R.drawable.bg_badge_neutral)
            status.setTextColor(getColor(R.color.text_secondary))
        } else {
            status.text = "Próximo"
            status.setBackgroundResource(R.drawable.bg_badge_public)
            status.setTextColor(getColor(R.color.success))
        }

        capacityText.text = listOf(
            CapacityPolicy.occupancyLabel(ev),
            CapacityPolicy.waitlistLabel(ev)
        ).filter { it.isNotEmpty() }.joinToString("\n")

        showMap(ev)
    }

    private fun showMap(ev: Event) {
        val hasLocation = ev.latitude != 0.0 || ev.longitude != 0.0
        val visibility = if (hasLocation) View.VISIBLE else View.GONE
        mapCard.visibility = visibility
        directionsButton.visibility = visibility
        if (!hasLocation) return

        val point = GeoPoint(ev.latitude, ev.longitude)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(point)

        val marker = Marker(mapView).apply {
            position = point
            title = ev.location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.clear()
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun openDirections() {
        val ev = event ?: return
        val uri = Uri.parse(
            "geo:${ev.latitude},${ev.longitude}?q=${ev.latitude},${ev.longitude}(${Uri.encode(ev.location)})"
        )
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            toast("Nenhum aplicativo de mapas encontrado")
        }
    }

    private fun currentState(): EnrollmentState? {
        val ev = event ?: return null
        return EnrollmentPolicy.stateFor(
            ev, userLoaded, myCourse, registration?.status, auth.currentUser?.uid.orEmpty()
        )
    }

    private fun refreshActions() {
        val ev = event ?: return
        favoriteButton.text = if (favorite) "♥ Favoritado" else "♡ Favoritar"
        qrButton.visibility = if (enrolled) View.VISIBLE else View.GONE

        when (currentState()) {
            EnrollmentState.FINISHED ->
                styleEnroll("Evento encerrado", enabled = false, danger = false)
            EnrollmentState.NOT_OPEN ->
                styleEnroll("Evento ainda não publicado", enabled = false, danger = false)
            EnrollmentState.LOADING, null ->
                styleEnroll("Carregando…", enabled = false, danger = false)
            EnrollmentState.CLOSED_FOR_COURSE ->
                styleEnroll("Fechado para o seu curso", enabled = false, danger = false)
            EnrollmentState.CAN_CANCEL ->
                styleEnroll("Cancelar inscrição", enabled = true, danger = true)
            EnrollmentState.CAN_ENROLL ->
                styleEnroll("Inscrever-se", enabled = true, danger = false)
            EnrollmentState.FULL_CAN_JOIN_WAITLIST ->
                styleEnroll("Entrar na lista de espera", enabled = true, danger = false)
            EnrollmentState.OFFER_AVAILABLE ->
                styleEnroll("Vaga disponível: confirmar inscrição", enabled = true, danger = false)
            EnrollmentState.ON_WAITLIST -> {
                val place = CapacityPolicy.waitlistPosition(ev, auth.currentUser?.uid.orEmpty())
                val suffix = if (place != null) " (${place}º da fila)" else ""
                styleEnroll("Sair da lista de espera$suffix", enabled = true, danger = true)
            }
        }
    }

    private fun styleEnroll(text: String, enabled: Boolean, danger: Boolean) {
        enrollButton.text = text
        enrollButton.isEnabled = enabled
        if (danger) {
            enrollButton.setBackgroundResource(R.drawable.bg_button_danger)
            enrollButton.setTextColor(getColor(R.color.danger))
        } else {
            enrollButton.setBackgroundResource(R.drawable.bg_button)
            enrollButton.setTextColor(
                getColor(if (enabled) R.color.white else R.color.text_secondary)
            )
        }
    }

    private fun refreshRating() {
        val ev = event ?: return

        ratingSummary.text = ratingSummaryData.label()

        when (RatingPolicy.accessFor(ev, enrolled)) {
            RatingAccess.NOT_FINISHED -> {
                ratingHint.text = "A avaliação fica disponível depois que o evento terminar."
                ratingHint.visibility = View.VISIBLE
                ratingInput.visibility = View.GONE
            }
            RatingAccess.NOT_ENROLLED -> {
                ratingHint.text = "Somente alunos inscritos podem avaliar este evento."
                ratingHint.visibility = View.VISIBLE
                ratingInput.visibility = View.GONE
            }
            RatingAccess.CAN_RATE -> {
                ratingHint.visibility = View.GONE
                ratingInput.visibility = View.VISIBLE
                ratingBar.rating = myStars.toFloat()
                ratingSubmitButton.text =
                    if (myStars > 0) "Atualizar minha avaliação" else "Enviar avaliação"
            }
        }
    }

    // ---------- Ações ----------

    private fun toggleFavorite() {
        val uid = auth.currentUser?.uid ?: return
        val newValue = !favorite
        FavoriteRepository.set(db, uid, eventId, newValue) {
            favorite = newValue
            refreshActions()
        }
    }

    private fun onEnrollClicked() {
        val user = auth.currentUser ?: return
        val me = RegistrationUser(
            uid = user.uid,
            name = PostPolicy.displayName(myName, user.email),
            email = user.email.orEmpty()
        )

        when (currentState()) {
            EnrollmentState.CAN_ENROLL, EnrollmentState.FULL_CAN_JOIN_WAITLIST ->
                RegistrationRepository.enroll(db, me, eventId) { result ->
                    handle(result) {
                        if (it == RegistrationStatus.CONFIRMED) "Inscrição confirmada!"
                        else "Você entrou na lista de espera"
                    }
                }
            EnrollmentState.CAN_CANCEL, EnrollmentState.ON_WAITLIST ->
                RegistrationRepository.cancel(db, me.uid, eventId) { result ->
                    handle(result) { "Inscrição cancelada" }
                }
            EnrollmentState.OFFER_AVAILABLE ->
                RegistrationRepository.confirmFromWaitlist(db, me.uid, eventId) { result ->
                    handle(result) { "Inscrição confirmada!" }
                }
            else -> Unit
        }
    }

    private fun handle(result: Result<String>, successMessage: (String) -> String) {
        result
            .onSuccess { toast(successMessage(it)) }
            .onFailure { toast(it.message) }
        reload()
    }

    private fun submitRating() {
        val uid = auth.currentUser?.uid ?: return
        val stars = ratingBar.rating.toInt()
        if (!RatingPolicy.isValid(stars)) {
            toast("Escolha de 1 a 5 estrelas")
            return
        }

        // O ID do documento é o uid: garante uma única avaliação por aluno
        db.collection("events").document(eventId).collection("ratings").document(uid)
            .set(
                mapOf(
                    "userId" to uid,
                    "stars" to stars,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                toast("Avaliação salva")
                loadRatings()
            }
            .addOnFailureListener { toast(it.message) }
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message ?: "Erro", Toast.LENGTH_LONG).show()
    }

    // ---------- Ciclo de vida do mapa ----------

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}