package com.example.campushub.domain

import com.example.campushub.DAY_MILLIS
import com.example.campushub.NOW
import com.example.campushub.testEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFilterTest {

    private val semana = testEvent(
        id = "1", title = "Semana da Computação", category = "Palestra",
        hostCourse = "Ciência da Computação", isPublic = true, dateMillis = NOW + 7 * DAY_MILLIS
    )
    private val workshop = testEvent(
        id = "2", title = "Workshop de Segurança da Informação", category = "Workshop",
        hostCourse = "Ciência da Computação", isPublic = false, dateMillis = NOW + 3 * DAY_MILLIS
    )
    private val feira = testEvent(
        id = "3", title = "Feira de Engenharia Civil", category = "Feira",
        hostCourse = "Engenharia Civil", isPublic = true, dateMillis = NOW + 10 * DAY_MILLIS
    )
    private val aula = testEvent(
        id = "4", title = "Aula Inaugural do Semestre", category = "Palestra",
        hostCourse = "Outro", isPublic = true, dateMillis = NOW - 20 * DAY_MILLIS
    )
    private val juri = testEvent(
        id = "5", title = "Simulado de Júri", category = "Workshop",
        hostCourse = "Direito", isPublic = false, dateMillis = NOW - 5 * DAY_MILLIS
    )
    private val xadrez = testEvent(
        id = "6", title = "Torneio de Xadrez", category = "Esporte",
        hostCourse = "Outro", isPublic = true, dateMillis = NOW - 3 * DAY_MILLIS
    )

    // Fora de ordem de propósito: o filtro é quem ordena
    private val all = listOf(aula, semana, juri, feira, xadrez, workshop)

    private fun ids(filters: EventFilters = EventFilters()): List<String> =
        EventFilter.apply(all, filters, NOW).map { it.id }

    @Test
    fun noFilters_returnsUpcomingByDateThenFinishedMostRecentFirst() {
        assertEquals(listOf("2", "1", "3", "6", "5", "4"), ids())
    }

    @Test
    fun query_isCaseInsensitive() {
        assertEquals(listOf("1"), ids(EventFilters(query = "SEMANA")))
    }

    @Test
    fun query_ignoresAccents() {
        assertEquals(listOf("2"), ids(EventFilters(query = "seguranca")))
        assertEquals(listOf("5"), ids(EventFilters(query = "juri")))
    }

    @Test
    fun query_isTrimmed() {
        assertEquals(listOf("6"), ids(EventFilters(query = "  xadrez ")))
    }

    @Test
    fun query_matchesAnywhereInTheTitle() {
        assertEquals(listOf("3"), ids(EventFilters(query = "civil")))
    }

    @Test
    fun query_withNoMatch_returnsEmptyList() {
        assertTrue(ids(EventFilters(query = "inexistente")).isEmpty())
    }

    @Test
    fun blankQuery_doesNotFilter() {
        assertEquals(6, ids(EventFilters(query = "   ")).size)
    }

    @Test
    fun course_filtersByHostCourse() {
        assertEquals(listOf("6", "4"), ids(EventFilters(course = "Outro")))
    }

    @Test
    fun access_publicOnly_hidesClosedEvents() {
        assertEquals(
            listOf("1", "3", "6", "4"),
            ids(EventFilters(access = AccessFilter.PUBLIC_ONLY))
        )
    }

    @Test
    fun access_myCourse_showsOnlyEventsOfTheStudentCourse() {
        val filters = EventFilters(access = AccessFilter.MY_COURSE, myCourse = "Ciência da Computação")
        assertEquals(listOf("2", "1"), ids(filters))
    }

    @Test
    fun access_myCourse_withUnknownCourse_returnsEmptyList() {
        val filters = EventFilters(access = AccessFilter.MY_COURSE, myCourse = "Medicina")
        assertTrue(ids(filters).isEmpty())
    }

    @Test
    fun category_filtersEvents() {
        assertEquals(listOf("2", "5"), ids(EventFilters(category = "Workshop")))
    }

    @Test
    fun category_null_doesNotFilter() {
        assertEquals(6, ids(EventFilters(category = null)).size)
    }

    @Test
    fun situation_upcoming_hidesFinishedEvents() {
        assertEquals(listOf("2", "1", "3"), ids(EventFilters(situation = SituationFilter.UPCOMING)))
    }

    @Test
    fun situation_finished_hidesUpcomingEvents() {
        assertEquals(listOf("6", "5", "4"), ids(EventFilters(situation = SituationFilter.FINISHED)))
    }

    @Test
    fun combinedFilters_applyAllConditions() {
        val finishedPublic = EventFilters(
            situation = SituationFilter.FINISHED,
            access = AccessFilter.PUBLIC_ONLY
        )
        assertEquals(listOf("6", "4"), ids(finishedPublic))
        assertEquals(listOf("6"), ids(finishedPublic.copy(query = "torneio")))
    }

    @Test
    fun emptyInput_returnsEmptyList() {
        assertTrue(EventFilter.apply(emptyList(), EventFilters(), NOW).isEmpty())
    }

    @Test
    fun defaultFilters_haveNoRestrictions() {
        val filters = EventFilters()
        assertEquals("", filters.query)
        assertNull(filters.course)
        assertEquals(AccessFilter.ALL, filters.access)
        assertNull(filters.category)
        assertEquals(SituationFilter.ALL, filters.situation)
        assertEquals("", filters.myCourse)
    }

    @Test
    fun defaultClock_isUsedWhenNowIsOmitted() {
        val past = testEvent(id = "p", dateMillis = System.currentTimeMillis() - DAY_MILLIS)
        val future = testEvent(id = "f", dateMillis = System.currentTimeMillis() + DAY_MILLIS)

        val upcoming = EventFilter.apply(
            listOf(past, future), EventFilters(situation = SituationFilter.UPCOMING)
        )

        assertEquals(listOf("f"), upcoming.map { it.id })
    }
}