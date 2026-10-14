package com.example.campushub.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Event(
    @get:Exclude var id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val hostCourse: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true
)