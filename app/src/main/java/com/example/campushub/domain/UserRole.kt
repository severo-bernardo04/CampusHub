package com.example.campushub.domain

object UserRole {
    const val STUDENT = "student"
    const val ORGANIZER = "organizer"

    fun isOrganizer(role: String?): Boolean = role == ORGANIZER

    fun fromLabel(label: String): String =
        if (label.trim().equals("Organizador", ignoreCase = true)) ORGANIZER else STUDENT

    fun label(role: String?): String = if (isOrganizer(role)) "Organizador" else "Aluno"
}