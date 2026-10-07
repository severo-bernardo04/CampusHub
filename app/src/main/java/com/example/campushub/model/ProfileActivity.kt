package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.campushub.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailText = findViewById<EditText>(R.id.emailInput)
        val courseSpinner = findViewById<Spinner>(R.id.courseSpinner)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        val courseAdapter = ArrayAdapter.createFromResource(
            this, R.array.course_list, android.R.layout.simple_spinner_item
        )
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = courseAdapter

        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            val user = doc.toObject(AppUser::class.java) ?: return@addOnSuccessListener
            nameInput.setText(user.name)
            emailText.setText(user.email)
            courseSpinner.setSelection(courseAdapter.getPosition(user.course))
        }

        saveButton.setOnClickListener {
            val updated = AppUser(
                name = nameInput.text.toString(),
                email = emailText.text.toString(),
                course = courseSpinner.selectedItem.toString()
            )
            userRef.set(updated).addOnCompleteListener { finish() }
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}