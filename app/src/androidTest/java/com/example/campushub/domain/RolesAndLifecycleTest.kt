package com.example.campushub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesAndLifecycleTest {

    @Test
    fun userRole_identifiesOrganizers() {
        assertTrue(UserRole.isOrganizer(UserRole.ORGANIZER))
        assertFalse(UserRole.isOrganizer(UserRole.STUDENT))
        assertFalse(UserRole.isOrganizer(null))
    }

    @Test
    fun userRole_fromLabel_defaultsToStudent() {
        assertEquals(UserRole.ORGANIZER, UserRole.fromLabel("Organizador"))
        assertEquals(UserRole.ORGANIZER, UserRole.fromLabel(" organizador "))
        assertEquals(UserRole.STUDENT, UserRole.fromLabel("Aluno"))
        assertEquals(UserRole.STUDENT, UserRole.fromLabel("qualquer coisa"))
    }

    @Test
    fun userRole_label() {
        assertEquals("Organizador", UserRole.label(UserRole.ORGANIZER))
        assertEquals("Aluno", UserRole.label(UserRole.STUDENT))
        assertEquals("Aluno", UserRole.label(null))
    }

    @Test
    fun eventStatus_label() {
        assertEquals("Rascunho", EventStatus.label(EventStatus.DRAFT))
        assertEquals("Publicado", EventStatus.label(EventStatus.PUBLISHED))
        assertEquals("Encerrado", EventStatus.label(EventStatus.CLOSED))
    }

    @Test
    fun lifecycle_onlyDraftsCanBePublished() {
        assertTrue(EventLifecycle.canPublish(EventStatus.DRAFT))
        assertFalse(EventLifecycle.canPublish(EventStatus.PUBLISHED))
        assertFalse(EventLifecycle.canPublish(EventStatus.CLOSED))
    }

    @Test
    fun lifecycle_onlyPublishedEventsCanBeClosed() {
        assertTrue(EventLifecycle.canClose(EventStatus.PUBLISHED))
        assertFalse(EventLifecycle.canClose(EventStatus.DRAFT))
        assertFalse(EventLifecycle.canClose(EventStatus.CLOSED))
    }

    @Test
    fun lifecycle_closedEventsCannotBeEdited() {
        assertTrue(EventLifecycle.canEdit(EventStatus.DRAFT))
        assertTrue(EventLifecycle.canEdit(EventStatus.PUBLISHED))
        assertFalse(EventLifecycle.canEdit(EventStatus.CLOSED))
    }

    @Test
    fun lifecycle_studentsDoNotSeeDrafts() {
        assertFalse(EventLifecycle.isVisibleToStudents(EventStatus.DRAFT))
        assertTrue(EventLifecycle.isVisibleToStudents(EventStatus.PUBLISHED))
        assertTrue(EventLifecycle.isVisibleToStudents(EventStatus.CLOSED))
    }

    @Test
    fun registrationStatus_label() {
        assertEquals("Confirmado", RegistrationStatus.label(RegistrationStatus.CONFIRMED))
        assertEquals("Lista de espera", RegistrationStatus.label(RegistrationStatus.WAITLIST))
    }
}