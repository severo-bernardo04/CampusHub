package com.example.campushub.support

import com.example.campushub.RegistrationRepository
import com.example.campushub.SeedEvents
import com.example.campushub.model.AppUser
import com.example.campushub.model.RegistrationUser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

const val TEST_PASSWORD = "senha123"
private const val TIMEOUT_SECONDS = 20L

val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

data class TestUser(val uid: String, val name: String, val email: String)

/** Espera uma Task do Firebase terminar (não use na thread principal). */
fun <T> await(task: Task<T>): T = Tasks.await(task, TIMEOUT_SECONDS, TimeUnit.SECONDS)

fun uniqueEmail(): String = "aluno_${UUID.randomUUID().toString().take(8)}@campushub.test"

/** Cria a conta no Auth e o perfil no Firestore. O usuário criado fica logado. */
fun createUser(
    name: String = "Aluno Teste",
    email: String = uniqueEmail(),
    course: String = "Ciência da Computação",
    role: String = "student"
): TestUser {
    val uid = await(auth.createUserWithEmailAndPassword(email, TEST_PASSWORD)).user!!.uid
    await(
        db.collection("users").document(uid)
            .set(AppUser(name = name, email = email, course = course, role = role))
    )
    return TestUser(uid, name, email)
}

/** Organizador: único perfil que pode gravar em /events (regras do Firestore). */
fun createOrganizer(name: String = "Organizador Teste"): TestUser =
    createUser(name = name, role = "organizer")

fun signIn(user: TestUser) {
    await(auth.signInWithEmailAndPassword(user.email, TEST_PASSWORD))
}

fun signOut() = auth.signOut()

/** Executa o seed como o organizador informado e espera terminar. */
fun runSeed(organizerId: String) {
    val latch = CountDownLatch(1)
    SeedEvents.run(db, organizerId) { latch.countDown() }
    assertTrue("O seed não terminou a tempo", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
}

/** Inscreve o aluno pelo mesmo código do app (precisa estar logado como ele). */
fun enrollStudent(user: TestUser, eventId: String) {
    val latch = CountDownLatch(1)
    var failure: Throwable? = null
    RegistrationRepository.enroll(db, RegistrationUser(user.uid, user.name, user.email), eventId) {
        failure = it.exceptionOrNull()
        latch.countDown()
    }
    assertTrue("A inscrição não terminou a tempo", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    assertNull("Falha ao inscrever: $failure", failure)
}

/** Garante que a operação foi barrada pelas regras de segurança do Firestore. */
fun assertPermissionDenied(block: () -> Unit) {
    try {
        block()
    } catch (e: ExecutionException) {
        val cause = e.cause
        assertTrue(
            "Esperado PERMISSION_DENIED, mas foi: $cause",
            cause is FirebaseFirestoreException &&
                    cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
        return
    }
    fail("Esperado PERMISSION_DENIED, mas a operação foi permitida")
}