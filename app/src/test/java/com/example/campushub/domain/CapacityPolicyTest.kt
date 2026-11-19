package com.example.campushub.domain

import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapacityPolicyTest {

    private val limited = testEvent(capacity = 2, confirmedCount = 1)
    private val full = testEvent(capacity = 2, confirmedCount = 2)
    private val unlimited = testEvent(capacity = 0, confirmedCount = 500)

    @Test
    fun unlimitedEvent_alwaysHasSeats() {
        assertTrue(CapacityPolicy.isUnlimited(unlimited))
        assertTrue(CapacityPolicy.hasSeat(unlimited))
        assertNull(CapacityPolicy.availableSeats(unlimited))
    }

    @Test
    fun availableSeats_isCapacityMinusConfirmed() {
        assertEquals(1, CapacityPolicy.availableSeats(limited))
    }

    @Test
    fun availableSeats_neverGoesBelowZero() {
        assertEquals(0, CapacityPolicy.availableSeats(testEvent(capacity = 2, confirmedCount = 5)))
    }

    @Test
    fun isFull_whenConfirmedReachesCapacity() {
        assertTrue(CapacityPolicy.isFull(full))
        assertFalse(CapacityPolicy.isFull(limited))
    }

    @Test
    fun canJoinDirectly_whenThereIsASeatAndNoWaitlist() {
        assertTrue(CapacityPolicy.canJoinDirectly(limited))
    }

    @Test
    fun canJoinDirectly_falseWhenEventIsFull() {
        assertFalse(CapacityPolicy.canJoinDirectly(full))
    }

    @Test
    fun canJoinDirectly_falseWhenSomeoneIsWaiting_evenWithFreeSeat() {
        val event = testEvent(capacity = 2, confirmedCount = 1, waitlist = listOf("other"))
        assertFalse(CapacityPolicy.canJoinDirectly(event))
    }

    @Test
    fun waitlistPosition_isOneBasedAndNullWhenAbsent() {
        val event = testEvent(waitlist = listOf("a", "b", "c"))
        assertEquals(1, CapacityPolicy.waitlistPosition(event, "a"))
        assertEquals(3, CapacityPolicy.waitlistPosition(event, "c"))
        assertNull(CapacityPolicy.waitlistPosition(event, "z"))
    }

    @Test
    fun canConfirmFromWaitlist_onlyFirstInLineWithFreeSeat() {
        val open = testEvent(capacity = 2, confirmedCount = 1, waitlist = listOf("a", "b"))
        assertTrue(CapacityPolicy.canConfirmFromWaitlist(open, "a"))
        assertFalse(CapacityPolicy.canConfirmFromWaitlist(open, "b"))

        val closed = testEvent(capacity = 2, confirmedCount = 2, waitlist = listOf("a"))
        assertFalse(CapacityPolicy.canConfirmFromWaitlist(closed, "a"))
    }

    @Test
    fun occupancyLabel_describesLimitedAndUnlimitedEvents() {
        assertEquals("1 de 2 vagas ocupadas", CapacityPolicy.occupancyLabel(limited))
        assertEquals("500 inscritos · sem limite de vagas", CapacityPolicy.occupancyLabel(unlimited))
    }

    @Test
    fun waitlistLabel_handlesZeroOneAndMany() {
        assertEquals("", CapacityPolicy.waitlistLabel(testEvent()))
        assertEquals("1 pessoa na lista de espera", CapacityPolicy.waitlistLabel(testEvent(waitlist = listOf("a"))))
        assertEquals(
            "2 pessoas na lista de espera",
            CapacityPolicy.waitlistLabel(testEvent(waitlist = listOf("a", "b")))
        )
    }
}