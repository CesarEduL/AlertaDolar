package com.waytolearn.alertadolar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InAppNotification(
    val timestampMs: Long,
    val message: String
)

object InAppNotificationStore {
    private const val MAX_ITEMS = 300

    fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(AppPreferences.KEY_INTERNAL_NOTIFICATIONS_JSON, "[]"))
        val updated = JSONArray()

        val entry = JSONObject()
            .put("timestamp", System.currentTimeMillis())
            .put("message", message)
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
            result.add(
                InAppNotification(
                    timestampMs = row.optLong("timestamp", 0L),
                    message = row.optString("message", "")
                )
            )
        }
        return result
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
}
