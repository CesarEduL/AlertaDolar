package com.waytolearn.alertadolar

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fuente única de tipos de cambio: ExchangeRate-API (si hay clave en [BuildConfig]) y Frankfurter como respaldo.
 * El histórico 7d usa solo Frankfurter; si falla, la UI puede mostrar solo el precio actual.
 *
 * Usa [HttpURLConnection] para evitar conflictos de versiones del OkHttp que trae Retrofit como transitivo.
 */
object ExchangeRateRepository {

    private const val USER_AGENT = "AlertaDolar/1.0 (Android)"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000

    @Throws(IOException::class)
    fun fetchLatestPenPerUnit(base: String): Double {
        // Generado en compile time (namespace = com.waytolearn.alertadolar). Sync Gradle / Rebuild si el IDE no lo ve.
        val key = BuildConfig.EXCHANGE_RATE_API_KEY
        if (key.isNotBlank()) {
            runCatching { fetchFromExchangeRateApi(key, base) }
                .onSuccess { return it }
        }
        return fetchFromFrankfurterLatest(base)
    }

    /**
     * @return par (mín, máx) en el periodo o null si la petición falla o no hay datos.
     */
    fun fetchSevenDayMinMaxPen(base: String): Pair<Double, Double>? =
        runCatching { fetchSevenDayMinMaxPenInternal(base) }.getOrNull()

    private fun fetchSevenDayMinMaxPenInternal(base: String): Pair<Double, Double>? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val end = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val start = sdf.format(cal.time)
        val url = "https://api.frankfurter.app/$start..$end?from=$base&to=PEN"
        val json = JSONObject(httpGet(url))
        val rates = json.getJSONObject("rates")
        val prices = mutableListOf<Double>()
        val keys = rates.keys()
        while (keys.hasNext()) {
            val day = keys.next()
            prices.add(rates.getJSONObject(day).getDouble("PEN"))
        }
        if (prices.isEmpty()) return null
        val min = prices.minOrNull() ?: return null
        val max = prices.maxOrNull() ?: return null
        return min to max
    }

    private fun fetchFromExchangeRateApi(
        apiKey: String,
        base: String
    ): Double {
        val url = "https://v6.exchangerate-api.com/v6/$apiKey/latest/$base"
        val json = JSONObject(httpGet(url))
        if (json.getString("result") != "success") {
            throw IOException(json.optString("error-type", "exchange API error"))
        }
        return json.getJSONObject("conversion_rates").getDouble("PEN")
    }

    private fun fetchFromFrankfurterLatest(base: String): Double {
        val url = "https://api.frankfurter.app/latest?from=$base&to=PEN"
        val json = JSONObject(httpGet(url))
        return json.getJSONObject("rates").getDouble("PEN")
    }

    private fun httpGet(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        return try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code: ${conn.responseMessage}\n${text.take(200)}")
            }
            text
        } finally {
            conn.disconnect()
        }
    }
}
