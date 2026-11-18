package com.example.campushub

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.domain.CapacityPolicy
import com.example.campushub.domain.RegistrationSummary
import com.example.campushub.model.Event
import com.google.firebase.firestore.FirebaseFirestore

class RegistrantsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var summaryText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrants)

        db = FirebaseFirestore.getInstance()
        eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        recyclerView = findViewById(R.id.registrantsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        summaryText = findViewById(R.id.summaryText)
        emptyText = findViewById(R.id.emptyText)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            val event = doc.toObject(Event::class.java) ?: return@addOnSuccessListener

            RegistrationRepository.loadAll(db, eventId) { registrations ->
                val stats = RegistrationSummary.stats(registrations)
                summaryText.text = listOf(
                    "Vagas ocupadas: " + CapacityPolicy.occupancyLabel(event),
                    "Check-ins: ${stats.checkedIn} de ${stats.confirmed}",
                    "Lista de espera: ${stats.waitlist}"
                ).joinToString("\n")

                val sorted = RegistrationSummary.sorted(registrations, event.waitlist)
                emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.adapter = RegistrantAdapter(sorted, event.waitlist)
            }
        }
    }
}