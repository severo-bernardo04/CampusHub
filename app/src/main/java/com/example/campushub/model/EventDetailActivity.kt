package com.example.campushub

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.model.AppUser
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String
    private var myCourse: String = ""

    private lateinit var enrollButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        eventId = intent.getStringExtra("eventId") ?: return

        enrollButton = findViewById(R.id.enrollButton)

        loadEvent()
    }

    private fun loadEvent() {
        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            val event = doc.toObject(Event::class.java) ?: return@addOnSuccessListener

            findViewById<TextView>(R.id.detailTitle).text = event.title
            findViewById<TextView>(R.id.detailDescription).text = event.description
            findViewById<TextView>(R.id.detailDate).text = event.date
            findViewById<TextView>(R.id.detailLocation).text = event.location
            findViewById<TextView>(R.id.detailCourse).text = event.hostCourse
            findViewById<TextView>(R.id.detailAccess).text =
                if (event.isPublic) "Aberto ao público" else "Fechado ao curso ${event.hostCourse}"

            checkAccessAndEnrollment(event)
        }
    }

    private fun checkAccessAndEnrollment(event: Event) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            myCourse = userDoc.toObject(AppUser::class.java)?.course ?: ""
            val canEnroll = event.isPublic || myCourse == event.hostCourse

            if (!canEnroll) {
                enrollButton.isEnabled = false
                enrollButton.text = "Evento fechado para seu curso"
                return@addOnSuccessListener
            }

            refreshEnrollButton()
        }
    }

    private fun refreshEnrollButton() {
        val uid = auth.currentUser?.uid ?: return
        val enrollmentRef = db.collection("users").document(uid)
            .collection("enrollments").document(eventId)

        enrollmentRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                enrollButton.text = "Cancelar inscrição"
                enrollButton.setOnClickListener {
                    enrollmentRef.delete().addOnCompleteListener { refreshEnrollButton() }
                }
            } else {
                enrollButton.text = "Inscrever-se"
                enrollButton.setOnClickListener {
                    enrollmentRef.set(mapOf("eventId" to eventId))
                        .addOnCompleteListener { refreshEnrollButton() }
                }
            }
        }
    }
}