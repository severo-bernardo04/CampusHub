package com.example.campushub.domain

import com.example.campushub.testRegistration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInTest {

    private val payload = QrPayload("evt-1", "u1", "tok")

    @Test
    fun payload_encodesWithPrefix() {
        assertEquals("campushub:evt-1:u1:tok", payload.encode())
    }

    @Test
    fun payload_roundTrips() {
        assertEquals(payload, QrPayload.parse(payload.encode()))
    }

    @Test
    fun parse_trimsSurroundingSpaces() {
        assertNotNull(QrPayload.parse("  campushub:evt-1:u1:tok \n"))
    }

    @Test
    fun parse_rejectsInvalidCodes() {
        assertNull(QrPayload.parse(null))
        assertNull(QrPayload.parse(""))
        assertNull(QrPayload.parse("outroapp:evt-1:u1:tok"))
        assertNull(QrPayload.parse("campushub:evt-1:u1"))
        assertNull(QrPayload.parse("campushub:evt-1:u1:tok:extra"))
        assertNull(QrPayload.parse("campushub::u1:tok"))
    }

    @Test
    fun evaluate_validRegistration() {
        assertEquals(CheckInResult.VALID, CheckInPolicy.evaluate(payload, "evt-1", testRegistration()))
    }

    @Test
    fun evaluate_nullPayloadIsInvalidCode() {
        assertEquals(CheckInResult.INVALID_CODE, CheckInPolicy.evaluate(null, "evt-1", testRegistration()))
    }

    @Test
    fun evaluate_codeFromAnotherEvent() {
        assertEquals(CheckInResult.WRONG_EVENT, CheckInPolicy.evaluate(payload, "evt-2", testRegistration()))
    }

    @Test
    fun evaluate_missingRegistration() {
        assertEquals(CheckInResult.NOT_FOUND, CheckInPolicy.evaluate(payload, "evt-1", null))
    }

    @Test
    fun evaluate_waitlistedStudentCannotCheckIn() {
        val waiting = testRegistration(status = RegistrationStatus.WAITLIST)
        assertEquals(CheckInResult.NOT_CONFIRMED, CheckInPolicy.evaluate(payload, "evt-1", waiting))
    }

    @Test
    fun evaluate_tokenMismatch() {
        val other = testRegistration(token = "outro")
        assertEquals(CheckInResult.INVALID_TOKEN, CheckInPolicy.evaluate(payload, "evt-1", other))
    }

    @Test
    fun evaluate_emptyStoredTokenIsInvalid() {
        val empty = testRegistration(token = "")
        assertEquals(CheckInResult.INVALID_TOKEN, CheckInPolicy.evaluate(payload, "evt-1", empty))
    }

    @Test
    fun evaluate_eachRegistrationCanBeValidatedOnlyOnce() {
        val used = testRegistration(checkedIn = true)
        assertEquals(CheckInResult.ALREADY_USED, CheckInPolicy.evaluate(payload, "evt-1", used))
    }

    @Test
    fun message_existsForEveryResult() {
        CheckInResult.values().forEach {
            assertTrue("sem mensagem para $it", CheckInPolicy.message(it).isNotBlank())
        }
        assertTrue(CheckInPolicy.message(CheckInResult.ALREADY_USED).contains("já"))
    }

    @Test
    fun outcome_isAValueObject() {
        assertEquals(CheckInOutcome(CheckInResult.VALID, "Ana"), CheckInOutcome(CheckInResult.VALID, "Ana"))
    }
}