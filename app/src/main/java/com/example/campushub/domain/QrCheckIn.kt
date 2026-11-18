package com.example.campushub.domain

import com.example.campushub.model.Registration

data class QrPayload(val eventId: String, val userId: String, val token: String) {

    fun encode(): String = "$PREFIX:$eventId:$userId:$token"

    companion object {
        const val PREFIX = "campushub"

        fun parse(text: String?): QrPayload? {
            val parts = text?.trim()?.split(":") ?: return null
            if (parts.size != 4 || parts[0] != PREFIX || parts.drop(1).any { it.isBlank() }) return null
            return QrPayload(parts[1], parts[2], parts[3])
        }
    }
}

enum class CheckInResult {
    VALID, INVALID_CODE, WRONG_EVENT, NOT_FOUND, NOT_CONFIRMED, INVALID_TOKEN, ALREADY_USED
}

data class CheckInOutcome(val result: CheckInResult, val studentName: String)

object CheckInPolicy {

    /** Decide se um QR pode registrar presença. Cada inscrição vale uma única vez. */
    fun evaluate(
        payload: QrPayload?,
        expectedEventId: String,
        registration: Registration?
    ): CheckInResult = when {
        payload == null -> CheckInResult.INVALID_CODE
        payload.eventId != expectedEventId -> CheckInResult.WRONG_EVENT
        registration == null -> CheckInResult.NOT_FOUND
        registration.status != RegistrationStatus.CONFIRMED -> CheckInResult.NOT_CONFIRMED
        registration.qrToken.isEmpty() || registration.qrToken != payload.token ->
            CheckInResult.INVALID_TOKEN
        registration.checkedIn -> CheckInResult.ALREADY_USED
        else -> CheckInResult.VALID
    }

    fun message(result: CheckInResult): String = when (result) {
        CheckInResult.VALID -> "Presença registrada"
        CheckInResult.INVALID_CODE -> "QR Code inválido"
        CheckInResult.WRONG_EVENT -> "Este QR Code é de outro evento"
        CheckInResult.NOT_FOUND -> "Inscrição não encontrada"
        CheckInResult.NOT_CONFIRMED -> "A inscrição não está confirmada (lista de espera)"
        CheckInResult.INVALID_TOKEN -> "QR Code inválido ou desatualizado"
        CheckInResult.ALREADY_USED -> "Este QR Code já foi utilizado"
    }
}