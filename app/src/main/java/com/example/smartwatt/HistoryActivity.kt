package com.example.smartwatt

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartwatt.Model.RiwayatModel
import com.example.smartwatt.adapter.RiwayatAdapter
import com.example.smartwatt.databinding.ActivityHistoryBinding
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: RiwayatAdapter
    private val riwayatList = mutableListOf<RiwayatModel>()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        tampilkanSemuaData()

        binding.btnZonaA.setOnClickListener {
            tampilkanRiwayatPerRuangan("ruangan1")
        }

        binding.btnZonaB.setOnClickListener {
            tampilkanRiwayatPerRuangan("ruangan2")
        }

        binding.btnSemuaZona.setOnClickListener {
            tampilkanSemuaData()
        }

        binding.btnHapusSemua.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi Hapus")
                .setMessage("Hapus semua riwayat?")
                .setPositiveButton("Ya") { _, _ ->
                    hapusSemua("ruangan1")
                    hapusSemua("ruangan2")
                    riwayatList.clear()
                    adapter.updateList(riwayatList)
                    cekKosong()
                }
                .setNegativeButton("Batal", null)
                .create()
            dialog.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = RiwayatAdapter(riwayatList) { id -> hapusRiwayat(id) }
        binding.rvRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayat.adapter = adapter
    }

    private fun tampilkanSemuaData() {
        riwayatList.clear()
        ambilRiwayat("ruangan1") {
            ambilRiwayat("ruangan2") {
                urutkanDanTampilkan()
            }
        }
    }

    private fun tampilkanRiwayatPerRuangan(ruangan: String) {
        riwayatList.clear()
        ambilRiwayat(ruangan) {
            urutkanDanTampilkan()
        }
    }

    private fun ambilRiwayat(ruangan: String, onComplete: () -> Unit) {
        database.child("riwayat/$ruangan")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val model = child.getValue(RiwayatModel::class.java)
                        model?.let {
                            it.id = child.key ?: ""
                            riwayatList.add(it)
                            Log.d("RiwayatDebug", "Data ditemukan: $it")
                        }
                    }
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HistoryActivity, "Gagal ambil data $ruangan", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })
    }

    private fun urutkanDanTampilkan() {
        riwayatList.sortByDescending { "${it.tanggal} ${it.waktu}" }
        adapter.updateList(riwayatList)
        cekKosong()
    }

    private fun hapusRiwayat(id: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Hapus riwayat ini?")
            .setPositiveButton("Ya") { _, _ ->
                // Hapus dari kedua ruangan (jika ada)
                val refs = listOf(
                    database.child("monitoring/riwayat/ruangan1/$id"),
                    database.child("monitoring/riwayat/ruangan2/$id")
                )
                refs.forEach { it.removeValue() }

                riwayatList.removeAll { it.id == id }
                adapter.updateList(riwayatList)
                cekKosong()
                Toast.makeText(this, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()
        dialog.show()
    }

    private fun hapusSemua(ruangan: String) {
        database.child("riwayat/$ruangan").removeValue()
    }

    private fun cekKosong() {
        val kosong = riwayatList.isEmpty()
        binding.rvRiwayat.visibility = if (kosong) View.GONE else View.VISIBLE
        binding.txtKosong.visibility = if (kosong) View.VISIBLE else View.GONE
        binding.btnHapusSemua.visibility = if (kosong) View.GONE else View.VISIBLE
    }
}
