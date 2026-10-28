package com.example.campushub

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.AppUser
import com.example.campushub.model.MediaPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.UUID

class MediaActivity : AppCompatActivity() {

    companion object {
        private const val MAX_VIDEO_SECONDS = 15
        private const val MAX_AUDIO_MILLIS = 30_000
        private const val MAX_VIDEO_BYTES = 50L * 1024 * 1024
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var mediaDb: MediaDbHelper
    private lateinit var eventId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    private var myName = ""
    private var adapter: MediaAdapter? = null

    private var pendingFile: File? = null      // arquivo aguardando o retorno da câmera
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordDialog: AlertDialog? = null
    private var player: MediaPlayer? = null
    private var playingId = -1L

    // ---------- Contratos de resultado (devem ser registrados antes do STARTED) ----------

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = pendingFile
            pendingFile = null
            if (success && file != null) savePost(MediaPost.TYPE_PHOTO, file) else file?.delete()
        }

    private val captureVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val file = pendingFile
            pendingFile = null
            if (result.resultCode == RESULT_OK && file != null && file.length() > 0) {
                savePost(MediaPost.TYPE_VIDEO, file)
            } else {
                file?.delete()
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) importFromUri(uri, MediaPost.TYPE_PHOTO, "jpg")
        }

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) importFromUri(uri, MediaPost.TYPE_VIDEO, "mp4")
        }

    private val audioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showRecordDialog()
            else toast("Sem permissão do microfone não é possível gravar áudio")
        }

    // ---------- Ciclo de vida ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        mediaDb = MediaDbHelper(this)

        eventId = intent.getStringExtra("eventId") ?: run {
            finish()
            return
        }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.mediaRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.emptyText)

        findViewById<View>(R.id.takePhotoButton).setOnClickListener { takePhoto() }
        findViewById<View>(R.id.pickPhotoButton).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<View>(R.id.recordAudioButton).setOnClickListener { startAudioRecording() }
        findViewById<View>(R.id.recordVideoButton).setOnClickListener { recordVideo() }
        findViewById<View>(R.id.pickVideoButton).setOnClickListener {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        loadMyName()
        loadPosts()
    }

    override fun onStop() {
        super.onStop()
        stopPlayback()
        recordDialog?.dismiss()
        finishRecording(save = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        finishRecording(save = false)
        mediaDb.close()
    }

    // ---------- Dados ----------

    private fun loadMyName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            myName = doc.toObject(AppUser::class.java)?.name.orEmpty()
        }
    }

    private fun loadPosts() {
        val uid = auth.currentUser?.uid ?: return
        val posts = mediaDb.listByEvent(eventId)
        emptyText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE

        adapter = MediaAdapter(
            posts = posts,
            myUid = uid,
            onPlayAudio = { toggleAudio(it) },
            onOpenPhoto = { showPhoto(it) },
            onPlayVideo = { showVideo(it) },
            onDelete = { confirmDelete(it) }
        ).also { it.playingId = playingId }
        recyclerView.adapter = adapter
    }

    private fun savePost(type: String, file: File) {
        val user = auth.currentUser ?: return
        mediaDb.insert(
            MediaPost(
                id = 0,
                eventId = eventId,
                authorId = user.uid,
                authorName = myName.ifEmpty { user.email ?: "Aluno" },
                type = type,
                path = file.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )
        loadPosts()
    }

    private fun confirmDelete(post: MediaPost) {
        if (post.authorId != auth.currentUser?.uid) return

        AlertDialog.Builder(this)
            .setTitle("Excluir publicação")
            .setMessage("Tem certeza que deseja excluir esta publicação?")
            .setPositiveButton("Excluir") { _, _ ->
                if (post.id == playingId) stopPlayback()
                File(post.path).delete()
                mediaDb.delete(post.id)
                loadPosts()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Arquivos ----------

    private fun newMediaFile(extension: String): File {
        val dir = File(filesDir, "media").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.$extension")
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun importFromUri(uri: Uri, type: String, extension: String) {
        Thread {
            try {
                val file = newMediaFile(extension)
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                if (type == MediaPost.TYPE_VIDEO && file.length() > MAX_VIDEO_BYTES) {
                    file.delete()
                    runOnUiThread { toast("Vídeo muito grande (máximo de 50 MB)") }
                } else {
                    runOnUiThread { savePost(type, file) }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Não foi possível importar o arquivo") }
            }
        }.start()
    }

    // ---------- Foto e vídeo pela câmera ----------

    private fun takePhoto() {
        val file = newMediaFile("jpg")
        pendingFile = file
        try {
            takePicture.launch(uriFor(file))
        } catch (e: ActivityNotFoundException) {
            pendingFile = null
            toast("Nenhum aplicativo de câmera encontrado")
        }
    }

    private fun recordVideo() {
        val file = newMediaFile("mp4")
        pendingFile = file
        val uri = uriFor(file)

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_VIDEO_SECONDS)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            captureVideo.launch(intent)
        } catch (e: ActivityNotFoundException) {
            pendingFile = null
            toast("Nenhum aplicativo de câmera encontrado")
        }
    }

    // ---------- Gravação de áudio ----------

    private fun startAudioRecording() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) showRecordDialog() else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

    private fun showRecordDialog() {
        stopPlayback()
        val file = newMediaFile("m4a")
        val rec = createRecorder()

        try {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setMaxDuration(MAX_AUDIO_MILLIS)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            rec.release()
            file.delete()
            toast("Não foi possível iniciar a gravação")
            return
        }

        recorder = rec
        recordingFile = file

        val dialog = AlertDialog.Builder(this)
            .setTitle("Gravando áudio")
            .setMessage("Fale agora. A gravação para sozinha em 30 segundos.")
            .setCancelable(false)
            .setPositiveButton("Publicar") { _, _ -> finishRecording(save = true) }
            .setNegativeButton("Cancelar") { _, _ -> finishRecording(save = false) }
            .create()

        rec.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                dialog.dismiss()
                finishRecording(save = true)
            }
        }

        recordDialog = dialog
        dialog.show()
    }

    private fun finishRecording(save: Boolean) {
        val rec = recorder ?: return
        val file = recordingFile
        recorder = null
        recordingFile = null

        try {
            rec.stop()
        } catch (e: RuntimeException) {
            // Gravação curta demais: o arquivo fica vazio e é descartado abaixo
        }
        rec.release()

        if (file == null) return
        if (save && file.length() > 0) savePost(MediaPost.TYPE_AUDIO, file) else file.delete()
    }

    // ---------- Reprodução ----------

    private fun toggleAudio(post: MediaPost) {
        if (playingId == post.id) {
            stopPlayback()
            return
        }
        stopPlayback()

        val mp = MediaPlayer()
        try {
            mp.setDataSource(post.path)
            mp.prepare()
            mp.setOnCompletionListener { stopPlayback() }
            mp.start()
            player = mp
            playingId = post.id
            adapter?.let {
                it.playingId = playingId
                it.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            mp.release()
            toast("Não foi possível reproduzir o áudio")
        }
    }

    private fun stopPlayback() {
        player?.let {
            try {
                it.stop()
            } catch (e: IllegalStateException) {
                // já estava parado
            }
            it.release()
        }
        player = null
        playingId = -1L
        adapter?.let {
            it.playingId = -1L
            it.notifyDataSetChanged()
        }
    }

    private fun showPhoto(post: MediaPost) {
        val imageView = ImageView(this).apply {
            adjustViewBounds = true
            setImageBitmap(ImageUtils.decode(post.path, 1600))
        }
        AlertDialog.Builder(this)
            .setView(imageView)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun showVideo(post: MediaPost) {
        stopPlayback()

        val height = (320 * resources.displayMetrics.density).toInt()
        val videoView = VideoView(this)
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.setVideoPath(post.path)
        videoView.setOnPreparedListener { it.start() }

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, height
            )
            addView(
                videoView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
        }

        AlertDialog.Builder(this)
            .setView(container)
            .setPositiveButton("Fechar", null)
            .create()
            .also { it.setOnDismissListener { videoView.stopPlayback() } }
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}