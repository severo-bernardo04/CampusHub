package com.example.campushub.domain

object EventStatus {
    const val DRAFT = "draft"
    const val PUBLISHED = "published"
    const val CLOSED = "closed"

    fun label(status: String): String = when (status) {
        DRAFT -> "Rascunho"
        CLOSED -> "Encerrado"
        else -> "Publicado"
    }
}

object EventLifecycle {
    fun canEdit(status: String): Boolean = status != EventStatus.CLOSED
    fun canPublish(status: String): Boolean = status == EventStatus.DRAFT
    fun canClose(status: String): Boolean = status == EventStatus.PUBLISHED
    fun isVisibleToStudents(status: String): Boolean = status != EventStatus.DRAFT
}