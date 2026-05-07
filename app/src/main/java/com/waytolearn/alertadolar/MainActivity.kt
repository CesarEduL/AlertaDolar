package com.waytolearn.alertadolar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var txtPrecioActual: TextView
    private lateinit var txtMetricas: TextView
    private lateinit var editPrecioAlerta: EditText
    private lateinit var monedas: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        monedas = resources.getStringArray(R.array.currency_codes)

        txtPrecioActual = findViewById(R.id.txtPrecioActual)
        txtMetricas = findViewById(R.id.txtMetricas)
        editPrecioAlerta = findViewById(R.id.editPrecioAlerta)
        val spinner: Spinner = findViewById(R.id.spinnerMonedas)
        val btnActualizar: Button = findViewById(R.id.btnActualizar)
        val btnInbox: ImageButton = findViewById(R.id.btnInbox)

        val prefs = getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monedas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val monedaGuardada = prefs.getString(AppPreferences.KEY_CURRENCY, monedas[0]) ?: monedas[0]
        val spinnerPosition = adapter.getPosition(monedaGuardada).coerceAtLeast(0)
        spinner.setSelection(spinnerPosition)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccion = monedas[position]
                prefs.edit().putString(AppPreferences.KEY_CURRENCY, seleccion).apply()
                obtenerPrecioYMétricas(seleccion)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val umbralGuardado = prefs.getFloat(AppPreferences.KEY_THRESHOLD, DEFAULT_THRESHOLD)
        editPrecioAlerta.setText(umbralGuardado.toString())

        editPrecioAlerta.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toFloatOrNull()
                if (input != null) {
                    prefs.edit().putFloat(AppPreferences.KEY_THRESHOLD, input).apply()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnActualizar.setOnClickListener {
            obtenerPrecioYMétricas(spinner.selectedItem.toString())
        }

        btnInbox.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        solicitarPermisoNotificaciones()
        iniciarRastreador()
    }

    private fun obtenerPrecioYMétricas(base: String) {
        txtPrecioActual.text = getString(R.string.loading_price)
        txtMetricas.text = getString(R.string.metrics_loading)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val precioActual = ExchangeRateRepository.fetchLatestPenPerUnit(base)
                val minMax = ExchangeRateRepository.fetchSevenDayMinMaxPen(base)

                withContext(Dispatchers.Main) {
                    txtPrecioActual.text = getString(R.string.price_format, precioActual)
                    txtMetricas.text = if (minMax != null) {
                        getString(
                            R.string.metrics_ready,
                            getString(R.string.price_format, minMax.first),
                            getString(R.string.price_format, minMax.second)
                        )
                    } else {
                        getString(R.string.metrics_partial)
                    }
                }
            } catch (e: Exception) {
                InAppNotificationStore.add(
                    this@MainActivity,
                    getString(R.string.inbox_log_ui_error, e.message ?: getString(R.string.error_network))
                )
                withContext(Dispatchers.Main) {
                    txtPrecioActual.text = getString(R.string.error_title)
                    txtMetricas.text = getString(
                        R.string.error_network_with_reason,
                        e.message ?: getString(R.string.error_network)
                    )
                    Toast.makeText(
                        this@MainActivity,
                        R.string.error_network,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS
                )
            }
        }
    }

    private fun iniciarRastreador() {
        // Nuevo comportamiento: se programa por hora local fija (10:00 y 20:00).
        DailyNotificationScheduler.scheduleNext(this)
    }

    companion object {
        private const val DEFAULT_THRESHOLD = 3.40f
        private const val REQUEST_NOTIFICATIONS = 101
    }
}
