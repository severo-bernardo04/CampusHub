package com.example.campushub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.File

object ImageUtils {

    /** Decodifica a imagem reduzida, para não estourar memória com fotos da câmera. */
    fun decode(path: String, maxSize: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(File(path))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = maxOf(info.size.width, info.size.height)
                decoder.setTargetSampleSize(maxOf(1, largest / maxSize))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply {
                inSampleSize = maxOf(1, largest / maxSize)
            }
            BitmapFactory.decodeFile(path, options)
        }
    } catch (e: Exception) {
        null
    }

    fun videoThumbnail(path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}