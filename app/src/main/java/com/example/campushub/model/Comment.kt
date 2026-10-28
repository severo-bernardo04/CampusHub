package com.example.campushub.model

import com.google.firebase.firestore.Exclude

data class Comment(
    @get:Exclude var id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)