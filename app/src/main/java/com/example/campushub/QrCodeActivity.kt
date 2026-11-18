package com.example.campushub

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.domain.QrPayload
import com.example.campushub.domain.RegistrationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QrCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        val eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val eventTitle = findViewById<TextView>(R.id.qrEventTitle)
        val qrImage = findViewById<ImageView>(R.id.qrImage)
        val codeText = findViewById<TextView>(R.id.qrCodeText)
        val statusText = findViewById<TextView>(R.id.qrStatus)

        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            eventTitle.text = doc.getString("title").orEmpty()
        }

        RegistrationRepository.loadOwn(db, uid, eventId) { registration ->
            if (registration == null || registration.status != RegistrationStatus.CONFIRMED) {
                statusText.text = "Você não tem inscrição confirmada neste evento."
                return@loadOwn
            }

            val payload = QrPayload(eventId, uid, registration.qrToken).encode()
            qrImage.setImageBitmap(QrCodeImage.create(payload))
            codeText.text = payload
            statusText.text = if (registration.checkedIn) {
                "✔ Presença já registrada neste evento"
            } else {
                "Apresente este QR Code ao organizador na entrada"
            }
        }
    }
}