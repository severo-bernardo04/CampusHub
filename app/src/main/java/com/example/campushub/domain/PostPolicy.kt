package com.example.campushub.domain

/** Regras compartilhadas por comentários e publicações de mídia. */
object PostPolicy {
    const val MAX_VIDEO_BYTES = 50L * 1024 * 1024

    fun displayName(name: String, email: String?): String = name.ifEmpty { email ?: "Aluno" }

    /** Só o autor pode editar ou excluir a própria publicação. */
    fun isOwner(authorId: String, uid: String?): Boolean = uid != null && authorId == uid

    /** Remove espaços nas pontas; texto vazio vira null. */
    fun cleanText(text: String): String? = text.trim().takeIf { it.isNotEmpty() }

    fun isVideoTooLarge(bytes: Long): Boolean = bytes > MAX_VIDEO_BYTES

    fun isEdited(updatedAt: Long): Boolean = updatedAt > 0
}