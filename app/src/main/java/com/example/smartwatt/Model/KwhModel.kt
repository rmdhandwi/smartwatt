package com.example.smartwatt.Model

data class KwhModel(
    var id: String = "",
    val waktu: String = "", // Harus dalam format "HH:mm:ss"
    val kwh: Double = 0.0,
)
