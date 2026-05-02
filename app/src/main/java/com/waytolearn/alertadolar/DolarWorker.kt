package com.waytolearn.alertadolar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.URL

class DolarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val CHANNEL_ID = "DOLAR_ALERTS"

    override fun doWork(): Result {
        // 1. Leemos las preferencias guardadas en MainActivity
        val prefs = applicationContext.getSharedPreferences("ConfigDolar", Context.MODE_PRIVATE)

        // Moneda seleccionada (USD por defecto)
        val monedaOrigen = prefs.getString("moneda_origen", "USD") ?: "USD"

        // Umbral de alerta (3.40 por defecto) - Leemos como Float y convertimos a Double
        val umbralAlerta = prefs.getFloat("umbral_alerta", 3.40f).toDouble()

        // 2. Intentamos obtener el precio actual
        val precio = obtenerPrecioDolar(monedaOrigen)

        // 3. Lógica de comparación dinámica
        if (precio != null && precio < umbralAlerta) {
            Log.d("DolarApp", "¡Alerta activada! Precio actual: $precio < Umbral: $umbralAlerta")
            mostrarNotificacion(precio, monedaOrigen)
        } else {
            Log.d("DolarApp", "Precio: $precio. No se requiere notificación.")
        }

        return Result.success()
    }

    private fun obtenerPrecioDolar(base: String): Double? {
        // Reemplaza con tu Key real si deseas usarla, de lo contrario saltará a Frankfurter
        val myApiKey = "TU_LLAVE_DE_EXCHANGERATE_AQUÍ"

        // INTENTO 1: ExchangeRate-API
        try {
            val url = "https://v6.exchangerate-api.com/v6/$myApiKey/latest/$base"
            val response = URL(url).readText()
            val json = JSONObject(response)

            if (json.getString("result") == "success") {
                return json.getJSONObject("conversion_rates").getDouble("PEN")
            }
        } catch (e: Exception) {
            Log.e("DolarApp", "ExchangeRate-API falló, usando Frankfurter...")
        }

        // INTENTO 2: Frankfurter (Respaldo)
        try {
            val url = "https://api.frankfurter.app/latest?from=$base&to=PEN"
            val response = URL(url).readText()
            val json = JSONObject(response)
            return json.getJSONObject("rates").getDouble("PEN")
        } catch (e: Exception) {
            Log.e("DolarApp", "Error crítico: Ambas APIs fallaron.")
            return null
        }
    }

    private fun mostrarNotificacion(precio: Double, moneda: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal si es Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Divisas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifica cuando el precio baja del límite configurado"
            }
            manager.createNotificationChannel(channel)
        }

        // Construir la notificación con el precio formateado a 2 decimales
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Alerta de precio: $moneda!")
            .setContentText("El precio actual es S/ %.2f. ¡Está por debajo de tu límite!".format(precio))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}