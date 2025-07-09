package com.example.smartwatt

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.example.smartwatt.Model.SettingModel
import com.example.smartwatt.adapter.SettingAdapter
import com.example.smartwatt.databinding.ActivitySettingBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: SettingAdapter
    private val ambangList = mutableListOf<SettingModel>()

    private var isEditMode = false
    private var currentEditingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference.child("ambang_batas")

        adapter = SettingAdapter(ambangList, ::editData, ::deleteData)
        binding.recyclerRiwayat.layoutManager = LinearLayoutManager(this)
        binding.recyclerRiwayat.adapter = adapter

        binding.btnSimpan.setOnClickListener {
            if (isEditMode) updateData() else simpanData()
        }

        ambilData()
    }

    private fun simpanData() {
        val ruangan1 = binding.edtAmbang1.text.toString().toIntOrNull()
        val ruangan2 = binding.edtAmbang2.text.toString().toIntOrNull()
        if (ruangan1 == null || ruangan2 == null) {
            Toast.makeText(this, "Mohon isi nilai ambang batas dengan benar", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.push().key ?: return
        val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val data = SettingModel(id, tanggal, ruangan1, ruangan2)

        database.child(id).setValue(data).addOnSuccessListener {
            Toast.makeText(this, "Data disimpan", Toast.LENGTH_SHORT).show()
            clearInput()
        }
    }

    // Fungsi untuk memperbarui data ambang batas ke Firebase
    private fun updateData() {
        // Ambil input dari EditText, ubah ke integer jika valid
        val ruangan1 = binding.edtAmbang1.text.toString().toIntOrNull()
        val ruangan2 = binding.edtAmbang2.text.toString().toIntOrNull()
        val id = currentEditingId // ID data yang sedang diedit

        // Validasi input (tidak boleh kosong/null)
        if (ruangan1 == null || ruangan2 == null || id == null) {
            Toast.makeText(this, "Data tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil tanggal hari ini dalam format "dd-MM-yyyy"
        val tanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        // Buat objek data untuk disimpan ke Firebase
        val data = SettingModel(id, tanggal, ruangan1, ruangan2)

        // Simpan data ke Firebase berdasarkan ID-nya
        database.child(id).setValue(data).addOnSuccessListener {
            Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
            clearInput() // Kosongkan input setelah berhasil update
        }
    }

    // Fungsi untuk mengambil semua data ambang batas dari Firebase
    private fun ambilData() {
        // Dengarkan perubahan data pada node Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ambangList.clear() // Kosongkan list sebelum menambahkan data baru

                // Iterasi setiap item data di Firebase
                for (item in snapshot.children) {
                    val data = item.getValue(SettingModel::class.java)
                    if (data != null) ambangList.add(data)
                }

                // Update tampilan adapter, urutkan berdasarkan tanggal terbaru
                adapter.updateData(ambangList.sortedByDescending { it.tanggal })
            }

            override fun onCancelled(error: DatabaseError) {
                // Bisa ditambahkan log atau pesan error jika perlu
            }
        })
    }


    private fun deleteData(item: SettingModel) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                database.child(item.id).removeValue()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun editData(item: SettingModel) {
        isEditMode = true
        currentEditingId = item.id
        binding.edtAmbang1.setText(item.ruangan1.toString())
        binding.edtAmbang2.setText(item.ruangan2.toString())
        binding.btnSimpan.text = "Update"
        binding.btnSimpan.visibility = View.VISIBLE
    }

    private fun clearInput() {
        isEditMode = false
        currentEditingId = null
        binding.edtAmbang1.text.clear()
        binding.edtAmbang2.text.clear()
        binding.btnSimpan.visibility = View.GONE
    }
}
