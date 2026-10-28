package com.example.campushub.model

data class MediaPost(
    val id: Long,
    val eventId: String,
    val authorId: String,
    val authorName: String,
    val type: String,
    val path: String,
    val createdAt: Long
) {
    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VIDEO = "video"
    }
}