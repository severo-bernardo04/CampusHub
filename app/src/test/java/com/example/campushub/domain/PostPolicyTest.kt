package com.example.campushub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostPolicyTest {

    @Test
    fun displayName_prefersTheName() {
        assertEquals("Ana", PostPolicy.displayName("Ana", "ana@x.com"))
    }

    @Test
    fun displayName_fallsBackToEmail() {
        assertEquals("ana@x.com", PostPolicy.displayName("", "ana@x.com"))
    }

    @Test
    fun displayName_fallsBackToGenericLabel() {
        assertEquals("Aluno", PostPolicy.displayName("", null))
    }

    @Test
    fun isOwner_trueForSameUser() {
        assertTrue(PostPolicy.isOwner("uid-1", "uid-1"))
    }

    @Test
    fun isOwner_falseForAnotherUser() {
        assertFalse(PostPolicy.isOwner("uid-1", "uid-2"))
    }

    @Test
    fun isOwner_falseWhenNobodyIsLoggedIn() {
        assertFalse(PostPolicy.isOwner("uid-1", null))
    }

    @Test
    fun cleanText_trimsSpaces() {
        assertEquals("olá", PostPolicy.cleanText("  olá  "))
    }

    @Test
    fun cleanText_blankBecomesNull() {
        assertNull(PostPolicy.cleanText("   "))
        assertNull(PostPolicy.cleanText(""))
    }

    @Test
    fun cleanText_keepsInnerSpaces() {
        assertEquals("muito bom evento", PostPolicy.cleanText(" muito bom evento "))
    }

    @Test
    fun isVideoTooLarge_falseAtTheLimit() {
        assertFalse(PostPolicy.isVideoTooLarge(PostPolicy.MAX_VIDEO_BYTES))
        assertFalse(PostPolicy.isVideoTooLarge(0L))
    }

    @Test
    fun isVideoTooLarge_trueAboveTheLimit() {
        assertTrue(PostPolicy.isVideoTooLarge(PostPolicy.MAX_VIDEO_BYTES + 1))
    }

    @Test
    fun isEdited_falseWhenNeverUpdated() {
        assertFalse(PostPolicy.isEdited(0L))
    }

    @Test
    fun isEdited_trueWhenUpdated() {
        assertTrue(PostPolicy.isEdited(1L))
    }
}