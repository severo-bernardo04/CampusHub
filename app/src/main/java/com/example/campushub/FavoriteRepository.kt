package com.example.campushub

import com.google.firebase.firestore.FirebaseFirestore

/** Favoritos ficam em users/{uid}/favorites/{eventId}, separados das inscrições. */
object FavoriteRepository {

    private fun collection(db: FirebaseFirestore, uid: String) =
        db.collection("users").document(uid).collection("favorites")

    fun loadIds(db: FirebaseFirestore, uid: String, onResult: (MutableSet<String>) -> Unit) {
        collection(db, uid).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.map { it.id }.toMutableSet())
            }
            .addOnFailureListener { onResult(mutableSetOf()) }
    }

    fun set(
        db: FirebaseFirestore,
        uid: String,
        eventId: String,
        favorite: Boolean,
        onDone: () -> Unit = {}
    ) {
        val ref = collection(db, uid).document(eventId)
        val task = if (favorite) {
            ref.set(mapOf("eventId" to eventId, "createdAt" to System.currentTimeMillis()))
        } else {
            ref.delete()
        }
        task.addOnCompleteListener { onDone() }
    }
}