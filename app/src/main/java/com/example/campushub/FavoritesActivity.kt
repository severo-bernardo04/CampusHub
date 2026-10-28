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
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.emptyText)
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return

        FavoriteRepository.loadIds(db, uid) { favoriteIds ->
            db.collection("events").get().addOnSuccessListener { result ->
                val events = result.documents
                    .mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.apply { id = doc.id }
                    }
                    .filter { it.id in favoriteIds }
                    .sortedBy { it.dateMillis }

                emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE

                recyclerView.adapter = EventAdapter(
                    events = events,
                    favoriteIds = favoriteIds,
                    onFavoriteChanged = { event, favorite ->
                        // Ao desfavoritar, o evento sai desta lista
                        FavoriteRepository.set(db, uid, event.id, favorite) { loadFavorites() }
                    },
                    onClick = { event ->
                        val intent = Intent(this, EventDetailActivity::class.java)
                        intent.putExtra("eventId", event.id)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}