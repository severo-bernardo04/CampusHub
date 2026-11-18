package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.domain.CapacityPolicy
import com.example.campushub.domain.CheckInPolicy
import com.example.campushub.domain.CheckInResult
import com.example.campushub.domain.EventFormValidator
import com.example.campushub.domain.EventLifecycle
import com.example.campushub.domain.EventStatus
import com.example.campushub.domain.QrPayload
import com.example.campushub.domain.RegistrationSummary
import com.example.campushub.model.Event
import com.example.campushub.model.Registration
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.jvm.java

class EventManageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String
    private var event: Event? = null

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var statusBadge: TextView
    private lateinit var date: TextView
    private lateinit var address: TextView
    private lateinit var occupancy: TextView
    private lateinit var waitlist: TextView
    private lateinit var checkins: TextView
    private lateinit var editButton: Button
    private lateinit var publishButton: Button
    private lateinit var closeButton: Button
    private lateinit var scanButton: Button
    private lateinit var manualButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_manage)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }

        image = findViewById(R.id.manageImage)
        title = findViewById(R.id.manageTitle)
        statusBadge = findViewById(R.id.manageStatus)
        date = findViewById(R.id.manageDate)
        address = findViewById(R.id.manageAddress)
        occupancy = findViewById(R.id.manageOccupancy)
        waitlist = findViewById(R.id.manageWaitlist)
        checkins = findViewById(R.id.manageCheckins)
        editButton = findViewById(R.id.editButton)
        publishButton = findViewById(R.id.publishButton)
        closeButton = findViewById(R.id.closeButton)
        scanButton = findViewById(R.id.scanButton)
        manualButton = findViewById(R.id.manualButton)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        editButton.setOnClickListener {
            startActivity(Intent(this, EventFormActivity::class.java).putExtra("eventId", eventId))
        }
        publishButton.setOnClickListener { publish() }
        closeButton.setOnClickListener { confirmClose() }
        findViewById<Button>(R.id.registrantsButton).setOnClickListener {
            startActivity(Intent(this, RegistrantsActivity::class.java).putExtra("eventId", eventId))
        }
        scanButton.setOnClickListener { scanQr() }
        manualButton.setOnClickListener { askCodeManually() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            val loaded = doc.toObject(Event::class.java)?.apply { id = doc.id } ?: return@addOnSuccessListener
            if (loaded.organizerId != auth.currentUser?.uid) {
                toast("Este evento pertence a outro organizador")
                finish()
                return@addOnSuccessListener
            }
            event = loaded
            bind(loaded)
            RegistrationRepository.loadAll(db, eventId) { bindStats(loaded, it) }
        }
    }

    private fun bind(ev: Event) {
        title.text = ev.title
        date.text = ev.formattedDate()
        address.text = listOf(ev.location, ev.address).filter { it.isNotBlank() }.joinToString("\n")
        occupancy.text = CapacityPolicy.occupancyLabel(ev)

        val bitmap = ImageCodec.decode(ev.imageBase64, 900)
        image.visibility = if (bitmap != null) View.VISIBLE else View.GONE
        image.setImageBitmap(bitmap)

        statusBadge.text = EventStatus.label(ev.status)
        when (ev.status) {
            EventStatus.DRAFT -> {
                statusBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                statusBadge.setTextColor(getColor(R.color.text_secondary))
            }
            EventStatus.CLOSED -> {
                statusBadge.setBackgroundResource(R.drawable.bg_badge_closed)
                statusBadge.setTextColor(getColor(R.color.warning))
            }
            else -> {
                statusBadge.setBackgroundResource(R.drawable.bg_badge_public)
                statusBadge.setTextColor(getColor(R.color.success))
            }
        }

        editButton.visibility = if (EventLifecycle.canEdit(ev.status)) View.VISIBLE else View.GONE
        publishButton.visibility = if (EventLifecycle.canPublish(ev.status)) View.VISIBLE else View.GONE
        closeButton.visibility = if (EventLifecycle.canClose(ev.status)) View.VISIBLE else View.GONE

        val checkInEnabled = ev.status != EventStatus.DRAFT
        scanButton.isEnabled = checkInEnabled
        manualButton.isEnabled = checkInEnabled
    }

    private fun bindStats(ev: Event, registrations: List<Registration>) {
        val stats = RegistrationSummary.stats(registrations)
        occupancy.text = "Vagas ocupadas: " + CapacityPolicy.occupancyLabel(ev)
        waitlist.text = "Lista de espera: ${stats.waitlist}"
        checkins.text = "Check-ins realizados: ${stats.checkedIn} de ${stats.confirmed} confirmados"
    }

    // ---------- Publicar e encerrar ----------

    private fun publish() {
        val ev = event ?: return
        val errors = EventFormValidator.validate(EventFormValidator.inputFrom(ev), publishing = true)

        if (errors.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Ainda não dá para publicar")
                .setMessage(errors.values.joinToString("\n• ", prefix = "• "))
                .setPositiveButton("Editar") { _, _ -> editButton.performClick() }
                .setNegativeButton("Fechar", null)
                .show()
            return
        }
        changeStatus(EventStatus.PUBLISHED)
    }

    private fun confirmClose() {
        AlertDialog.Builder(this)
            .setTitle("Encerrar evento")
            .setMessage(
                "Novas inscrições serão bloqueadas, o evento passa a constar como encerrado " +
                        "e os inscritos poderão avaliá-lo. Deseja continuar?"
            )
            .setPositiveButton("Encerrar") { _, _ -> changeStatus(EventStatus.CLOSED) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changeStatus(status: String) {
        db.collection("events").document(eventId).update("status", status)
            .addOnSuccessListener { load() }
            .addOnFailureListener { toast(it.message) }
    }

    // ---------- Check-in ----------

    private fun scanQr() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner: Task<Barcode> = GmsBarcodeScanning.getClient(this, options).startScan()
        scanner
            .addOnSuccessListener { barcode -> handleCode(barcode.rawValue) }
            .addOnFailureListener { toast("Não foi possível abrir o leitor: ${it.message}") }
    }

    private fun askCodeManually() {
        val input = EditText(this).apply { hint = "campushub:evento:aluno:codigo" }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Digitar código do QR")
            .setView(container)
            .setPositiveButton("Validar") { _, _ -> handleCode(input.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleCode(raw: String?) {
        RegistrationRepository.checkIn(db, eventId, QrPayload.parse(raw)) { result ->
            result
                .onSuccess { outcome ->
                    val ok = outcome.result == CheckInResult.VALID
                    val message = if (ok) "✔ ${outcome.studentName}\nPresença registrada"
                    else CheckInPolicy.message(outcome.result)
                    AlertDialog.Builder(this)
                        .setTitle(if (ok) "Check-in realizado" else "Check-in recusado")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                    load()
                }
                .onFailure { toast(it.message) }
        }
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message ?: "Erro", Toast.LENGTH_LONG).show()
    }
}