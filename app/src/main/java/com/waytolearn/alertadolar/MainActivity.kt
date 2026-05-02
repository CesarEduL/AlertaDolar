package com.waytolearn.alertadolar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var txtPrecioActual: TextView
    private lateinit var txtMetricas: TextView
    private lateinit var editPrecioAlerta: EditText
    private val monedas = arrayOf("USD", "EUR", "GBP", "BRL", "CLP")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        txtPrecioActual = findViewById(R.id.txtPrecioActual)
        txtMetricas = findViewById(R.id.txtMetricas)
        editPrecioAlerta = findViewById(R.id.editPrecioAlerta)
        val spinner: Spinner = findViewById(R.id.spinnerMonedas)
        val btnActualizar: Button = findViewById(R.id.btnActualizar)

        val prefs = getSharedPreferences("ConfigDolar", Context.MODE_PRIVATE)

        // 1. Configurar Selector de Monedas (Spinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monedas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Recuperar moneda guardada anteriormente
        val monedaGuardada = prefs.getString("moneda_origen", "USD")
        val spinnerPosition = adapter.getPosition(monedaGuardada)
        spinner.setSelection(spinnerPosition)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccion = monedas[position]
                prefs.edit().putString("moneda_origen", seleccion).apply()
                obtenerPrecioYMétricas(seleccion)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Configurar el campo de Precio Alerta (EditText)
        val umbralGuardado = prefs.getFloat("umbral_alerta", 3.40f)
        editPrecioAlerta.setText(umbralGuardado.toString())

        editPrecioAlerta.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toFloatOrNull()
                if (input != null) {
                    prefs.edit().putFloat("umbral_alerta", input).apply()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 3. Botón Actualizar
        btnActualizar.setOnClickListener {
            obtenerPrecioYMétricas(spinner.selectedItem.toString())
        }

        solicitarPermisoNotificaciones()
        iniciarRastreador()
    }

    @SuppressLint("SetTextI18n")
    private fun obtenerPrecioYMétricas(base: String) {
        txtPrecioActual.text = "..."
        txtMetricas.text = "Calculando métricas..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Obtener precio actual (Frankfurter)
                val respActual = URL("https://api.frankfurter.app/latest?from=$base&to=PEN").readText()
                val precioActual = JSONObject(respActual).getJSONObject("rates").getDouble("PEN")

                // 2. Obtener historial (últimos 7 días)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                val fechaHoy = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val fechaPasada = sdf.format(cal.time)

                val respHist = URL("https://api.frankfurter.app/$fechaPasada..$fechaHoy?from=$base&to=PEN").readText()
                val rates = JSONObject(respHist).getJSONObject("rates")

                val listaPrecios = mutableListOf<Double>()
                val keys = rates.keys()
                while (keys.hasNext()) {
                    val fecha = keys.next()
                    listaPrecios.add(rates.getJSONObject(fecha).getDouble("PEN"))
                }

                val maximo = listaPrecios.maxOrNull() ?: precioActual
                val minimo = listaPrecios.minOrNull() ?: precioActual

                withContext(Dispatchers.Main) {
                    txtPrecioActual.text = "S/ %.2f".format(precioActual)
                    txtMetricas.text = "Mín (7d): S/ %.2f  |  Máx (7d): S/ %.2f".format(minimo, maximo)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtPrecioActual.text = "Error"
                    txtMetricas.text = "Revisa tu conexión a internet"
                }
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun iniciarRastreador() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DolarWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MiRastreadorDolar",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}