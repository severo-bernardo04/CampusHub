package com.example.campushub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyEventsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_events)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.myEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("enrollments").get()
            .addOnSuccessListener { enrollments ->
                val eventIds = enrollments.documents.map { it.id }

                if (eventIds.isEmpty()) {
                    recyclerView.adapter = EventAdapter(emptyList()) {}
                    return@addOnSuccessListener
                }

                db.collection("events").whereIn("__name__", eventIds).get()
                    .addOnSuccessListener { result ->
                        val events = result.documents.map { doc ->
                            doc.toObject(Event::class.java)!!.apply { id = doc.id }
                        }
                        recyclerView.adapter = EventAdapter(events) {}
                    }
            }
    }
}