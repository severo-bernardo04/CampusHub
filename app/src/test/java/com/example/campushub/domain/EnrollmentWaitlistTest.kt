package com.example.campushub.domain

import com.example.campushub.NOW
import com.example.campushub.DAY_MILLIS
import com.example.campushub.model.Event
import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class EnrollmentWaitlistTest {

    private val me = "u-me"

    private fun state(
        event: Event,
        registration: String? = null,
        loaded: Boolean = true,
        course: String = "Direito"
    ) = EnrollmentPolicy.stateFor(event, loaded, course, registration, me, NOW)

    @Test
    fun fullEvent_withoutRegistration_offersTheWaitlist() {
        val full = testEvent(capacity = 2, confirmedCount = 2)
        assertEquals(EnrollmentState.FULL_CAN_JOIN_WAITLIST, state(full))
    }

    @Test
    fun freeSeatButSomeoneIsWaiting_stillGoesThroughTheWaitlist() {
        val event = testEvent(capacity = 2, confirmedCount = 1, waitlist = listOf("other"))
        assertEquals(EnrollmentState.FULL_CAN_JOIN_WAITLIST, state(event))
    }

    @Test
    fun waitlisted_notFirstInLine_keepsWaiting() {
        val event = testEvent(capacity = 2, confirmedCount = 1, waitlist = listOf("other", me))
        assertEquals(EnrollmentState.ON_WAITLIST, state(event, RegistrationStatus.WAITLIST))
    }

    @Test
    fun waitlisted_firstInLineButEventStillFull_keepsWaiting() {
        val event = testEvent(capacity = 2, confirmedCount = 2, waitlist = listOf(me))
        assertEquals(EnrollmentState.ON_WAITLIST, state(event, RegistrationStatus.WAITLIST))
    }

    @Test
    fun waitlisted_firstInLineWithFreeSeat_canConfirm() {
        val event = testEvent(capacity = 2, confirmedCount = 1, waitlist = listOf(me))
        assertEquals(EnrollmentState.OFFER_AVAILABLE, state(event, RegistrationStatus.WAITLIST))
    }

    @Test
    fun confirmedStudent_canCancel() {
        assertEquals(EnrollmentState.CAN_CANCEL, state(testEvent(), RegistrationStatus.CONFIRMED))
    }

    @Test
    fun draftEvent_isNotOpen() {
        assertEquals(EnrollmentState.NOT_OPEN, state(testEvent(status = EventStatus.DRAFT)))
    }

    @Test
    fun eventClosedByTheOrganizer_countsAsFinished() {
        assertEquals(EnrollmentState.FINISHED, state(testEvent(status = EventStatus.CLOSED)))
    }

    @Test
    fun waitlistedStudentOfAnotherCourse_keepsTheirPlaceInTheQueue() {
        val closed = testEvent(
            isPublic = false, capacity = 1, confirmedCount = 1, waitlist = listOf(me),
            dateMillis = NOW + DAY_MILLIS
        )
        assertEquals(EnrollmentState.ON_WAITLIST, state(closed, RegistrationStatus.WAITLIST))
    }

    @Test
    fun userNotLoaded_winsOverRegistration() {
        assertEquals(
            EnrollmentState.LOADING,
            state(testEvent(), RegistrationStatus.CONFIRMED, loaded = false)
        )
    }
}