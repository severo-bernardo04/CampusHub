package com.example.campushub.support

import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before

/** Base dos testes de integração e E2E: usa o emulador e começa cada teste com dados limpos. */
abstract class FirebaseEmulatorTest {

    @Before
    fun resetEmulators() {
        FirebaseEmulator.configure()
        FirebaseEmulator.reset()
    }

    @After
    fun endSession() {
        FirebaseAuth.getInstance().signOut()
    }
}