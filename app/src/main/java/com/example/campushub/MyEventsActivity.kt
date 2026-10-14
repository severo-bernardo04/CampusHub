package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class MyEventsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_events)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.myEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.emptyText)
    }

    override fun onResume() {
        super.onResume()
        loadMyEvents()
    }

    private fun showEvents(events: List<Event>) {
        emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.adapter = EventAdapter(events) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }
    }

    private fun loadMyEvents() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("enrollments").get()
            .addOnSuccessListener { enrollments ->
                val eventIds = enrollments.documents.map { it.id }

                if (eventIds.isEmpty()) {
                    showEvents(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("events")
                    .whereIn(FieldPath.documentId(), eventIds.take(10))
                    .get()
                    .addOnSuccessListener { result ->
                        showEvents(result.documents.map { doc ->
                            doc.toObject(Event::class.java)!!.apply { id = doc.id }
                        })
                    }
            }
    }
}