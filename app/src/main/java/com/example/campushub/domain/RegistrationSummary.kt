package com.example.campushub.domain

import com.example.campushub.model.Registration

data class RegistrationStats(val confirmed: Int, val waitlist: Int, val checkedIn: Int)

object RegistrationSummary {

    fun stats(registrations: List<Registration>) = RegistrationStats(
        confirmed = registrations.count { it.status == RegistrationStatus.CONFIRMED },
        waitlist = registrations.count { it.status == RegistrationStatus.WAITLIST },
        checkedIn = registrations.count {
            it.status == RegistrationStatus.CONFIRMED && it.checkedIn
        }
    )

    /** Confirmados por nome, depois a lista de espera na ordem da fila do evento. */
    fun sorted(registrations: List<Registration>, waitlistOrder: List<String>): List<Registration> {
        val confirmed = registrations
            .filter { it.status == RegistrationStatus.CONFIRMED }
            .sortedBy { it.userName.lowercase() }
        val waiting = registrations
            .filter { it.status == RegistrationStatus.WAITLIST }
            .sortedBy { r -> waitlistOrder.indexOf(r.userId).let { if (it < 0) Int.MAX_VALUE else it } }
        return confirmed + waiting
    }

    fun positionOf(userId: String, waitlistOrder: List<String>): Int? =
        waitlistOrder.indexOf(userId).takeIf { it >= 0 }?.plus(1)
}