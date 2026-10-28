package com.example.campushub

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.AppUser
import com.example.campushub.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var commentInput: EditText

    private var myName = ""

    private val commentsRef: CollectionReference
        get() = db.collection("events").document(eventId).collection("comments")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.commentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.emptyText)
        commentInput = findViewById(R.id.commentInput)

        findViewById<Button>(R.id.sendCommentButton).setOnClickListener { postComment() }

        loadMyName()
        loadComments()
    }

    private fun loadMyName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            myName = doc.toObject(AppUser::class.java)?.name.orEmpty()
        }
    }

    private fun loadComments() {
        val uid = auth.currentUser?.uid ?: return

        commentsRef.orderBy("createdAt", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                val comments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.apply { id = doc.id }
                }
                emptyText.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE

                recyclerView.adapter = CommentAdapter(
                    comments = comments,
                    myUid = uid,
                    onEdit = { editComment(it) },
                    onDelete = { confirmDelete(it) }
                )
            }
            .addOnFailureListener { toast(it.message) }
    }

    private fun postComment() {
        val user = auth.currentUser ?: return
        val text = commentInput.text.toString().trim()
        if (text.isEmpty()) return

        val comment = Comment(
            authorId = user.uid,
            authorName = myName.ifEmpty { user.email ?: "Aluno" },
            text = text,
            createdAt = System.currentTimeMillis()
        )

        commentsRef.add(comment)
            .addOnSuccessListener {
                commentInput.text.clear()
                loadComments()
            }
            .addOnFailureListener { toast(it.message) }
    }

    private fun editComment(comment: Comment) {
        val input = EditText(this).apply {
            setText(comment.text)
            setSelection(comment.text.length)
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    commentsRef.document(comment.id)
                        .update(mapOf("text" to newText, "updatedAt" to System.currentTimeMillis()))
                        .addOnSuccessListener { loadComments() }
                        .addOnFailureListener { toast(it.message) }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("Excluir comentário")
            .setMessage("Tem certeza que deseja excluir este comentário?")
            .setPositiveButton("Excluir") { _, _ ->
                commentsRef.document(comment.id).delete()
                    .addOnSuccessListener { loadComments() }
                    .addOnFailureListener { toast(it.message) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message ?: "Erro", Toast.LENGTH_LONG).show()
    }
}