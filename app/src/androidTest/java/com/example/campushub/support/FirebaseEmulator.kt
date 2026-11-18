package com.example.campushub.support

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL

/** Aponta o app para o Firebase Emulator Suite rodando no computador (10.0.2.2 = host do emulador). */
object FirebaseEmulator {
    private const val HOST = "10.0.2.2"
    private const val FIRESTORE_PORT = 8080
    private const val AUTH_PORT = 9099

    private var configured = false

    @Synchronized
    fun configure() {
        if (configured) return
        FirebaseFirestore.getInstance().useEmulator(HOST, FIRESTORE_PORT)
        FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
        configured = true
    }

    /** Apaga todos os usuários e documentos, deixando cada teste com estado limpo. */
    fun reset() {
        FirebaseAuth.getInstance().signOut()
        val projectId = FirebaseApp.getInstance().options.projectId
            ?: error("google-services.json sem project_id")

        clear("http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/(default)/documents")
        clear("http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")
    }

    private fun clear(url: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            val code = connection.responseCode
            check(code in 200..299) { "Falha ao limpar o emulador ($code): $url" }
        } finally {
            connection.disconnect()
        }
    }
}