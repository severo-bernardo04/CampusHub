package com.example.campushub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.model.AppUser
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String

    private lateinit var mapView: MapView
    private lateinit var mapCard: View
    private lateinit var directionsButton: Button
    private lateinit var favoriteButton: Button
    private lateinit var enrollButton: Button
    private lateinit var ratingSummary: TextView
    private lateinit var ratingHint: TextView
    private lateinit var ratingInput: LinearLayout
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingSubmitButton: Button

    private var event: Event? = null
    private var userLoaded = false
    private var myCourse = ""
    private var enrolled = false
    private var favorite = false
    private var myStars = 0
    private var ratingAvg = 0.0
    private var ratingCount = 0

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
        enrollButton.setOnClickListener { toggleEnrollment() }
        ratingSubmitButton.setOnClickListener { submitRating() }

        findViewById<Button>(R.id.commentsButton).setOnClickListener {
            startActivity(Intent(this, CommentsActivity::class.java).putExtra("eventId", eventId))
        }
        findViewById<Button>(R.id.mediaButton).setOnClickListener {
            startActivity(Intent(this, MediaActivity::class.java).putExtra("eventId", eventId))
        }

        loadEvent()
        loadUser()
        loadEnrollment()
        loadFavorite()
        loadRatings()
    }

    private fun configureMap() {
        val config = Configuration.getInstance()
        config.load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        config.userAgentValue = packageName
        config.osmdroidBasePath = File(cacheDir, "osmdroid")
        config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }

    private fun bindViews() {
        mapView = findViewById(R.id.mapView)
        mapCard = findViewById(R.id.mapCard)
        directionsButton = findViewById(R.id.directionsButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        enrollButton = findViewById(R.id.enrollButton)
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
            myCourse = doc.toObject(AppUser::class.java)?.course ?: ""
            userLoaded = true
            refreshActions()
        }
    }

    private fun loadEnrollment() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("enrollments").document(eventId)
            .get().addOnSuccessListener { doc ->
                enrolled = doc.exists()
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
                var sum = 0L
                var count = 0
                myStars = 0
                snapshot.documents.forEach { doc ->
                    val stars = (doc.getLong("stars") ?: 0L).toInt()
                    if (stars in 1..5) {
                        sum += stars
                        count++
                        if (doc.id == uid) myStars = stars
                    }
                }
                ratingCount = count
                ratingAvg = if (count > 0) sum.toDouble() / count else 0.0
                refreshRating()
            }
    }

    // ---------- Exibição ----------

    private fun bindEvent(ev: Event) {
        findViewById<TextView>(R.id.detailTitle).text = ev.title
        findViewById<TextView>(R.id.detailDescription).text = ev.description
        findViewById<TextView>(R.id.detailDate).text = ev.formattedDate()
        findViewById<TextView>(R.id.detailLocation).text = ev.location
        findViewById<TextView>(R.id.detailAddress).text = ev.address
        findViewById<TextView>(R.id.detailCourse).text = ev.hostCourse

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

    private fun refreshActions() {
        val ev = event ?: return

        favoriteButton.text = if (favorite) "♥ Favoritado" else "♡ Favoritar"

        val canEnroll = ev.isPublic || myCourse == ev.hostCourse
        when {
            ev.isFinished() -> styleEnroll("Evento encerrado", enabled = false, danger = false)
            !userLoaded -> styleEnroll("Carregando…", enabled = false, danger = false)
            !canEnroll -> styleEnroll("Fechado para o seu curso", enabled = false, danger = false)
            enrolled -> styleEnroll("Cancelar inscrição", enabled = true, danger = true)
            else -> styleEnroll("Inscrever-se", enabled = true, danger = false)
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

        ratingSummary.text = if (ratingCount == 0) {
            "Ainda sem avaliações"
        } else {
            val average = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", ratingAvg)
            val label = if (ratingCount == 1) "1 avaliação" else "$ratingCount avaliações"
            "★ $average  ·  $label"
        }

        when {
            !ev.isFinished() -> {
                ratingHint.text = "A avaliação fica disponível depois que o evento terminar."
                ratingHint.visibility = View.VISIBLE
                ratingInput.visibility = View.GONE
            }
            !enrolled -> {
                ratingHint.text = "Somente alunos inscritos podem avaliar este evento."
                ratingHint.visibility = View.VISIBLE
                ratingInput.visibility = View.GONE
            }
            else -> {
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

    private fun toggleEnrollment() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
            .collection("enrollments").document(eventId)

        if (enrolled) {
            ref.delete().addOnSuccessListener {
                enrolled = false
                refreshActions()
                refreshRating()
            }
        } else {
            ref.set(mapOf("eventId" to eventId)).addOnSuccessListener {
                enrolled = true
                refreshActions()
                refreshRating()
            }
        }
    }

    private fun submitRating() {
        val uid = auth.currentUser?.uid ?: return
        val stars = ratingBar.rating.toInt()
        if (stars < 1) {
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