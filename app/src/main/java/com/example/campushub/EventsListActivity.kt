package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
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
    private var favoriteIds: MutableSet<String> = mutableSetOf()
    private var myCourse: String = ""

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var courseFilter: Spinner
    private lateinit var typeFilter: Spinner
    private lateinit var categoryFilter: Spinner
    private lateinit var situationFilter: Spinner
    private lateinit var greetingText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_list)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchInput = findViewById(R.id.searchInput)
        courseFilter = findViewById(R.id.courseFilterSpinner)
        typeFilter = findViewById(R.id.typeFilterSpinner)
        categoryFilter = findViewById(R.id.categoryFilterSpinner)
        situationFilter = findViewById(R.id.situationFilterSpinner)
        greetingText = findViewById(R.id.greetingText)
        emptyText = findViewById(R.id.emptyText)

        setupSpinner(typeFilter, R.array.type_filter_list)
        setupSpinner(categoryFilter, R.array.category_filter_list)
        setupSpinner(situationFilter, R.array.situation_filter_list)

        findViewById<Button>(R.id.myEventsButton).setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }
        findViewById<Button>(R.id.favoritesButton).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        loadUserCourse()
        SeedEvents.run(db, auth.currentUser?.uid) { loadEvents() }

        searchInput.doAfterTextChanged { applyFilters() }
        courseFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
        typeFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
        categoryFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
        situationFilter.setOnItemSelectedListener(SimpleSelectListener { applyFilters() })
    }

    override fun onResume() {
        super.onResume()
        // Os favoritos podem ter mudado nas outras telas
        val uid = auth.currentUser?.uid ?: return
        FavoriteRepository.loadIds(db, uid) {
            favoriteIds = it
            applyFilters()
        }
    }

    private fun setupSpinner(spinner: Spinner, arrayRes: Int) {
        ArrayAdapter.createFromResource(
            this, arrayRes, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = it
        }
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
                allEvents = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.apply { id = doc.id }
                }

                val courses = listOf("Todos os cursos") +
                        allEvents.map { it.hostCourse }.distinct().sorted()
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

        // Busca pelo título (ignora maiúsculas e acentos)
        val query = searchInput.text.toString().trim().normalized()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.title.normalized().contains(query) }
        }

        // Curso
        val selectedCourse = courseFilter.selectedItem?.toString()
        if (selectedCourse != null && selectedCourse != "Todos os cursos") {
            filtered = filtered.filter { it.hostCourse == selectedCourse }
        }

        // Acesso
        when (typeFilter.selectedItem?.toString()) {
            "Abertos ao público" -> filtered = filtered.filter { it.isPublic }
            "Do meu curso" -> filtered = filtered.filter { it.hostCourse == myCourse }
        }

        // Categoria (posição 0 = todas)
        if (categoryFilter.selectedItemPosition > 0) {
            val category = categoryFilter.selectedItem.toString()
            filtered = filtered.filter { it.category == category }
        }

        // Situação: 0 = todos, 1 = próximos, 2 = encerrados
        when (situationFilter.selectedItemPosition) {
            1 -> filtered = filtered.filter { !it.isFinished() }
            2 -> filtered = filtered.filter { it.isFinished() }
        }

        // Próximos primeiro (do mais perto ao mais longe), depois os encerrados (mais recentes antes)
        filtered = filtered.sortedWith(
            compareBy<Event>({ it.isFinished() }, {
                if (it.isFinished()) -it.dateMillis else it.dateMillis
            })
        )

        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        recyclerView.adapter = EventAdapter(
            events = filtered,
            favoriteIds = favoriteIds,
            onFavoriteChanged = { event, favorite ->
                auth.currentUser?.uid?.let { uid ->
                    FavoriteRepository.set(db, uid, event.id, favorite)
                }
            },
            onClick = { event ->
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            }
        )
    }
}