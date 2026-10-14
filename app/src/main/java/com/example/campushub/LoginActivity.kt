package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerLink = findViewById<TextView>(R.id.registerLink)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)

        loginButton.setOnClickListener {
            auth.signInWithEmailAndPassword(
                emailInput.text.toString(),
                passwordInput.text.toString()
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, EventsListActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Falha no login",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, EventsListActivity::class.java))
            finish()
        }
    }
}