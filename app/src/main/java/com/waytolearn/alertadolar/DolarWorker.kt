package com.waytolearn.alertadolar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlin.math.abs

class DolarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)

        val moneda = prefs.getString(AppPreferences.KEY_CURRENCY, "USD") ?: "USD"
        val umbral = prefs.getFloat(AppPreferences.KEY_THRESHOLD, 3.40f).toDouble()

        val precio = runCatching {
            ExchangeRateRepository.fetchLatestPenPerUnit(moneda)
        }.getOrNull()

        if (precio == null) {
            val msg = ctx.getString(R.string.log_price_unavailable)
            Log.w(TAG, msg)
            InAppNotificationStore.add(ctx, msg)
            DailyNotificationScheduler.scheduleNext(ctx)
            return Result.success()
        }

        val keyLast = AppPreferences.KEY_LAST_NOTIFIED_PRICE_PREFIX + moneda
        val previo = prefs.getString(keyLast, null)?.toDoubleOrNull()
        val cambio = previo == null || abs(previo - precio) > EPSILON

        val precioTexto = ctx.getString(R.string.price_format, precio)
        val umbralTexto = ctx.getString(R.string.price_format, umbral)
        val titulo = if (cambio) {
            ctx.getString(R.string.notification_title_changed, moneda)
        } else {
            ctx.getString(R.string.notification_title_unchanged, moneda)
        }
        val cuerpo = if (precio < umbral) {
            ctx.getString(R.string.notification_body_below_threshold, moneda, precioTexto, umbralTexto)
        } else {
            ctx.getString(R.string.notification_body_daily_status, moneda, precioTexto, umbralTexto)
        }

        mostrarNotificacion(ctx, titulo, cuerpo)
        InAppNotificationStore.add(ctx, "$titulo - $cuerpo")
        prefs.edit().putString(keyLast, precio.toString()).apply()

        Log.d(TAG, "Diaria $moneda: $precioTexto (umbral $umbralTexto), cambio=$cambio")
        DailyNotificationScheduler.scheduleNext(ctx)
        return Result.success()
    }

    private fun mostrarNotificacion(ctx: Context, title: String, body: String) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = ctx.getString(R.string.notification_channel_id)
        val soundUri = Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/${R.raw.alerta_dolar}"
        )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                ctx.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.notification_channel_description)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(ctx, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
                }
            }
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DolarApp"
        private const val NOTIFICATION_ID = 1
        private const val EPSILON = 0.0001
    }
}
