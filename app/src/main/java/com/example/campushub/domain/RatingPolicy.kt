package com.example.campushub.domain

import com.example.campushub.isFinished
import com.example.campushub.model.Event
import java.util.Locale

enum class RatingAccess { NOT_FINISHED, NOT_ENROLLED, CAN_RATE }

data class RatingSummary(val average: Double, val count: Int) {

    fun label(): String {
        if (count == 0) return "Ainda sem avaliações"
        val formatted = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", average)
        val plural = if (count == 1) "1 avaliação" else "$count avaliações"
        return "★ $formatted  ·  $plural"
    }
}

object RatingPolicy {
    const val MIN_STARS = 1
    const val MAX_STARS = 5

    fun isValid(stars: Int): Boolean = stars in MIN_STARS..MAX_STARS

    /** Calcula a média ignorando valores fora de 1 a 5. */
    fun summarize(stars: List<Int>): RatingSummary {
        val valid = stars.filter { isValid(it) }
        if (valid.isEmpty()) return RatingSummary(0.0, 0)
        return RatingSummary(valid.sum().toDouble() / valid.size, valid.size)
    }

    /** Só alunos inscritos avaliam, e só depois que o evento terminou. */
    fun accessFor(
        event: Event,
        enrolled: Boolean,
        now: Long = System.currentTimeMillis()
    ): RatingAccess = when {
        !event.isFinished(now) -> RatingAccess.NOT_FINISHED
        !enrolled -> RatingAccess.NOT_ENROLLED
        else -> RatingAccess.CAN_RATE
    }
}