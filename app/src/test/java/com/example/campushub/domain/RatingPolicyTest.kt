package com.example.campushub.domain

import com.example.campushub.DAY_MILLIS
import com.example.campushub.NOW
import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPolicyTest {

    private val upcoming = testEvent(dateMillis = NOW + DAY_MILLIS)
    private val finished = testEvent(dateMillis = NOW - DAY_MILLIS)

    @Test
    fun isValid_acceptsStarsFromOneToFive() {
        (1..5).forEach { assertTrue("$it deveria ser válido", RatingPolicy.isValid(it)) }
    }

    @Test
    fun isValid_rejectsValuesOutsideTheRange() {
        listOf(-1, 0, 6, 100).forEach {
            assertFalse("$it deveria ser inválido", RatingPolicy.isValid(it))
        }
    }

    @Test
    fun summarize_ofNoRatings() {
        val summary = RatingPolicy.summarize(emptyList())
        assertEquals(0, summary.count)
        assertEquals(0.0, summary.average, 0.0)
    }

    @Test
    fun summarize_computesAverage() {
        val summary = RatingPolicy.summarize(listOf(5, 3, 4))
        assertEquals(3, summary.count)
        assertEquals(4.0, summary.average, 0.0001)
    }

    @Test
    fun summarize_ignoresInvalidValues() {
        val summary = RatingPolicy.summarize(listOf(5, 0, 6, 3))
        assertEquals(2, summary.count)
        assertEquals(4.0, summary.average, 0.0001)
    }

    @Test
    fun summarize_ofOnlyInvalidValuesIsEmpty() {
        val summary = RatingPolicy.summarize(listOf(0, 9))
        assertEquals(0, summary.count)
    }

    @Test
    fun label_withoutRatings() {
        assertEquals("Ainda sem avaliações", RatingSummary(0.0, 0).label())
    }

    @Test
    fun label_withSingleRating() {
        assertEquals("★ 5,0  ·  1 avaliação", RatingSummary(5.0, 1).label())
    }

    @Test
    fun label_withSeveralRatings() {
        assertEquals("★ 4,0  ·  3 avaliações", RatingSummary(4.0, 3).label())
    }

    @Test
    fun label_usesCommaAsDecimalSeparator() {
        assertEquals("★ 4,3  ·  3 avaliações", RatingPolicy.summarize(listOf(5, 4, 4)).label())
    }

    @Test
    fun label_roundsToOneDecimalPlace() {
        assertEquals("★ 4,7  ·  3 avaliações", RatingPolicy.summarize(listOf(5, 5, 4)).label())
    }

    @Test
    fun accessFor_beforeTheEventEnds() {
        assertEquals(RatingAccess.NOT_FINISHED, RatingPolicy.accessFor(upcoming, true, NOW))
    }

    @Test
    fun accessFor_finishedButNotEnrolled() {
        assertEquals(RatingAccess.NOT_ENROLLED, RatingPolicy.accessFor(finished, false, NOW))
    }

    @Test
    fun accessFor_finishedAndEnrolled() {
        assertEquals(RatingAccess.CAN_RATE, RatingPolicy.accessFor(finished, true, NOW))
    }

    @Test
    fun accessFor_usesCurrentTimeByDefault() {
        val past = testEvent(dateMillis = System.currentTimeMillis() - DAY_MILLIS)
        assertEquals(RatingAccess.CAN_RATE, RatingPolicy.accessFor(past, true))
    }
}