package com.example.campushub.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Event(
    @get:Exclude var id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val hostCourse: String = "",
    val location: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dateMillis: Long = 0L,
    val organizerId: String = "",
    val status: String = "published",
    val capacity: Int = 0,
    val confirmedCount: Int = 0,
    val waitlist: List<String> = emptyList(),
    val imageBase64: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true
)