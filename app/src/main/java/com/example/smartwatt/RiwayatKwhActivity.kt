package com.example.smartwatt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartwatt.Model.KwhModel
import com.example.smartwatt.adapter.KwhAdapter
import com.example.smartwatt.databinding.ActivityHistoryBinding
import com.example.smartwatt.databinding.ActivityRiwayatKwhBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RiwayatKwhActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatKwhBinding
    private lateinit var adapter: KwhAdapter
    private lateinit var database: FirebaseDatabase
    private var currentPath = "kwh1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiwayatKwhBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()

        adapter = KwhAdapter(listOf()) { kwh ->
            deleteItem(kwh)
        }

        binding.recyclerKwh.layoutManager = LinearLayoutManager(this)
        binding.recyclerKwh.adapter = adapter

        // Button listeners
        binding.btnKwh1.setOnClickListener {
            currentPath = "kwh1"
            loadData("kwh1")
        }

        binding.btnKwh2.setOnClickListener {
            currentPath = "kwh2"
            loadData("kwh2")
        }

        binding.btnAll.setOnClickListener {
            loadAllData()
        }

        binding.btnClear.setOnClickListener {
            clearAllData()
        }

        loadData(currentPath)
    }

    private fun loadData(path: String) {
        val ref = database.getReference("riwayat").child(path)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dataList = mutableListOf<KwhModel>()
                snapshot.children.forEach {
                    val item = it.getValue(KwhModel::class.java)
                    item?.id = it.key ?: ""
                    item?.let { dataList.add(it) }
                }
                adapter.updateData(dataList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RiwayatKwhActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllData() {
        val allData = mutableListOf<KwhModel>()
        val riwayatRef = database.getReference("riwayat")

        riwayatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { ruangan ->
                    ruangan.children.forEach {
                        val item = it.getValue(KwhModel::class.java)
                        item?.id = it.key ?: ""
                        item?.let { allData.add(it) }
                    }
                }
                adapter.updateData(allData)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RiwayatKwhActivity, "Gagal memuat semua data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteItem(kwh: KwhModel) {
        val ref = database.getReference("riwayat").child(currentPath).child(kwh.id)
        ref.removeValue().addOnSuccessListener {
            Toast.makeText(this, "Data dihapus", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal menghapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAllData() {
        val ref = database.getReference("riwayat").child(currentPath)
        ref.removeValue().addOnSuccessListener {
            Toast.makeText(this, "Semua data dihapus", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal menghapus semua data", Toast.LENGTH_SHORT).show()
        }
    }
}
