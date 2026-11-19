package com.example.campushub.domain

import com.example.campushub.DAY_MILLIS
import com.example.campushub.NOW
import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFormValidatorTest {

    private fun valid() = EventFormInput(
        title = "Semana da Computação",
        description = "Palestras e oficinas",
        category = "Palestra",
        dateMillis = NOW + DAY_MILLIS,
        address = "Av. Batista Bonotto Sobrinho, 733, Santiago - RS",
        capacityText = "80",
        latitude = -29.18995,
        longitude = -54.88577
    )

    private fun errors(input: EventFormInput, publishing: Boolean = true) =
        EventFormValidator.validate(input, publishing, NOW)

    @Test
    fun validInput_hasNoErrors() {
        assertTrue(errors(valid()).isEmpty())
    }

    @Test
    fun shortTitle_isRejected() {
        assertTrue(errors(valid().copy(title = " ab ")).containsKey(FormField.TITLE))
    }

    @Test
    fun blankDescriptionCategoryAndAddress_areRejected() {
        val result = errors(valid().copy(description = " ", category = "", address = ""))
        assertTrue(result.containsKey(FormField.DESCRIPTION))
        assertTrue(result.containsKey(FormField.CATEGORY))
        assertTrue(result.containsKey(FormField.ADDRESS))
    }

    @Test
    fun capacity_mustBeBetweenOneAndTenThousand() {
        assertNull(EventFormValidator.parseCapacity("0"))
        assertNull(EventFormValidator.parseCapacity("abc"))
        assertNull(EventFormValidator.parseCapacity("10001"))
        assertEquals(1, EventFormValidator.parseCapacity(" 1 "))
        assertEquals(10_000, EventFormValidator.parseCapacity("10000"))
        assertTrue(errors(valid().copy(capacityText = "")).containsKey(FormField.CAPACITY))
    }

    @Test
    fun capacity_cannotBeLowerThanConfirmedStudents() {
        val result = errors(valid().copy(capacityText = "5", confirmedCount = 8))
        assertTrue(result.getValue(FormField.CAPACITY).contains("8"))
        assertFalse(errors(valid().copy(capacityText = "8", confirmedCount = 8)).containsKey(FormField.CAPACITY))
    }

    @Test
    fun publishing_requiresAFutureDate() {
        val past = valid().copy(dateMillis = NOW - DAY_MILLIS)
        assertTrue(errors(past, publishing = true).containsKey(FormField.DATE))
    }

    @Test
    fun draft_acceptsAPastDate() {
        val past = valid().copy(dateMillis = NOW - DAY_MILLIS)
        assertFalse(errors(past, publishing = false).containsKey(FormField.DATE))
    }

    @Test
    fun missingDate_isAlwaysRejected() {
        assertTrue(errors(valid().copy(dateMillis = 0), publishing = false).containsKey(FormField.DATE))
    }

    @Test
    fun publishing_requiresTheLocationToBeConfirmedOnTheMap() {
        val noMap = valid().copy(latitude = 0.0, longitude = 0.0)
        assertTrue(errors(noMap, publishing = true).containsKey(FormField.MAP_LOCATION))
        assertFalse(errors(noMap, publishing = false).containsKey(FormField.MAP_LOCATION))
    }

    @Test
    fun inputFrom_copiesTheEventFields() {
        val event = testEvent(title = "Feira", capacity = 40, confirmedCount = 3)
        val input = EventFormValidator.inputFrom(event)
        assertEquals("Feira", input.title)
        assertEquals("40", input.capacityText)
        assertEquals(3, input.confirmedCount)
    }
}