package com.example.campushub

import com.example.campushub.domain.RegistrationStatus
import com.example.campushub.model.Event
import com.example.campushub.model.Registration

const val NOW = 1_800_000_000_000L
const val DAY_MILLIS = 24L * 60 * 60 * 1000

fun testEvent(
    id: String = "id",
    title: String = "Evento de teste",
    category: String = "Palestra",
    hostCourse: String = "Ciência da Computação",
    isPublic: Boolean = true,
    dateMillis: Long = NOW + DAY_MILLIS,
    status: String = "published",
    capacity: Int = 0,
    confirmedCount: Int = 0,
    waitlist: List<String> = emptyList()
) = Event(
    id = id,
    title = title,
    category = category,
    hostCourse = hostCourse,
    isPublic = isPublic,
    dateMillis = dateMillis,
    status = status,
    capacity = capacity,
    confirmedCount = confirmedCount,
    waitlist = waitlist
)

fun testRegistration(
    userId: String = "u1",
    name: String = "Ana",
    status: String = RegistrationStatus.CONFIRMED,
    token: String = "tok",
    checkedIn: Boolean = false,
    createdAt: Long = 1L
) = Registration(
    userId = userId,
    userName = name,
    status = status,
    qrToken = token,
    checkedIn = checkedIn,
    createdAt = createdAt
)