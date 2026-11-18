package com.example.campushub.domain

import com.example.campushub.DAY_MILLIS
import com.example.campushub.NOW
import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollmentPolicyTest {

    private val course = "Ciência da Computação"
    private val upcomingPublic = testEvent(isPublic = true, hostCourse = course, dateMillis = NOW + DAY_MILLIS)
    private val upcomingClosed = testEvent(isPublic = false, hostCourse = course, dateMillis = NOW + DAY_MILLIS)
    private val finishedPublic = testEvent(isPublic = true, hostCourse = course, dateMillis = NOW - DAY_MILLIS)

    @Test
    fun canEnroll_publicEventIsOpenToAnyCourse() {
        assertTrue(EnrollmentPolicy.canEnroll(upcomingPublic, "Direito"))
    }

    @Test
    fun canEnroll_closedEventIsOpenToTheHostCourse() {
        assertTrue(EnrollmentPolicy.canEnroll(upcomingClosed, course))
    }

    @Test
    fun canEnroll_closedEventIsBlockedForOtherCourses() {
        assertFalse(EnrollmentPolicy.canEnroll(upcomingClosed, "Direito"))
    }

    @Test
    fun state_finishedEvent() {
        val state = EnrollmentPolicy.stateFor(finishedPublic, true, course, false, NOW)
        assertEquals(EnrollmentState.FINISHED, state)
    }

    @Test
    fun state_finishedWinsOverEnrollmentAndLoading() {
        assertEquals(
            EnrollmentState.FINISHED,
            EnrollmentPolicy.stateFor(finishedPublic, true, course, true, NOW)
        )
        assertEquals(
            EnrollmentState.FINISHED,
            EnrollmentPolicy.stateFor(finishedPublic, false, course, false, NOW)
        )
    }

    @Test
    fun state_userNotLoaded() {
        val state = EnrollmentPolicy.stateFor(upcomingPublic, false, "", false, NOW)
        assertEquals(EnrollmentState.LOADING, state)
    }

    @Test
    fun state_closedForOtherCourse() {
        val state = EnrollmentPolicy.stateFor(upcomingClosed, true, "Direito", false, NOW)
        assertEquals(EnrollmentState.CLOSED_FOR_COURSE, state)
    }

    @Test
    fun state_enrolledCanCancel() {
        val state = EnrollmentPolicy.stateFor(upcomingPublic, true, course, true, NOW)
        assertEquals(EnrollmentState.CAN_CANCEL, state)
    }

    @Test
    fun state_notEnrolledCanEnroll() {
        val state = EnrollmentPolicy.stateFor(upcomingPublic, true, "Direito", false, NOW)
        assertEquals(EnrollmentState.CAN_ENROLL, state)
    }

    @Test
    fun state_closedEventOfOwnCourseCanEnroll() {
        val state = EnrollmentPolicy.stateFor(upcomingClosed, true, course, false, NOW)
        assertEquals(EnrollmentState.CAN_ENROLL, state)
    }

    @Test
    fun state_defaultClockIsUsedWhenNowIsOmitted() {
        val future = testEvent(dateMillis = System.currentTimeMillis() + DAY_MILLIS)
        assertEquals(
            EnrollmentState.CAN_ENROLL,
            EnrollmentPolicy.stateFor(future, true, course, false)
        )
    }
}