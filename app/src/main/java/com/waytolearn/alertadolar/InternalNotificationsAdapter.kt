package com.waytolearn.alertadolar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InternalNotificationsAdapter(
    private val onForceSystemNotify: (InAppNotification) -> Unit
) : RecyclerView.Adapter<InternalNotificationsAdapter.VH>() {

    private val items = mutableListOf<InAppNotification>()

    fun submit(list: List<InAppNotification>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_internal_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.txt.text = InAppNotificationStore.formatForList(item)
        holder.btn.visibility = if (item.isPriceChange) View.VISIBLE else View.GONE
        holder.btn.setOnClickListener {
            onForceSystemNotify(item)
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txt: TextView = itemView.findViewById(R.id.txtEntry)
        val btn: ImageButton = itemView.findViewById(R.id.btnForceSystemNotify)
    }
}
