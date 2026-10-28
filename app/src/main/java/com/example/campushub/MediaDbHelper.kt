package com.example.campushub

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.campushub.model.MediaPost

/** Guarda localmente os metadados das publicações de foto, áudio e vídeo. */
class MediaDbHelper(context: Context) :
    SQLiteOpenHelper(context, "campushub_media.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE media (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "eventId TEXT NOT NULL, " +
                    "authorId TEXT NOT NULL, " +
                    "authorName TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "path TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS media")
        onCreate(db)
    }

    fun insert(post: MediaPost): Long {
        val values = ContentValues().apply {
            put("eventId", post.eventId)
            put("authorId", post.authorId)
            put("authorName", post.authorName)
            put("type", post.type)
            put("path", post.path)
            put("createdAt", post.createdAt)
        }
        return writableDatabase.insert("media", null, values)
    }

    fun listByEvent(eventId: String): List<MediaPost> {
        val result = mutableListOf<MediaPost>()
        readableDatabase.query(
            "media", null, "eventId = ?", arrayOf(eventId), null, null, "createdAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    MediaPost(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        eventId = cursor.getString(cursor.getColumnIndexOrThrow("eventId")),
                        authorId = cursor.getString(cursor.getColumnIndexOrThrow("authorId")),
                        authorName = cursor.getString(cursor.getColumnIndexOrThrow("authorName")),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        path = cursor.getString(cursor.getColumnIndexOrThrow("path")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                    )
                )
            }
        }
        return result
    }

    fun delete(id: Long) {
        writableDatabase.delete("media", "id = ?", arrayOf(id.toString()))
    }
}