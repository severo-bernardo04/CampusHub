package com.example.campushub

import android.util.Log
import com.example.campushub.domain.SampleEvents
import com.google.firebase.firestore.FirebaseFirestore

object SeedEvents {

    /**
     * Popula "events" quando ela está vazia. Os eventos pertencem ao organizador
     * que executou e cada documento recebe um ID único gerado pelo Firestore.
     */
    fun run(db: FirebaseFirestore, organizerId: String, onDone: () -> Unit) {
        db.collection("events").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onDone()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                SampleEvents.build(System.currentTimeMillis(), organizerId).forEach { event ->
                    batch.set(db.collection("events").document(), event)
                }
                batch.commit()
                    .addOnFailureListener { Log.e("SEED", "Falha ao gravar", it) }
                    .addOnCompleteListener { onDone() }
            }
            .addOnFailureListener {
                Log.e("SEED", "Falha ao ler", it)
                onDone()
            }
    }
}