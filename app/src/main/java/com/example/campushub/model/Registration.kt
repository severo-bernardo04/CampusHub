package com.example.campushub.model

import com.google.firebase.firestore.Exclude

data class Registration(
    @get:Exclude var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val status: String = "confirmed",
    val createdAt: Long = 0L,
    val qrToken: String = "",
    val checkedIn: Boolean = false,
    val checkedInAt: Long = 0L
)

data class RegistrationUser(val uid: String, val name: String, val email: String)