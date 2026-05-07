package com.waytolearn.alertadolar

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InternalNotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val root = findViewById<android.view.View>(R.id.rootNotifications)
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
        val btnClearAll: ImageButton = findViewById(R.id.btnClearAll)

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
        adapter.submit(InAppNotificationStore.getAll(this))
    }
}
