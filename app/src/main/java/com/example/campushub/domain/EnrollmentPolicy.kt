package com.example.campushub.domain

import com.example.campushub.isFinished
import com.example.campushub.model.Event

enum class EnrollmentState {
    LOADING, FINISHED, CLOSED_FOR_COURSE, CAN_CANCEL, CAN_ENROLL,
    NOT_OPEN, FULL_CAN_JOIN_WAITLIST, ON_WAITLIST, OFFER_AVAILABLE
}

object EnrollmentPolicy {

    /** Evento público: qualquer curso. Evento fechado: só o curso anfitrião. */
    fun canEnroll(event: Event, myCourse: String): Boolean =
        event.isPublic || event.hostCourse == myCourse

    fun stateFor(
        event: Event,
        userLoaded: Boolean,
        myCourse: String,
        enrolled: Boolean,
        now: Long = System.currentTimeMillis()
    ): EnrollmentState = stateFor(
        event, userLoaded, myCourse,
        if (enrolled) RegistrationStatus.CONFIRMED else null, "", now
    )

    fun stateFor(
        event: Event,
        userLoaded: Boolean,
        myCourse: String,
        registration: String?,
        uid: String,
        now: Long = System.currentTimeMillis()
    ): EnrollmentState = when {
        event.isFinished(now) -> EnrollmentState.FINISHED
        event.status == EventStatus.DRAFT -> EnrollmentState.NOT_OPEN
        !userLoaded -> EnrollmentState.LOADING
        registration == RegistrationStatus.CONFIRMED -> EnrollmentState.CAN_CANCEL
        registration == RegistrationStatus.WAITLIST ->
            if (CapacityPolicy.canConfirmFromWaitlist(event, uid)) EnrollmentState.OFFER_AVAILABLE
            else EnrollmentState.ON_WAITLIST
        !canEnroll(event, myCourse) -> EnrollmentState.CLOSED_FOR_COURSE
        CapacityPolicy.canJoinDirectly(event) -> EnrollmentState.CAN_ENROLL
        else -> EnrollmentState.FULL_CAN_JOIN_WAITLIST
    }
}