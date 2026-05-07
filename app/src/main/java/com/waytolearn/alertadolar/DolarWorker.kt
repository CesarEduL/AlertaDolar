package com.waytolearn.alertadolar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class DolarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)

        val monedaOrigen = prefs.getString(AppPreferences.KEY_CURRENCY, "USD") ?: "USD"
        val umbralAlerta = prefs.getFloat(AppPreferences.KEY_THRESHOLD, 3.40f).toDouble()

        val precio = runCatching {
            ExchangeRateRepository.fetchLatestPenPerUnit(monedaOrigen)
        }.getOrNull()

        if (precio == null) {
            Log.w(TAG, ctx.getString(R.string.log_price_unavailable))
            return Result.success()
        }

        if (precio < umbralAlerta) {
            Log.d(TAG, ctx.getString(R.string.log_alert_fired, precio, umbralAlerta))
            mostrarNotificacion(ctx, precio, monedaOrigen)
        } else {
            Log.d(TAG, ctx.getString(R.string.log_no_alert, precio))
        }

        return Result.success()
    }

    private fun mostrarNotificacion(ctx: Context, precio: Double, moneda: String) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = ctx.getString(R.string.notification_channel_id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                ctx.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.notification_channel_description)
            }
            manager.createNotificationChannel(channel)
        }

        val precioTexto = ctx.getString(R.string.price_format, precio)
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.notification_title, moneda))
            .setContentText(ctx.getString(R.string.notification_body, precioTexto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DolarApp"
        private const val NOTIFICATION_ID = 1
    }
}
