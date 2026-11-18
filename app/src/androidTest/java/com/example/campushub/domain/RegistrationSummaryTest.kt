package com.example.campushub.domain

import com.example.campushub.testRegistration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationSummaryTest {

    private val list = listOf(
        testRegistration(userId = "w2", name = "Zeca", status = RegistrationStatus.WAITLIST),
        testRegistration(userId = "c2", name = "bruno", checkedIn = true),
        testRegistration(userId = "w1", name = "Yara", status = RegistrationStatus.WAITLIST),
        testRegistration(userId = "c1", name = "Ana"),
        testRegistration(userId = "c3", name = "Carla", checkedIn = true)
    )

    @Test
    fun stats_countsConfirmedWaitlistAndCheckIns() {
        val stats = RegistrationSummary.stats(list)
        assertEquals(3, stats.confirmed)
        assertEquals(2, stats.waitlist)
        assertEquals(2, stats.checkedIn)
    }

    @Test
    fun stats_ofEmptyList_isZero() {
        assertEquals(RegistrationStats(0, 0, 0), RegistrationSummary.stats(emptyList()))
    }

    @Test
    fun sorted_listsConfirmedByNameThenWaitlistInQueueOrder() {
        val result = RegistrationSummary.sorted(list, waitlistOrder = listOf("w1", "w2"))
        assertEquals(listOf("c1", "c2", "c3", "w1", "w2"), result.map { it.userId })
    }

    @Test
    fun sorted_putsUnknownWaitlistedStudentsLast() {
        val result = RegistrationSummary.sorted(list, waitlistOrder = listOf("w2"))
        assertEquals(listOf("w2", "w1"), result.takeLast(2).map { it.userId })
    }

    @Test
    fun positionOf_isOneBasedAndNullWhenAbsent() {
        assertEquals(2, RegistrationSummary.positionOf("w2", listOf("w1", "w2")))
        assertNull(RegistrationSummary.positionOf("x", listOf("w1", "w2")))
    }
}