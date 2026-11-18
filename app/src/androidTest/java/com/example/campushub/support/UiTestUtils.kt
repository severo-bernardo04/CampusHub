package com.example.campushub.support

import android.os.SystemClock
import android.view.View
import android.widget.RatingBar
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher

/**
 * O Espresso não sabe esperar o Firebase (operações assíncronas em rede).
 * Repete a verificação até passar ou estourar o tempo.
 */
fun waitUntil(timeoutMs: Long = 20_000, assertion: () -> Unit) {
    val end = SystemClock.uptimeMillis() + timeoutMs
    var lastError: Throwable? = null
    while (SystemClock.uptimeMillis() < end) {
        try {
            assertion()
            return
        } catch (e: Throwable) {
            lastError = e
            Thread.sleep(300)
        }
    }
    throw AssertionError("Condição não atendida em ${timeoutMs}ms", lastError)
}

fun setRating(value: Float): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RatingBar::class.java)
    override fun getDescription(): String = "Define a avaliação como $value"
    override fun perform(uiController: UiController, view: View) {
        (view as RatingBar).rating = value
    }
}