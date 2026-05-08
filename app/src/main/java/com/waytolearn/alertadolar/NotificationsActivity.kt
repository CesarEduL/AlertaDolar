package com.waytolearn.alertadolar

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var filterSpinner: Spinner
    private lateinit var adapter: InternalNotificationsAdapter

    private val filterTypes: List<InternalNotificationType?> = listOf(
        null,
        InternalNotificationType.PRICE_CHANGE,
        InternalNotificationType.PRICE_STABLE_ABOVE,
        InternalNotificationType.PRICE_BELOW_THRESHOLD,
        InternalNotificationType.ERROR
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val root = findViewById<View>(R.id.rootNotifications)
        val basePadding = root.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                basePadding + bars.left,
                basePadding + bars.top,
                basePadding + bars.right,
                basePadding + bars.bottom
            )
            insets
        }

        recyclerView = findViewById(R.id.recyclerInAppNotifications)
        filterSpinner = findViewById(R.id.spinnerInboxFilter)
        val btnClearAll: ImageButton = findViewById(R.id.btnClearAll)

        val filterLabels = listOf(
            getString(R.string.inbox_filter_all),
            getString(R.string.inbox_filter_change),
            getString(R.string.inbox_filter_stable_above),
            getString(R.string.inbox_filter_below),
            getString(R.string.inbox_filter_error)
        )
        filterSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, filterLabels).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                refresh()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        adapter = InternalNotificationsAdapter { item ->
            if (!item.isPriceChange) return@InternalNotificationsAdapter
            CurrencyNotificationHelper.post(
                this,
                InAppNotificationStore.summaryFromStoredMessage(item.message),
                item.message.trim()
            )
            Toast.makeText(
                this,
                R.string.inbox_force_sent_hint,
                Toast.LENGTH_SHORT
            ).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        btnClearAll.setOnClickListener {
            InAppNotificationStore.clear(this)
            refresh()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val all = InAppNotificationStore.getAll(this)
        val pos = filterSpinner.selectedItemPosition.coerceIn(0, filterTypes.lastIndex)
        val type = filterTypes[pos]
        val filtered = if (type == null) all else all.filter { it.type == type }
        adapter.submit(filtered)
    }
}
