package com.example.pikaroo.ui.orders.view

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** "2026-10-20T14:10:00Z" -> "dd/MM HH:mm" en la hora del teléfono; null si no se puede leer. */
fun formatTime(iso: String?): String? {
    if (iso == null) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: return null
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        null
    }
}
