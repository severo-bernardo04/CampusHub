package com.example.campushub

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.doAfterTextChanged
import com.example.campushub.domain.EventFormInput
import com.example.campushub.domain.EventFormValidator
import com.example.campushub.domain.EventLifecycle
import com.example.campushub.domain.EventStatus
import com.example.campushub.domain.FormField
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventFormActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var courseSpinner: Spinner
    private lateinit var publicSwitch: SwitchCompat
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var locationInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var mapStatus: TextView
    private lateinit var capacityInput: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var removeImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var publishButton: Button

    private var editingId: String? = null
    private var existing: Event? = null
    private var populating = false
    private var latitude = 0.0
    private var longitude = 0.0
    private var imageBase64 = ""

    private val selected: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 7)
        set(Calendar.HOUR_OF_DAY, 19)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Thread {
                    val encoded = ImageCodec.encode(this, uri)
                    runOnUiThread {
                        if (encoded == null) {
                            toast("Não foi possível usar esta imagem")
                        } else {
                            imageBase64 = encoded
                            showImage()
                        }
                    }
                }.start()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_form)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        titleInput = findViewById(R.id.titleInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        courseSpinner = findViewById(R.id.courseSpinner)
        publicSwitch = findViewById(R.id.publicSwitch)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        locationInput = findViewById(R.id.locationInput)
        addressInput = findViewById(R.id.addressInput)
        mapStatus = findViewById(R.id.mapStatus)
        capacityInput = findViewById(R.id.capacityInput)
        imagePreview = findViewById(R.id.imagePreview)
        removeImageButton = findViewById(R.id.removeImageButton)
        saveButton = findViewById(R.id.saveButton)
        publishButton = findViewById(R.id.publishButton)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        ArrayAdapter.createFromResource(this, R.array.category_list, android.R.layout.simple_spinner_item)
            .also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = it
            }
        ArrayAdapter.createFromResource(this, R.array.course_list, android.R.layout.simple_spinner_item)
            .also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courseSpinner.adapter = it
            }

        refreshDateTexts()
        showImage()

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selected.set(year, month, day)
                    refreshDateTexts()
                },
                selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hour)
                    selected.set(Calendar.MINUTE, minute)
                    refreshDateTexts()
                },
                selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true
            ).show()
        }

        // Mudou o endereço: a coordenada antiga deixa de valer
        addressInput.doAfterTextChanged {
            if (!populating) {
                latitude = 0.0
                longitude = 0.0
                mapStatus.text = "Endereço alterado: use \"Buscar no mapa\""
            }
        }
        findViewById<Button>(R.id.searchMapButton).setOnClickListener { searchOnMap() }

        findViewById<Button>(R.id.pickImageButton).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        removeImageButton.setOnClickListener {
            imageBase64 = ""
            showImage()
        }

        saveButton.setOnClickListener { save(publish = false) }
        publishButton.setOnClickListener { save(publish = true) }

        editingId = intent.getStringExtra("eventId")
        editingId?.let { loadForEdit(it) }
    }

    private fun loadForEdit(id: String) {
        db.collection("events").document(id).get().addOnSuccessListener { doc ->
            val event = doc.toObject(Event::class.java)?.apply { this.id = doc.id } ?: return@addOnSuccessListener
            existing = event
            populating = true

            titleInput.setText(event.title)
            descriptionInput.setText(event.description)
            locationInput.setText(event.location)
            addressInput.setText(event.address)
            capacityInput.setText(event.capacity.toString())
            publicSwitch.isChecked = event.isPublic
            categorySpinner.setSelection((categorySpinner.adapter as ArrayAdapter<String>).getPosition(event.category))
            courseSpinner.setSelection((courseSpinner.adapter as ArrayAdapter<String>).getPosition(event.hostCourse))
            selected.timeInMillis = event.dateMillis
            latitude = event.latitude
            longitude = event.longitude
            imageBase64 = event.imageBase64

            if (latitude != 0.0 || longitude != 0.0) mapStatus.text = "Local já confirmado no mapa"
            populating = false

            refreshDateTexts()
            showImage()

            findViewById<TextView>(R.id.formTitle).text = "Editar evento"
            if (EventLifecycle.canPublish(event.status)) {
                saveButton.text = "Salvar rascunho"
            } else {
                saveButton.text = "Salvar alterações"
                publishButton.visibility = View.GONE
            }
            if (!EventLifecycle.canEdit(event.status)) {
                toast("Eventos encerrados não podem ser editados")
                finish()
            }
        }
    }

    private fun searchOnMap() {
        val address = addressInput.text.toString().trim()
        if (address.isEmpty()) {
            addressInput.error = "Informe o endereço"
            return
        }
        mapStatus.text = "Buscando…"
        AddressGeocoder.search(address) { point ->
            if (point == null) {
                mapStatus.text = "Endereço não encontrado. Inclua cidade e estado, ex.: Santiago - RS"
            } else {
                latitude = point.latitude
                longitude = point.longitude
                mapStatus.text = "Localizado: ${point.displayName}"
            }
        }
    }

    private fun refreshDateTexts() {
        val locale = Locale.forLanguageTag("pt-BR")
        dateButton.text = SimpleDateFormat("dd/MM/yyyy", locale).format(selected.time)
        timeButton.text = SimpleDateFormat("HH:mm", locale).format(selected.time)
    }

    private fun showImage() {
        val bitmap = ImageCodec.decode(imageBase64, 800)
        imagePreview.visibility = if (bitmap != null) View.VISIBLE else View.GONE
        removeImageButton.visibility = if (bitmap != null) View.VISIBLE else View.GONE
        imagePreview.setImageBitmap(bitmap)
    }

    private fun save(publish: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        val input = EventFormInput(
            title = titleInput.text.toString(),
            description = descriptionInput.text.toString(),
            category = categorySpinner.selectedItem?.toString().orEmpty(),
            dateMillis = selected.timeInMillis,
            address = addressInput.text.toString(),
            capacityText = capacityInput.text.toString(),
            latitude = latitude,
            longitude = longitude,
            confirmedCount = existing?.confirmedCount ?: 0
        )

        val errors = EventFormValidator.validate(input, publishing = publish)
        titleInput.error = errors[FormField.TITLE]
        descriptionInput.error = errors[FormField.DESCRIPTION]
        addressInput.error = errors[FormField.ADDRESS]
        capacityInput.error = errors[FormField.CAPACITY]
        if (errors.isNotEmpty()) {
            // Erros de data e de mapa não têm campo de texto próprio: mostra em aviso
            (errors[FormField.DATE] ?: errors[FormField.MAP_LOCATION])?.let { toast(it) }
            return
        }

        val capacity = EventFormValidator.parseCapacity(input.capacityText) ?: return
        val id = editingId

        if (id == null) {
            val event = Event(
                title = input.title.trim(),
                description = input.description.trim(),
                category = input.category,
                hostCourse = courseSpinner.selectedItem.toString(),
                location = locationInput.text.toString().trim(),
                address = input.address.trim(),
                latitude = latitude,
                longitude = longitude,
                dateMillis = input.dateMillis,
                organizerId = uid,
                status = if (publish) EventStatus.PUBLISHED else EventStatus.DRAFT,
                capacity = capacity,
                imageBase64 = imageBase64,
                isPublic = publicSwitch.isChecked
            )
            db.collection("events").add(event)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { toast(it.message) }
        } else {
            // update() com campos explícitos preserva contador de vagas e lista de espera
            val updates = mutableMapOf<String, Any>(
                "title" to input.title.trim(),
                "description" to input.description.trim(),
                "category" to input.category,
                "hostCourse" to courseSpinner.selectedItem.toString(),
                "location" to locationInput.text.toString().trim(),
                "address" to input.address.trim(),
                "latitude" to latitude,
                "longitude" to longitude,
                "dateMillis" to input.dateMillis,
                "capacity" to capacity,
                "imageBase64" to imageBase64,
                "isPublic" to publicSwitch.isChecked
            )
            if (publish && existing?.status == EventStatus.DRAFT) {
                updates["status"] = EventStatus.PUBLISHED
            }
            db.collection("events").document(id).update(updates)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { toast(it.message) }
        }
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message ?: "Erro", Toast.LENGTH_LONG).show()
    }
}