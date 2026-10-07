package com.example.campushub.model

data class Event(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val hostCourse: String = "",
    val isPublic: Boolean = true
)