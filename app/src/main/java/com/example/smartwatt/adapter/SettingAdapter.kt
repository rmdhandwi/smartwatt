package com.example.smartwatt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartwatt.Model.SettingModel
import com.example.smartwatt.R


class SettingAdapter(
    private var list: List<SettingModel>,
    private val onEdit: (SettingModel) -> Unit,
    private val onDelete: (SettingModel) -> Unit
) : RecyclerView.Adapter<SettingAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTanggal: TextView = itemView.findViewById(R.id.txtTanggal)
        val txtRuangan1: TextView = itemView.findViewById(R.id.txtRuangan1)
        val txtRuangan2: TextView = itemView.findViewById(R.id.txtRuangan2)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnHapus: ImageView = itemView.findViewById(R.id.btnHapusItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtTanggal.text = item.tanggal
        holder.txtRuangan1.text = "Batas Ruangan 1: ${item.ruangan1} W"
        holder.txtRuangan2.text = "Batas Ruangan 2: ${item.ruangan2} W"

        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnHapus.setOnClickListener { onDelete(item) }
    }

    fun updateData(newList: List<SettingModel>) {
        list = newList
        notifyDataSetChanged()
    }
}