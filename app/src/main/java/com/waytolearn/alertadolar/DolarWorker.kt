package com.waytolearn.alertadolar

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
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
            InAppNotificationStore.add(ctx, msg, InternalNotificationType.ERROR)
            DailyNotificationScheduler.scheduleNext(ctx)
            return Result.success()
        }

        val keyLast = AppPreferences.KEY_LAST_NOTIFIED_PRICE_PREFIX + moneda
        val anterior = prefs.getString(keyLast, null)?.toDoubleOrNull()
        val huboMovimiento = anterior != null && abs(anterior - precio) > EPSILON

        val precioTexto = ctx.getString(R.string.price_format, precio)
        val umbralTexto = ctx.getString(R.string.price_format, umbral)

        val cuerpoSistema = when {
            huboMovimiento && anterior != null -> {
                val anteriorTexto = ctx.getString(R.string.price_format, anterior)
                ctx.getString(R.string.notification_line_changed, moneda, anteriorTexto, precioTexto)
            }

            precio < umbral -> ctx.getString(
                R.string.notification_line_stable_below,
                moneda,
                precioTexto,
                umbralTexto
            )

            else -> ctx.getString(
                R.string.notification_line_stable_above,
                moneda,
                precioTexto,
                umbralTexto
            )
        }

        val cuerpoLargo = buildString {
            append(cuerpoSistema)
            if (huboMovimiento) {
                append("\n")
                append(
                    if (precio < umbral) {
                        ctx.getString(
                            R.string.notification_line_stable_below,
                            moneda,
                            precioTexto,
                            umbralTexto
                        )
                    } else {
                        ctx.getString(
                            R.string.notification_line_stable_above,
                            moneda,
                            precioTexto,
                            umbralTexto
                        )
                    }
                )
            }
        }

        CurrencyNotificationHelper.post(
            ctx,
            cuerpoSistema,
            cuerpoLargo.trim(),
            CurrencyNotificationHelper.NOTIFICATION_ID_DAILY_SCHEDULE
        )
        val internalType = when {
            huboMovimiento -> InternalNotificationType.PRICE_CHANGE
            precio < umbral -> InternalNotificationType.PRICE_BELOW_THRESHOLD
            else -> InternalNotificationType.PRICE_STABLE_ABOVE
        }
        InAppNotificationStore.add(ctx, cuerpoLargo.trim(), internalType)

        val scheduledHour = inputData.getInt(DailyNotificationScheduler.KEY_SCHEDULED_HOUR, -1)
        val cal = Calendar.getInstance()
        val isSundayEveningMaintenance =
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && scheduledHour == 20
        if (isSundayEveningMaintenance) {
            InAppNotificationStore.removeOldest(ctx)
        }

        prefs.edit().putString(keyLast, precio.toString()).apply()

        Log.d(TAG, "Diaria $moneda: $precioTexto (umbral $umbralTexto), movimiento=$huboMovimiento")
        DailyNotificationScheduler.scheduleNext(ctx)
        return Result.success()
    }

    companion object {
        private const val TAG = "DolarApp"
        private const val EPSILON = 0.0001
    }
}
