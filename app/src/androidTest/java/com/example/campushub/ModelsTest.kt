package com.example.campushub

import com.example.campushub.model.AppUser
import com.example.campushub.model.Comment
import com.example.campushub.model.Event
import com.example.campushub.model.MediaPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun event_hasSafeDefaults() {
        val event = Event()
        assertEquals("", event.id)
        assertEquals("", event.title)
        assertEquals(0.0, event.latitude, 0.0)
        assertEquals(0.0, event.longitude, 0.0)
        assertEquals(0L, event.dateMillis)
        assertTrue(event.isPublic)
    }

    @Test
    fun event_copyChangesOnlyTheGivenField() {
        val original = testEvent(id = "a", title = "Original")
        val copy = original.copy(title = "Novo")
        assertEquals("Novo", copy.title)
        assertEquals(original.id, copy.id)
        assertEquals(original.hostCourse, copy.hostCourse)
    }

    @Test
    fun event_equalityIsBasedOnValues() {
        assertEquals(testEvent(id = "a"), testEvent(id = "a"))
        assertNotEquals(testEvent(id = "a"), testEvent(id = "b"))
        assertEquals(testEvent(id = "a").hashCode(), testEvent(id = "a").hashCode())
    }

    @Test
    fun comment_hasSafeDefaults() {
        val comment = Comment()
        assertEquals("", comment.authorId)
        assertEquals("", comment.text)
        assertEquals(0L, comment.createdAt)
        assertEquals(0L, comment.updatedAt)
    }

    @Test
    fun comment_copyMarksCommentAsEdited() {
        val comment = Comment(text = "antes", createdAt = 1L)
        val edited = comment.copy(text = "depois", updatedAt = 2L)
        assertEquals("depois", edited.text)
        assertEquals(2L, edited.updatedAt)
        assertEquals(1L, edited.createdAt)
    }

    @Test
    fun mediaPost_exposesTypeConstants() {
        assertEquals("photo", MediaPost.TYPE_PHOTO)
        assertEquals("audio", MediaPost.TYPE_AUDIO)
        assertEquals("video", MediaPost.TYPE_VIDEO)
    }

    @Test
    fun mediaPost_equalityIsBasedOnValues() {
        val a = MediaPost(1, "e", "u", "Ana", MediaPost.TYPE_PHOTO, "/p.jpg", 10L)
        val b = MediaPost(1, "e", "u", "Ana", MediaPost.TYPE_PHOTO, "/p.jpg", 10L)
        assertEquals(a, b)
        assertNotEquals(a, b.copy(type = MediaPost.TYPE_AUDIO))
    }

    @Test
    fun appUser_hasSafeDefaults() {
        val user = AppUser()
        assertEquals("", user.name)
        assertEquals("", user.email)
        assertEquals("", user.course)
    }

    @Test
    fun appUser_equalityIsBasedOnValues() {
        assertEquals(AppUser("Ana", "a@x.com", "Direito"), AppUser("Ana", "a@x.com", "Direito"))
        assertNotEquals(AppUser("Ana", "a@x.com", "Direito"), AppUser("Ana", "a@x.com", "Medicina"))
    }
}