package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.MediaPost
import java.io.File

class MediaAdapter(
    private val posts: List<MediaPost>,
    private val myUid: String,
    private val onPlayAudio: (MediaPost) -> Unit,
    private val onOpenPhoto: (MediaPost) -> Unit,
    private val onPlayVideo: (MediaPost) -> Unit,
    private val onDelete: (MediaPost) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    /** ID da publicação de áudio que está tocando (-1 = nenhuma). */
    var playingId: Long = -1L

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val author: TextView = view.findViewById(R.id.mediaAuthor)
        val date: TextView = view.findViewById(R.id.mediaDate)
        val delete: TextView = view.findViewById(R.id.mediaDelete)
        val photo: ImageView = view.findViewById(R.id.mediaPhoto)
        val audioButton: Button = view.findViewById(R.id.audioButton)
        val videoFrame: FrameLayout = view.findViewById(R.id.videoFrame)
        val videoThumb: ImageView = view.findViewById(R.id.videoThumb)
        val missing: TextView = view.findViewById(R.id.mediaMissing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val post = posts[position]
        val exists = File(post.path).exists()

        holder.author.text = post.authorName
        holder.date.text = post.createdAt.toDateTimeString()

        // Só o autor pode excluir a própria publicação
        holder.delete.visibility = if (post.authorId == myUid) View.VISIBLE else View.GONE
        holder.delete.setOnClickListener { onDelete(post) }

        holder.photo.visibility = View.GONE
        holder.audioButton.visibility = View.GONE
        holder.videoFrame.visibility = View.GONE
        holder.missing.visibility = if (exists) View.GONE else View.VISIBLE
        if (!exists) return

        when (post.type) {
            MediaPost.TYPE_PHOTO -> {
                holder.photo.visibility = View.VISIBLE
                holder.photo.setImageBitmap(ImageUtils.decode(post.path, 900))
                holder.photo.setOnClickListener { onOpenPhoto(post) }
            }
            MediaPost.TYPE_AUDIO -> {
                holder.audioButton.visibility = View.VISIBLE
                holder.audioButton.text =
                    if (post.id == playingId) "■  Parar" else "▶  Ouvir áudio"
                holder.audioButton.setOnClickListener { onPlayAudio(post) }
            }
            MediaPost.TYPE_VIDEO -> {
                holder.videoFrame.visibility = View.VISIBLE
                holder.videoThumb.setImageBitmap(ImageUtils.videoThumbnail(post.path))
                holder.videoFrame.setOnClickListener { onPlayVideo(post) }
            }
        }
    }

    override fun getItemCount() = posts.size
}