package com.example.campushub.domain

import com.example.campushub.model.Event

/** Regras de vagas e lista de espera. Capacidade 0 = sem limite. */
object CapacityPolicy {

    fun isUnlimited(event: Event): Boolean = event.capacity <= 0

    fun hasSeat(event: Event): Boolean = isUnlimited(event) || event.confirmedCount < event.capacity

    fun isFull(event: Event): Boolean = !hasSeat(event)

    /** Vagas livres, ou null quando o evento não tem limite. */
    fun availableSeats(event: Event): Int? =
        if (isUnlimited(event)) null else maxOf(0, event.capacity - event.confirmedCount)

    /** Entra direto só se há vaga E ninguém espera na fila (ninguém fura a fila). */
    fun canJoinDirectly(event: Event): Boolean = hasSeat(event) && event.waitlist.isEmpty()

    /** Posição na lista de espera (1 = próximo da fila), ou null se não está nela. */
    fun waitlistPosition(event: Event, uid: String): Int? =
        event.waitlist.indexOf(uid).takeIf { it >= 0 }?.plus(1)

    /** Quando uma vaga abre, só o primeiro da fila pode confirmar. */
    fun canConfirmFromWaitlist(event: Event, uid: String): Boolean =
        hasSeat(event) && event.waitlist.firstOrNull() == uid

    fun occupancyLabel(event: Event): String =
        if (isUnlimited(event)) "${event.confirmedCount} inscritos · sem limite de vagas"
        else "${event.confirmedCount} de ${event.capacity} vagas ocupadas"

    fun waitlistLabel(event: Event): String = when (event.waitlist.size) {
        0 -> ""
        1 -> "1 pessoa na lista de espera"
        else -> "${event.waitlist.size} pessoas na lista de espera"
    }
}