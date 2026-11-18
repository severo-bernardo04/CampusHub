package com.example.campushub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Guarda a imagem do evento como JPEG reduzido em Base64 dentro do próprio documento. */
object ImageCodec {
    private const val MAX_SIDE = 800
    private const val MAX_BASE64_LENGTH = 700_000

    fun encode(context: Context, uri: Uri): String? {
        return try {
            val bitmap = decodeUri(context, uri) ?: return null
            var quality = 75
            var encoded: String
            do {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 15
            } while (encoded.length > MAX_BASE64_LENGTH && quality > 10)
            encoded
        } catch (e: Exception) {
            null
        }
    }

    fun decode(base64: String, maxSide: Int): Bitmap? {
        if (base64.isEmpty()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / maxSide)
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeUri(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt(),
                        (info.size.height * scale).toInt()
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / MAX_SIDE)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}