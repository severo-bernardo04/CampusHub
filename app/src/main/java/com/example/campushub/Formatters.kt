package com.example.campushub

import com.example.campushub.domain.EventStatus
import com.example.campushub.model.Event
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toDateTimeString(): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(this))

/** Encerrado = o organizador encerrou OU a data/hora já passou. */
fun Event.isFinished(now: Long = System.currentTimeMillis()): Boolean =
    status == EventStatus.CLOSED || dateMillis < now

fun Event.formattedDate(): String = dateMillis.toDateTimeString()

/** Minúsculas e sem acento, para a busca por título. */
fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()