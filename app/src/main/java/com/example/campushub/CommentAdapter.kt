package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.Comment

class CommentAdapter(
    private val comments: List<Comment>,
    private val myUid: String,
    private val onEdit: (Comment) -> Unit,
    private val onDelete: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val author: TextView = view.findViewById(R.id.commentAuthor)
        val date: TextView = view.findViewById(R.id.commentDate)
        val text: TextView = view.findViewById(R.id.commentText)
        val edit: TextView = view.findViewById(R.id.commentEdit)
        val delete: TextView = view.findViewById(R.id.commentDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.author.text = comment.authorName
        holder.date.text = comment.createdAt.toDateTimeString() +
                if (comment.updatedAt > 0) " · editado" else ""
        holder.text.text = comment.text

        // Só o autor vê as ações de editar e excluir
        val isMine = comment.authorId == myUid
        holder.edit.visibility = if (isMine) View.VISIBLE else View.GONE
        holder.delete.visibility = if (isMine) View.VISIBLE else View.GONE
        holder.edit.setOnClickListener { onEdit(comment) }
        holder.delete.setOnClickListener { onDelete(comment) }
    }

    override fun getItemCount() = comments.size
}