package com.example.azarbijan_tech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceUsageAdapter(private val items: List<DeviceUsage>) :
    RecyclerView.Adapter<DeviceUsageAdapter.DeviceUsageViewHolder>() {

    class DeviceUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivDeviceIcon: ImageView = itemView.findViewById(R.id.ivDeviceIcon)
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvDeviceUsage: TextView = itemView.findViewById(R.id.tvDeviceUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_usage, parent, false)
        return DeviceUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceUsageViewHolder, position: Int) {
        val item = items[position]
        holder.ivDeviceIcon.setImageResource(item.iconRes)
        holder.tvDeviceName.text = item.name
        holder.tvDeviceUsage.text = item.usageTime
    }

    override fun getItemCount(): Int = items.size
}
