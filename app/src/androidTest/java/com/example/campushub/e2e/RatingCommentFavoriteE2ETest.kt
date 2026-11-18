package com.example.campushub.e2e

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.campushub.LoginActivity
import com.example.campushub.R
import com.example.campushub.support.*
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RatingCommentFavoriteE2ETest : FirebaseEmulatorTest() {

    private val eventTitle = "Torneio de Xadrez"
    private lateinit var student: TestUser

    @Before
    fun createStudentEnrolledInTheEvent() {
        val organizer = createOrganizer()
        runSeed(organizer.uid)
        signOut()

        // O aluno se inscreve pelo mesmo código do app; o evento já passou, então a avaliação abre
        student = createUser(name = "Maria Teste")
        val eventId = await(db.collection("events").whereEqualTo("title", eventTitle).get())
            .documents.single().id
        enrollStudent(student, eventId)
        signOut()
    }

    @Test
    fun studentRatesFinishedEventCommentsAndFavoritesIt() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            // 1. Login pela interface
            onView(withId(R.id.emailInput)).perform(replaceText(student.email))
            onView(withId(R.id.passwordInput)).perform(replaceText(TEST_PASSWORD))
            onView(withId(R.id.loginButton)).perform(scrollTo(), click())

            // 2. Busca o evento encerrado e abre os detalhes
            waitUntil { onView(withId(R.id.eventsRecyclerView)).check(matches(isDisplayed())) }
            onView(withId(R.id.searchInput)).perform(replaceText("Xadrez"))
            waitUntil { onView(withText(eventTitle)).check(matches(isDisplayed())) }
            onView(withText(eventTitle)).perform(click())

            // 3. Avalia com 5 estrelas (aluno inscrito + evento encerrado)
            waitUntil {
                onView(withId(R.id.ratingInput))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            }
            onView(withId(R.id.ratingBar)).perform(scrollTo(), setRating(5f))
            onView(withId(R.id.ratingSubmitButton)).perform(scrollTo(), click())
            waitUntil {
                onView(withId(R.id.ratingSummary)).check(matches(withText(containsString("1 avaliação"))))
            }
            onView(withId(R.id.ratingSummary)).check(matches(withText(containsString("5,0"))))

            // 4. Favorita o evento
            onView(withId(R.id.favoriteButton)).perform(scrollTo(), click())
            waitUntil { onView(withId(R.id.favoriteButton)).check(matches(withText("♥ Favoritado"))) }

            // 5. Comenta
            onView(withId(R.id.commentsButton)).perform(scrollTo(), click())
            waitUntil { onView(withId(R.id.emptyText)).check(matches(isDisplayed())) }
            Thread.sleep(1_000) // dá tempo de carregar o nome do autor
            onView(withId(R.id.commentInput)).perform(replaceText("Ótimo evento!"))
            onView(withId(R.id.sendCommentButton)).perform(click())
            waitUntil { onView(withText("Ótimo evento!")).check(matches(isDisplayed())) }
            onView(withText(student.name)).check(matches(isDisplayed()))

            // 6. O evento aparece em "Meus Favoritos"
            pressBack() // comentários -> detalhes
            pressBack() // detalhes -> lista
            waitUntil { onView(withId(R.id.favoritesButton)).check(matches(isDisplayed())) }
            onView(withId(R.id.favoritesButton)).perform(click())
            waitUntil { onView(withText(eventTitle)).check(matches(isDisplayed())) }
        }

        // Confere no backend que tudo foi persistido
        val eventId = await(db.collection("events").whereEqualTo("title", eventTitle).get())
            .documents.single().id

        val rating = await(
            db.collection("events").document(eventId).collection("ratings").document(student.uid).get()
        )
        assertEquals(5L, rating.getLong("stars"))

        val comments = await(db.collection("events").document(eventId).collection("comments").get())
        assertEquals(1, comments.size())
        val comment = comments.documents.single()
        assertEquals("Ótimo evento!", comment.getString("text"))
        assertEquals(student.uid, comment.getString("authorId"))
        assertEquals(student.name, comment.getString("authorName"))

        val favorite = await(
            db.collection("users").document(student.uid).collection("favorites").document(eventId).get()
        )
        assertTrue(favorite.exists())
    }
}