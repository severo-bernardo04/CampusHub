package com.example.campushub

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
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

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val courseSpinner = findViewById<Spinner>(R.id.courseSpinner)
        val registerButton = findViewById<Button>(R.id.registerButton)

        ArrayAdapter.createFromResource(
            this, R.array.course_list, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            courseSpinner.adapter = adapter
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val course = courseSpinner.selectedItem.toString()

            auth.createUserWithEmailAndPassword(email, passwordInput.text.toString())
                .addOnCompleteListener {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = AppUser(name = name, email = email, course = course)
                    db.collection("users").document(uid).set(user)
                        .addOnCompleteListener { finish() }
                }
        }
    }
}