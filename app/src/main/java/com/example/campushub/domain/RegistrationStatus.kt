package com.example.campushub.domain

object RegistrationStatus {
    const val CONFIRMED = "confirmed"
    const val WAITLIST = "waitlist"

    fun label(status: String): String = if (status == CONFIRMED) "Confirmado" else "Lista de espera"
}