package com.example.campushub.e2e

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.campushub.LoginActivity
import com.example.campushub.R
import com.example.campushub.support.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnrollmentFlowE2ETest : FirebaseEmulatorTest() {

    private val eventTitle = "Semana da Computação"

    @Before
    fun seedEventsWithoutEnrollments() {
        // Só organizador grava em /events (regras); depois voltamos ao estado deslogado
        val organizer = createOrganizer()
        runSeed(organizer.uid)
        signOut()
    }

    @Test
    fun newStudentRegistersFindsAnEventEnrollsAndCancels() {
        val email = uniqueEmail()

        ActivityScenario.launch(LoginActivity::class.java).use {
            // 1. Cadastro pela interface (perfil padrão: Aluno)
            onView(withId(R.id.registerLink)).perform(click())
            onView(withId(R.id.nameInput)).perform(replaceText("Bernardo Teste"))
            onView(withId(R.id.emailInput)).perform(replaceText(email))
            onView(withId(R.id.passwordInput)).perform(replaceText(TEST_PASSWORD))
            onView(withId(R.id.registerButton)).perform(scrollTo(), click())

            // 2. O app volta ao login, reconhece a sessão e abre a lista de eventos
            waitUntil { onView(withId(R.id.eventsRecyclerView)).check(matches(isDisplayed())) }

            // 3. Busca pelo título
            onView(withId(R.id.searchInput)).perform(replaceText("Semana"))
            waitUntil { onView(withText(eventTitle)).check(matches(isDisplayed())) }
            onView(withText(eventTitle)).perform(click())

            // 4. Inscrição
            waitUntil { onView(withId(R.id.enrollButton)).check(matches(withText("Inscrever-se"))) }
            onView(withId(R.id.enrollButton)).perform(scrollTo(), click())
            waitUntil { onView(withId(R.id.enrollButton)).check(matches(withText("Cancelar inscrição"))) }

            // 5. O evento aparece em "Meus Eventos"
            pressBack()
            waitUntil { onView(withId(R.id.myEventsButton)).check(matches(isDisplayed())) }
            onView(withId(R.id.myEventsButton)).perform(click())
            waitUntil { onView(withText(eventTitle)).check(matches(isDisplayed())) }

            // 6. Cancela a inscrição pelos detalhes
            onView(withText(eventTitle)).perform(click())
            waitUntil { onView(withId(R.id.enrollButton)).check(matches(withText("Cancelar inscrição"))) }
            onView(withId(R.id.enrollButton)).perform(scrollTo(), click())
            waitUntil { onView(withId(R.id.enrollButton)).check(matches(withText("Inscrever-se"))) }

            // 7. "Meus Eventos" volta a ficar vazio
            pressBack()
            waitUntil { onView(withText(eventTitle)).check(doesNotExist()) }
            onView(withText("Você ainda não está inscrito em nenhum evento.")).check(matches(isDisplayed()))
        }

        // Confere no backend que a inscrição realmente foi removida
        val uid = auth.currentUser!!.uid
        val enrollments = await(db.collection("users").document(uid).collection("enrollments").get())
        assertEquals(0, enrollments.size())
    }
}