package com.example.campushub

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FormattersTest {

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

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun toDateTimeString_formatsDayMonthYearAndTime() {
        val millis = utcMillis(2026, Calendar.NOVEMBER, 5, 19, 30)
        assertEquals("05/11/2026 às 19:30", millis.toDateTimeString())
    }

    @Test
    fun toDateTimeString_padsSingleDigits() {
        val millis = utcMillis(2026, Calendar.JANUARY, 2, 3, 4)
        assertEquals("02/01/2026 às 03:04", millis.toDateTimeString())
    }

    @Test
    fun isFinished_trueWhenEventDateIsInThePast() {
        assertTrue(testEvent(dateMillis = NOW - 1).isFinished(NOW))
    }

    @Test
    fun isFinished_falseWhenEventDateIsInTheFuture() {
        assertFalse(testEvent(dateMillis = NOW + 1).isFinished(NOW))
    }

    @Test
    fun isFinished_falseWhenEventStartsExactlyNow() {
        assertFalse(testEvent(dateMillis = NOW).isFinished(NOW))
    }

    @Test
    fun isFinished_usesCurrentTimeByDefault() {
        val past = testEvent(dateMillis = System.currentTimeMillis() - DAY_MILLIS)
        val future = testEvent(dateMillis = System.currentTimeMillis() + DAY_MILLIS)
        assertTrue(past.isFinished())
        assertFalse(future.isFinished())
    }

    @Test
    fun formattedDate_usesEventDate() {
        val millis = utcMillis(2026, Calendar.DECEMBER, 10, 19, 0)
        assertEquals("10/12/2026 às 19:00", testEvent(dateMillis = millis).formattedDate())
    }

    @Test
    fun normalized_removesAccents() {
        assertEquals("ciencia da computacao", "Ciência da Computação".normalized())
        assertEquals("confraternizacao", "Confraternização".normalized())
    }

    @Test
    fun normalized_lowercasesText() {
        assertEquals("palestra", "PALESTRA".normalized())
    }

    @Test
    fun normalized_keepsDigitsAndPunctuation() {
        assertEquals("visita tecnica: obra 2", "Visita Técnica: Obra 2".normalized())
    }

    @Test
    fun normalized_ofEmptyStringIsEmpty() {
        assertEquals("", "".normalized())
    }
}