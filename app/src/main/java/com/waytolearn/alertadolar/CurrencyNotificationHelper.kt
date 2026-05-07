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
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicInteger

/** Construye y publica notificaciones de tipo de cambio en el mismo canal que el trabajo diario. */
object CurrencyNotificationHelper {

    /** Id fijo del aviso programado (10:00 / 20:00). */
    const val NOTIFICATION_ID_DAILY_SCHEDULE = 1

    private val nextManualId = AtomicInteger(10_001)

    /**
     * Si [notificationId] es null, genera uno nuevo para no sustituir notificaciones forzadas.
     */
    fun post(
        context: Context,
        summary: String,
        bigText: String,
        notificationId: Int? = null
    ) {
        val ctx = context.applicationContext
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = ctx.getString(R.string.notification_channel_id)
        val title = ctx.getString(R.string.notification_title_bar)

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

        val nid = notificationId ?: nextManualId.incrementAndGet()

        val intent = Intent(ctx, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            nid,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = bigText.trim()
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(summary.trim())
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

        manager.notify(nid, notification)
    }
}
