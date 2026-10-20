package com.example.pikaroo.common.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Segundos desde 1970 -> "2026-10-20T15:05:00Z". java.time no existe en Android 24-25 sin desugaring. */
fun formatIsoUtc(epochSeconds: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(epochSeconds * 1000))
