package com.waytolearn.alertadolar

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Respeta status bar / navigation bar (evita superposiciones).
        val root: View = findViewById(R.id.rootNotifications)
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

        listView = findViewById(R.id.listInAppNotifications)
        val btnClearAll: Button = findViewById(R.id.btnClearAll)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

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
        val rows = InAppNotificationStore.getAll(this)
            .map { InAppNotificationStore.formatForList(it) }
        adapter.clear()
        adapter.addAll(rows)
        adapter.notifyDataSetChanged()
    }
}
