package com.example.campushub

import com.example.campushub.model.Event
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toDateTimeString(): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(this))

/** Evento encerrado = a data/hora dele já passou. */
fun Event.isFinished(): Boolean = dateMillis < System.currentTimeMillis()

fun Event.formattedDate(): String = dateMillis.toDateTimeString()

/** Minúsculas e sem acento, para a busca por título. */
fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()