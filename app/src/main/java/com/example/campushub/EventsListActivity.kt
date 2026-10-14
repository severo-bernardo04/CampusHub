package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.AppUser
import com.example.campushub.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventsListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var allEvents: List<Event> = emptyList()
    private var myCourse: String = ""

    private lateinit var recyclerView: RecyclerView
    private lateinit var courseFilter: Spinner
    private lateinit var typeFilter: Spinner
    private lateinit var greetingText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_list)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        courseFilter = findViewById(R.id.courseFilterSpinner)
        typeFilter = findViewById(R.id.typeFilterSpinner)
        greetingText = findViewById(R.id.greetingText)
        emptyText = findViewById(R.id.emptyText)

        ArrayAdapter.createFromResource(
            this, R.array.type_filter_list, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeFilter.adapter = it
        }

        findViewById<Button>(R.id.myEventsButton).setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }

        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        loadUserCourse()
        SeedEvents.run(db) { loadEvents() }

        courseFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
        typeFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
    }

    private fun loadUserCourse() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(AppUser::class.java)
                myCourse = user?.course ?: ""

                val firstName = user?.name?.trim()?.split(" ")?.firstOrNull().orEmpty()
                greetingText.text = if (firstName.isNotEmpty()) "Olá, $firstName" else "Olá!"
            }
    }

    private fun loadEvents() {
        db.collection("events").get()
            .addOnSuccessListener { result ->
                allEvents = result.documents.map { doc ->
                    doc.toObject(Event::class.java)!!.apply { id = doc.id }
                }

                val courses = listOf("Todos os cursos") +
                        allEvents.map { it.hostCourse }.distinct()
                ArrayAdapter(this, android.R.layout.simple_spinner_item, courses).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    courseFilter.adapter = it
                }

                applyFilters()
            }
            .addOnFailureListener {
                Log.e("EVENTS", "Falha ao carregar", it)
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun applyFilters() {
        var filtered = allEvents

        val selectedCourse = courseFilter.selectedItem?.toString()
        if (selectedCourse != null && selectedCourse != "Todos os cursos") {
            filtered = filtered.filter { it.hostCourse == selectedCourse }
        }

        when (typeFilter.selectedItem?.toString()) {
            "Abertos ao público" -> filtered = filtered.filter { it.isPublic }
            "Do meu curso" -> filtered = filtered.filter { it.hostCourse == myCourse }
        }

        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        recyclerView.adapter = EventAdapter(filtered) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }
    }
}