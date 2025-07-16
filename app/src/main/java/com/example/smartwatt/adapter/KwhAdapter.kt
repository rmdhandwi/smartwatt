package com.example.smartwatt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartwatt.Model.KwhModel
import com.example.smartwatt.R

class KwhAdapter(
    private var list: List<KwhModel>,
    private val onDelete: (KwhModel) -> Unit
) : RecyclerView.Adapter<KwhAdapter.KwhViewHolder>() {

    inner class KwhViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtWaktu: TextView = itemView.findViewById(R.id.txtWaktu)
        val txtKwh: TextView = itemView.findViewById(R.id.txtKwh)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KwhViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kwh, parent, false)
        return KwhViewHolder(view)
    }

    override fun onBindViewHolder(holder: KwhViewHolder, position: Int) {
        val data = list[position]
        holder.txtWaktu.text = "Waktu: ${data.waktu}" ?: "-"
        holder.txtKwh.text = "KWH: ${data.kwh}" ?: "-"
        holder.btnDelete.setOnClickListener {
            onDelete(data)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<KwhModel>) {
        list = newList
        notifyDataSetChanged()
    }
}
