package com.example.smartwatt.Model

data class RiwayatModel(
    var id: String = "",
    val tanggal: String = "",
    val waktu: String = "", // Harus dalam format "HH:mm:ss"
    val tegangan: Double = 0.0,
    val arus: Double = 0.0,
    val daya: Double = 0.0,
    val keterangan: String = "",
    val tempat: String = ""
)




