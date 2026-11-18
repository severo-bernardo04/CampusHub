package com.example.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.campushub.FavoriteRepository
import com.example.campushub.domain.RatingPolicy
import com.example.campushub.isFinished
import com.example.campushub.model.AppUser
import com.example.campushub.model.Comment
import com.example.campushub.model.Event
import com.example.campushub.support.*
import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseIntegrationTest : FirebaseEmulatorTest() {

    // ---------- Authentication ----------

    @Test
    fun it01_registerCreatesAccountInFirebaseAuth() {
        val email = uniqueEmail()

        val result = await(auth.createUserWithEmailAndPassword(email, TEST_PASSWORD))

        assertNotNull(result.user)
        assertEquals(email, auth.currentUser?.email)
    }

    @Test
    fun it02_loginWithValidCredentialsSucceeds() {
        val user = createUser()
        signOut()
        assertNull(auth.currentUser)

        await(auth.signInWithEmailAndPassword(user.email, TEST_PASSWORD))

        assertEquals(user.uid, auth.currentUser?.uid)
    }

    @Test
    fun it03_loginWithWrongPasswordIsRejected() {
        val user = createUser()
        signOut()

        try {
            await(auth.signInWithEmailAndPassword(user.email, "senha-errada"))
            fail("O login com senha errada não deveria funcionar")
        } catch (e: ExecutionException) {
            assertTrue("Erro inesperado: ${e.cause}", e.cause is FirebaseAuthException)
        }

        assertNull(auth.currentUser)
    }

    // ---------- Firestore: perfil e seed ----------

    @Test
    fun it04_userProfileIsSavedAndReadBackFromFirestore() {
        val user = createUser(name = "Maria Silva", course = "Direito")

        val doc = await(db.collection("users").document(user.uid).get())

        assertEquals(
            AppUser(name = "Maria Silva", email = user.email, course = "Direito"),
            doc.toObject(AppUser::class.java)
        )
    }

    @Test
    fun it05_seedCreatesEventsOwnedByTheOrganizerWithUniqueIds() {
        val organizer = createOrganizer()

        runSeed(organizer.uid)

        val events = await(db.collection("events").get()).documents.mapNotNull { doc ->
            doc.toObject(Event::class.java)?.apply { id = doc.id }
        }
        assertEquals(11, events.size)
        assertEquals(11, events.map { it.id }.toSet().size)
        assertTrue(events.none { it.id.startsWith("seed_") })
        assertTrue(events.all { it.organizerId == organizer.uid })
        // isPublic precisa sobreviver à ida e volta (mapeamento @PropertyName)
        assertEquals(3, events.count { !it.isPublic })
        assertEquals(7, events.count { !it.isFinished() })
    }

    @Test
    fun it06_seedIsIdempotent() {
        val organizer = createOrganizer()

        runSeed(organizer.uid)
        runSeed(organizer.uid)

        val events = await(db.collection("events").get())
        assertEquals("O seed não pode duplicar eventos", 11, events.size())
    }

    // ---------- Favoritos ----------

    @Test
    fun it07_favoriteRepositoryAddsLoadsAndRemovesFavorites() {
        val user = createUser()

        setFavorite(user.uid, "evento-1", true)
        setFavorite(user.uid, "evento-2", true)
        assertEquals(setOf("evento-1", "evento-2"), favoriteIds(user.uid))

        setFavorite(user.uid, "evento-1", false)
        assertEquals(setOf("evento-2"), favoriteIds(user.uid))
    }

    // ---------- Regras de segurança ----------

    @Test
    fun it08_commentsCanOnlyBeEditedOrDeletedByTheirAuthor() {
        val author = createUser(name = "Autor")
        val comments = db.collection("events").document("evento-1").collection("comments")
        val ref = await(
            comments.add(Comment(authorId = author.uid, authorName = author.name, text = "Original", createdAt = 1L))
        )

        // Outro aluno consegue ler, mas não alterar
        signOut()
        createUser(name = "Outro aluno")
        assertEquals("Original", await(ref.get()).getString("text"))
        assertPermissionDenied { await(ref.update("text", "Hackeado")) }
        assertPermissionDenied { await(ref.delete()) }

        // O autor edita e exclui o próprio comentário
        signOut()
        signIn(author)
        await(ref.update(mapOf("text" to "Editado", "updatedAt" to 2L)))
        assertEquals("Editado", await(ref.get()).getString("text"))
        await(ref.delete())
        assertFalse(await(ref.get()).exists())
    }

    @Test
    fun it09_ratingIsUniquePerUserAndRejectsInvalidStars() {
        val user = createUser()
        val ratings = db.collection("events").document("evento-1").collection("ratings")
        val myRating = ratings.document(user.uid)

        // Avaliar de novo altera a nota em vez de criar outro documento
        await(myRating.set(mapOf("userId" to user.uid, "stars" to 3, "updatedAt" to 1L)))
        await(myRating.set(mapOf("userId" to user.uid, "stars" to 5, "updatedAt" to 2L)))

        val snapshot = await(ratings.get())
        assertEquals(1, snapshot.size())
        val stars = snapshot.documents.map { (it.getLong("stars") ?: 0L).toInt() }
        assertEquals(5.0, RatingPolicy.summarize(stars).average, 0.0)

        // Nota fora de 1 a 5 é barrada pelas regras
        assertPermissionDenied {
            await(myRating.set(mapOf("userId" to user.uid, "stars" to 6, "updatedAt" to 3L)))
        }
        assertEquals(5L, await(myRating.get()).getLong("stars"))
    }

    @Test
    fun it10_enrollmentsAndFavoritesArePrivateToTheirOwner() {
        val owner = createUser(name = "Dono")
        val enrollment = db.collection("users").document(owner.uid)
            .collection("enrollments").document("evento-1")

        // O dono se inscreve e cancela
        await(enrollment.set(mapOf("eventId" to "evento-1")))
        assertTrue(await(enrollment.get()).exists())
        await(enrollment.delete())
        assertFalse(await(enrollment.get()).exists())
        await(enrollment.set(mapOf("eventId" to "evento-1")))

        // Outro usuário não lê nem escreve nos dados do dono
        signOut()
        createUser(name = "Intruso")
        assertPermissionDenied { await(db.collection("users").document(owner.uid).get()) }
        assertPermissionDenied {
            await(db.collection("users").document(owner.uid).collection("favorites").get())
        }
        assertPermissionDenied { await(enrollment.set(mapOf("eventId" to "evento-1"))) }
        assertPermissionDenied { await(enrollment.delete()) }
    }

    // ---------- Auxiliares ----------

    private fun setFavorite(uid: String, eventId: String, favorite: Boolean) {
        val latch = CountDownLatch(1)
        FavoriteRepository.set(db, uid, eventId, favorite) { latch.countDown() }
        assertTrue(latch.await(20, TimeUnit.SECONDS))
    }

    private fun favoriteIds(uid: String): Set<String> {
        val latch = CountDownLatch(1)
        var result: Set<String> = emptySet()
        FavoriteRepository.loadIds(db, uid) {
            result = it
            latch.countDown()
        }
        assertTrue(latch.await(20, TimeUnit.SECONDS))
        return result
    }
}