package com.waytolearn.alertadolar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InAppNotification(
    val timestampMs: Long,
    val message: String,
    val isPriceChange: Boolean
)

object InAppNotificationStore {
    private const val MAX_ITEMS = 300

    /**
     * Nuevas entradas guardan explícito [priceChange].
     */
    fun add(context: Context, message: String, priceChange: Boolean = false) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]"))
        val updated = JSONArray()

        val entry = JSONObject()
            .put("timestamp", System.currentTimeMillis())
            .put("message", message)
            .put("priceChange", priceChange)
        updated.put(entry)

        for (i in 0 until current.length()) {
            if (updated.length() >= MAX_ITEMS) break
            updated.put(current.getJSONObject(i))
        }

        prefs.edit()
            .putString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, updated.toString())
            .apply()
    }

    fun getAll(context: Context): List<InAppNotification> {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]")
        val json = JSONArray(raw)
        val result = mutableListOf<InAppNotification>()
        for (i in 0 until json.length()) {
            val row = json.getJSONObject(i)
            val message = row.optString("message", "")
            val isPc = row.optNullableBoolean("priceChange")
                ?: messageLooksLikePriceChange(message)
            result.add(
                InAppNotification(
                    timestampMs = row.optLong("timestamp", 0L),
                    message = message,
                    isPriceChange = isPc
                )
            )
        }
        return result
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        return when {
            !has(key) -> null
            JSONObject.NULL.equals(opt(key)) -> null
            else -> getBoolean(key)
        }
    }

    /** Compatibilidad con entradas viejas antes de tener el flag guardado en JSON. */
    fun messageLooksLikePriceChange(message: String): Boolean {
        if (message.contains(" cambió de ")) return true
        if (message.contains("(cambió)")) return true
        return false
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]").apply()
    }

    fun formatForList(item: InAppNotification): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val time = formatter.format(Date(item.timestampMs))
        return "[$time] ${item.message}"
    }

    /** Primera línea para resumen compacto del sistema Android. */
    fun summaryFromStoredMessage(fullMessage: String): String =
        fullMessage.trim().substringBefore('\n').ifBlank { fullMessage.trim() }
}
