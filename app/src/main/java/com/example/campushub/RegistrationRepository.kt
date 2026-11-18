package com.example.campushub

import com.example.campushub.domain.CapacityPolicy
import com.example.campushub.domain.CheckInOutcome
import com.example.campushub.domain.CheckInPolicy
import com.example.campushub.domain.CheckInResult
import com.example.campushub.domain.QrPayload
import com.example.campushub.domain.RegistrationStatus
import com.example.campushub.model.Event
import com.example.campushub.model.Registration
import com.example.campushub.model.RegistrationUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID

object RegistrationRepository {

    private fun eventRef(db: FirebaseFirestore, eventId: String): DocumentReference =
        db.collection("events").document(eventId)

    private fun registrationRef(db: FirebaseFirestore, eventId: String, uid: String) =
        eventRef(db, eventId).collection("registrations").document(uid)

    private fun enrollmentRef(db: FirebaseFirestore, uid: String, eventId: String) =
        db.collection("users").document(uid).collection("enrollments").document(eventId)

    /** Inscreve o aluno: confirmado se há vaga e a fila está vazia; senão, lista de espera. */
    fun enroll(
        db: FirebaseFirestore,
        user: RegistrationUser,
        eventId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val event = eventRef(db, eventId)
        val registration = registrationRef(db, eventId, user.uid)
        val enrollment = enrollmentRef(db, user.uid, eventId)

        db.runTransaction { tx ->
            val current = tx.get(event).toObject(Event::class.java)
                ?: throw IllegalStateException("Evento não encontrado")
            if (tx.get(registration).exists()) {
                throw IllegalStateException("Você já está inscrito neste evento")
            }

            val status = if (CapacityPolicy.canJoinDirectly(current)) {
                RegistrationStatus.CONFIRMED
            } else {
                RegistrationStatus.WAITLIST
            }

            tx.set(
                registration,
                Registration(
                    userId = user.uid,
                    userName = user.name,
                    userEmail = user.email,
                    status = status,
                    createdAt = System.currentTimeMillis(),
                    qrToken = if (status == RegistrationStatus.CONFIRMED) {
                        UUID.randomUUID().toString()
                    } else ""
                )
            )
            tx.set(enrollment, mapOf("eventId" to eventId, "status" to status))

            if (status == RegistrationStatus.CONFIRMED) {
                tx.update(event, "confirmedCount", FieldValue.increment(1))
            } else {
                tx.update(event, "waitlist", FieldValue.arrayUnion(user.uid))
            }
            status
        }
            .addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Cancela a inscrição (libera a vaga) ou sai da lista de espera. */
    fun cancel(
        db: FirebaseFirestore,
        uid: String,
        eventId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val event = eventRef(db, eventId)
        val registration = registrationRef(db, eventId, uid)
        val enrollment = enrollmentRef(db, uid, eventId)

        db.runTransaction { tx ->
            val current = tx.get(registration).toObject(Registration::class.java)
                ?: throw IllegalStateException("Inscrição não encontrada")

            tx.delete(registration)
            tx.delete(enrollment)
            if (current.status == RegistrationStatus.CONFIRMED) {
                tx.update(event, "confirmedCount", FieldValue.increment(-1))
            } else {
                tx.update(event, "waitlist", FieldValue.arrayRemove(uid))
            }
            "cancelled"
        }
            .addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** O primeiro da fila confirma a inscrição quando uma vaga fica disponível. */
    fun confirmFromWaitlist(
        db: FirebaseFirestore,
        uid: String,
        eventId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val event = eventRef(db, eventId)
        val registration = registrationRef(db, eventId, uid)
        val enrollment = enrollmentRef(db, uid, eventId)

        db.runTransaction { tx ->
            val current = tx.get(event).toObject(Event::class.java)
                ?: throw IllegalStateException("Evento não encontrado")
            val mine = tx.get(registration).toObject(Registration::class.java)
                ?: throw IllegalStateException("Inscrição não encontrada")

            if (mine.status != RegistrationStatus.WAITLIST ||
                !CapacityPolicy.canConfirmFromWaitlist(current, uid)
            ) {
                throw IllegalStateException("A vaga ainda não está disponível para você")
            }

            tx.update(
                registration,
                mapOf("status" to RegistrationStatus.CONFIRMED, "qrToken" to UUID.randomUUID().toString())
            )
            tx.set(
                enrollment,
                mapOf("eventId" to eventId, "status" to RegistrationStatus.CONFIRMED),
                SetOptions.merge()
            )
            tx.update(
                event,
                mapOf(
                    "confirmedCount" to FieldValue.increment(1),
                    "waitlist" to FieldValue.arrayRemove(uid)
                )
            )
            RegistrationStatus.CONFIRMED
        }
            .addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun loadOwn(
        db: FirebaseFirestore,
        uid: String,
        eventId: String,
        onResult: (Registration?) -> Unit
    ) {
        registrationRef(db, eventId, uid).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(Registration::class.java)?.apply { id = doc.id })
            }
            .addOnFailureListener { onResult(null) }
    }

    /** Só o organizador do evento consegue ler esta lista (regras do Firestore). */
    fun loadAll(
        db: FirebaseFirestore,
        eventId: String,
        onResult: (List<Registration>) -> Unit
    ) {
        eventRef(db, eventId).collection("registrations").get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Registration::class.java)?.apply { id = doc.id }
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /** Registra a presença em transação: se já foi usado, a segunda leitura é recusada. */
    fun checkIn(
        db: FirebaseFirestore,
        eventId: String,
        payload: QrPayload?,
        onResult: (Result<CheckInOutcome>) -> Unit
    ) {
        db.runTransaction { tx ->
            val ref = if (payload != null && payload.eventId == eventId) {
                registrationRef(db, eventId, payload.userId)
            } else null

            val registration = ref?.let { tx.get(it).toObject(Registration::class.java) }
            val result = CheckInPolicy.evaluate(payload, eventId, registration)

            if (result == CheckInResult.VALID && ref != null) {
                tx.update(
                    ref,
                    mapOf("checkedIn" to true, "checkedInAt" to System.currentTimeMillis())
                )
            }
            CheckInOutcome(result, registration?.userName.orEmpty())
        }
            .addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}