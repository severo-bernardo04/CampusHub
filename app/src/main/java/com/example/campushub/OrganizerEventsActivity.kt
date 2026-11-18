package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.domain.EventFilter
import com.example.campushub.domain.EventFilters
import com.example.campushub.model.AppUser
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class OrganizerEventsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var greetingText: TextView
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizer_events)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.organizerRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.emptyText)
        greetingText = findViewById(R.id.greetingText)

        findViewById<Button>(R.id.newEventButton).setOnClickListener {
            startActivity(Intent(this, EventFormActivity::class.java))
        }
        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val name = doc.toObject(AppUser::class.java)?.name?.trim()?.split(" ")?.firstOrNull().orEmpty()
            greetingText.text = if (name.isNotEmpty()) "Olá, $name" else "Olá!"
        }

        // Primeira abertura: popula eventos de exemplo (se não houver nenhum) e lista
        SeedEvents.run(db, uid) { loadEvents() }
    }

    override fun onResume() {
        super.onResume()
        if (started) loadEvents() else started = true
    }

    private fun loadEvents() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("events").whereEqualTo("organizerId", uid).get()
            .addOnSuccessListener { result ->
                val events = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.apply { id = doc.id }
                }
                val sorted = EventFilter.apply(events, EventFilters())

                emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.adapter = OrganizerEventAdapter(sorted) { event ->
                    startActivity(
                        Intent(this, EventManageActivity::class.java).putExtra("eventId", event.id)
                    )
                }
            }
    }
}