package com.example.campushub

import android.app.Activity
import android.content.Intent
import com.example.campushub.domain.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Abre a tela inicial certa conforme o perfil: organizador ou aluno. */
object RoleRouter {

    fun openHome(activity: Activity, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val target = if (UserRole.isOrganizer(doc.getString("role"))) {
                    OrganizerEventsActivity::class.java
                } else {
                    EventsListActivity::class.java
                }
                activity.startActivity(Intent(activity, target))
                activity.finish()
            }
            .addOnFailureListener { onFailure() }
    }
}