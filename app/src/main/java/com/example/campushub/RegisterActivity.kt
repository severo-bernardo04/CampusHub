package com.example.campushub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.domain.UserRole
import com.example.campushub.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val courseSpinner = findViewById<Spinner>(R.id.courseSpinner)
        val roleSpinner = findViewById<Spinner>(R.id.roleSpinner)
        val registerButton = findViewById<Button>(R.id.registerButton)

        setupSpinner(courseSpinner, R.array.course_list)
        setupSpinner(roleSpinner, R.array.role_list)

        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val course = courseSpinner.selectedItem.toString()
            val role = UserRole.fromLabel(roleSpinner.selectedItem.toString())

            auth.createUserWithEmailAndPassword(email, passwordInput.text.toString())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("REGISTER", "Falhou", task.exception)
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Falha no cadastro",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = AppUser(name = name, email = email, course = course, role = role)
                    db.collection("users").document(uid).set(user)
                        .addOnCompleteListener { finish() }
                }
        }
    }

    private fun setupSpinner(spinner: Spinner, arrayRes: Int) {
        ArrayAdapter.createFromResource(
            this, arrayRes, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }
}