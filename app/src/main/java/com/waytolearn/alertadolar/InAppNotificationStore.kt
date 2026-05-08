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
    val type: InternalNotificationType
) {
    val isPriceChange: Boolean get() = type == InternalNotificationType.PRICE_CHANGE
}

object InAppNotificationStore {
    private const val MAX_ITEMS = 300

    fun add(
        context: Context,
        message: String,
        type: InternalNotificationType
    ) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]"))
        val updated = JSONArray()

        val entry = JSONObject()
            .put("timestamp", System.currentTimeMillis())
            .put("message", message)
            .put("type", type.storageKey)
            .put(
                "priceChange",
                type == InternalNotificationType.PRICE_CHANGE
            )
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
            val typeKey = when {
                !row.has("type") || JSONObject.NULL.equals(row.opt("type")) -> null
                else -> row.optString("type").takeIf { it.isNotEmpty() }
            }
            val isPc = row.optNullableBoolean("priceChange")
            val type = InternalNotificationType.fromStorageOrInfer(typeKey, message, isPc)
            result.add(
                InAppNotification(
                    timestampMs = row.optLong("timestamp", 0L),
                    message = message,
                    type = type
                )
            )
        }
        return result
    }

    /**
     * Elimina el registro **más antiguo** (último índice del JSON: orden reciente primero).
     * No hace nada si hay 0 o 1 filas (no borra la única entrada).
     */
    fun removeOldest(context: Context) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]"))
        if (arr.length() <= 1) return
        val newArr = JSONArray()
        for (i in 0 until arr.length() - 1) {
            newArr.put(arr.getJSONObject(i))
        }
        prefs.edit().putString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, newArr.toString())
            .apply()
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        return when {
            !has(key) -> null
            JSONObject.NULL.equals(opt(key)) -> null
            else -> getBoolean(key)
        }
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

    fun summaryFromStoredMessage(fullMessage: String): String =
        fullMessage.trim().substringBefore('\n').ifBlank { fullMessage.trim() }
}
