package com.example.smartwatt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartwatt.Model.RiwayatModel
import com.example.smartwatt.R

class RiwayatAdapter(
    private var data: MutableList<RiwayatModel>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTanggal: TextView = itemView.findViewById(R.id.txtTanggal)
        val txtJam: TextView = itemView.findViewById(R.id.txtJam)
        val txtTegangan: TextView = itemView.findViewById(R.id.txtTegangan)
        val txtArus: TextView = itemView.findViewById(R.id.txtArus)
        val txtDaya: TextView = itemView.findViewById(R.id.txtDaya)
        val txtKeterangan: TextView = itemView.findViewById(R.id.txtKeterangan)
        val txtRuangan: TextView = itemView.findViewById(R.id.txtRuangan)
        val btnHapusItem: View = itemView.findViewById(R.id.btnHapusItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.txtTanggal.text = item.tanggal ?: "-"
        holder.txtJam.text = item.waktu ?: "-"
        holder.txtTegangan.text = "${item.tegangan} V"
        holder.txtArus.text = "${item.arus} A"
        holder.txtDaya.text = "${item.daya} W"
        holder.txtKeterangan.text = item.keterangan ?: "-"
        holder.txtRuangan.text = item.tempat ?: "-"

        holder.btnHapusItem.setOnClickListener {
            onDelete(item.id)
        }
    }

    fun updateList(newList: List<RiwayatModel>) {
        data = newList.toMutableList()
        notifyDataSetChanged()
    }
}
