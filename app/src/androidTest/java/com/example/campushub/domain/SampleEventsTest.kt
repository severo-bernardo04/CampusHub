package com.example.campushub.domain

import com.example.campushub.DAY_MILLIS
import com.example.campushub.NOW
import com.example.campushub.isFinished
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class SampleEventsTest {

    private val validCategories = setOf(
        "Palestra", "Workshop", "Feira", "Visita Técnica", "Cultural", "Esporte", "Social"
    )
    private val validCourses = setOf(
        "Ciência da Computação", "Engenharia Civil", "Administração", "Direito", "Medicina", "Outro"
    )

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun useUtc() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun build_returnsElevenEvents() {
        assertEquals(11, SampleEvents.build(NOW).size)
    }

    @Test
    fun build_hasUniqueTitles() {
        val titles = SampleEvents.build(NOW).map { it.title }
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun build_hasSevenUpcomingAndFourFinishedEvents() {
        val events = SampleEvents.build(NOW)
        assertEquals(7, events.count { !it.isFinished(NOW) })
        assertEquals(4, events.count { it.isFinished(NOW) })
    }

    @Test
    fun build_usesOnlyValidCategories() {
        assertTrue(SampleEvents.build(NOW).all { it.category in validCategories })
    }

    @Test
    fun build_usesOnlyValidCourses() {
        assertTrue(SampleEvents.build(NOW).all { it.hostCourse in validCourses })
    }

    @Test
    fun build_everyEventHasAddressAndCoordinates() {
        SampleEvents.build(NOW).forEach {
            assertTrue("${it.title} sem endereço", it.address.isNotBlank())
            assertTrue("${it.title} sem latitude", it.latitude != 0.0)
            assertTrue("${it.title} sem longitude", it.longitude != 0.0)
        }
    }

    @Test
    fun build_containsPublicAndClosedEvents() {
        val events = SampleEvents.build(NOW)
        assertEquals(8, events.count { it.isPublic })
        assertEquals(3, events.count { !it.isPublic })
    }

    @Test
    fun build_datesFollowTheGivenClock() {
        val today = SampleEvents.build(NOW)
        val inThirtyDays = SampleEvents.build(NOW + 30 * DAY_MILLIS)

        today.indices.forEach { i ->
            assertEquals(30 * DAY_MILLIS, inThirtyDays[i].dateMillis - today[i].dateMillis)
        }
    }

    @Test
    fun build_defaultClockProducesUpcomingAndFinishedEvents() {
        val events = SampleEvents.build()
        assertEquals(7, events.count { !it.isFinished() })
        assertEquals(4, events.count { it.isFinished() })
    }

    @Test
    fun build_usesRealSantiagoAddressesInsideTheCity() {
        SampleEvents.build(NOW).forEach {
            assertTrue("${it.title}: endereço fora de Santiago", it.address.contains("Santiago - RS"))
            assertTrue("${it.title}: latitude fora da cidade", it.latitude in -29.30..-29.10)
            assertTrue("${it.title}: longitude fora da cidade", it.longitude in -54.95..-54.80)
        }
    }

    @Test
    fun build_eventsAreOwnedPublishedAndHaveCapacity() {
        val events = SampleEvents.build(NOW, organizerId = "org-1")
        assertTrue(events.all { it.organizerId == "org-1" })
        assertTrue(events.all { it.status == EventStatus.PUBLISHED })
        assertTrue(events.all { it.capacity > 0 })
    }

    @Test
    fun build_hasAnEventWithTinyCapacityToTryTheWaitlist() {
        assertTrue(SampleEvents.build(NOW).any { it.capacity == 2 && !it.isFinished(NOW) })
    }
}