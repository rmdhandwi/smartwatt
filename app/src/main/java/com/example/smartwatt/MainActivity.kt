package com.example.smartwatt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import com.example.smartwatt.Auth.LoginActivity
import com.example.smartwatt.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var ambangId: String = ""
    private var batasRuangan1: Float = 0f
    private var batasRuangan2: Float = 0f


    private lateinit var pressAnim: android.view.animation.Animation
    private lateinit var releaseAnim: android.view.animation.Animation

    private val relayStates = BooleanArray(2) { false }
    private val CHANNEL_ID = "smartwatt_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        createNotificationChannel()
        initToggleButton()
        initCardAnimations()
        ambilAmbangBatas()
        listenToFirebaseData()

    }

    private fun toggleRuangan() {
        if (binding.zonaA.isVisible) {
            binding.zonaA.visibility = View.GONE
            binding.zonaB.visibility = View.VISIBLE
            binding.txtZona.text = "Ruangan 2"
            binding.btnZonaA.text = "Ruangan 1"
        } else {
            binding.zonaA.visibility = View.VISIBLE
            binding.zonaB.visibility = View.GONE
            binding.txtZona.text = "Ruangan 1"
            binding.btnZonaB.text = "Ruangan 2"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartWatt Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for SmartWatt app"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(notificationId: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun initToggleButton() {
        binding.btnRelay1.setOnClickListener {
            relayStates[0] = !relayStates[0]
            toggleRelay(binding.btnRelay1, relayStates[0])
            updateRelayStateInFirebase("relay1", relayStates[0])
        }

        binding.btnRelay2.setOnClickListener {
            relayStates[1] = !relayStates[1]
            toggleRelay(binding.btnRelay2, relayStates[1])
            updateRelayStateInFirebase("relay2", relayStates[1])
        }

        binding.btnZonaA.setOnClickListener { toggleRuangan() }
        binding.btnZonaB.setOnClickListener { toggleRuangan() }
    }

    private fun toggleRelay(button: View, isOn: Boolean) {
        if (button is android.widget.Button) {
            button.isSelected = isOn
            button.text = if (isOn) "ON" else "OFF"
            button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).duration = 100
            }

            val backgroundRes = if (isOn) R.drawable.btn_on else R.drawable.btn_off
            button.setBackgroundResource(backgroundRes)
        }
    }

    private fun updateRelayStateInFirebase(relayName: String, state: Boolean) {
        val path = when (relayName) {
            "relay1" -> "/monitoring/ruangan1/relay"
            "relay2" -> "/monitoring/ruangan2/relay"
            else -> ""
        }
        if (path.isNotEmpty()) {
            database.child(path).setValue(state)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCardAnimation(card: View, onClick: () -> Unit) {
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(pressAnim)
                MotionEvent.ACTION_UP -> {
                    v.startAnimation(releaseAnim)
                    onClick()
                }
                MotionEvent.ACTION_CANCEL -> v.startAnimation(releaseAnim)
            }
            true
        }
    }

    @SuppressLint("ResourceType")
    private fun initCardAnimations() {
        pressAnim = AnimationUtils.loadAnimation(this, R.animator.card_press)
        releaseAnim = AnimationUtils.loadAnimation(this, R.animator.card_release)

        setCardAnimation(binding.btnHome) {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        setCardAnimation(binding.btnHistory) {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        setCardAnimation(binding.btnLogout) {
            logoutUser()
        }
    }

    private fun logoutUser() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Logout")
        builder.setMessage("Apakah Anda yakin ingin logout?")
        builder.setPositiveButton("Ya") { _, _ ->
            auth.signOut()
            Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }


    private fun simpanRiwayat(ruangan: String, volt: Float, amper: Float, watt: Float) {
        val calendar = Calendar.getInstance()
        val tanggalFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val waktuFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val tanggal = tanggalFormat.format(calendar.time)
        val waktu = waktuFormat.format(calendar.time)

        val tempat = when (ruangan) {
            "ruangan1" -> "Ruangan 1"
            "ruangan2" -> "Ruangan 2"
            else -> "Tidak Diketahui"
        }

        val ket = when (ruangan) {
            "ruangan1" -> "Daya melebihi ${batasRuangan1}W di Ruangan 1!"
            "ruangan2" -> "Daya melebihi ${batasRuangan2}W di Ruangan 2!"
            else -> "Tidak Diketahui"
        }

        val data = mapOf(
            "tanggal" to tanggal,
            "waktu" to waktu,
            "tegangan" to (volt.toDouble() * 10).roundToInt() / 10.0,
            "arus" to (amper.toDouble() * 10).roundToInt() / 10.0,
            "daya" to (watt.toDouble() * 10).roundToInt() / 10.0,
            "keterangan" to ket,
            "tempat" to tempat
        )


        database.child("riwayat").child(ruangan).push().setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Riwayat disimpan ($tempat)", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan riwayat: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun ambilAmbangBatas() {
        val ambangRef = FirebaseDatabase.getInstance().getReference("ambang_batas")

        ambangRef.orderByChild("tanggal").limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (item in snapshot.children) {
                        ambangId = item.key ?: ""
                        val r1 = item.child("ruangan1").getValue(Int::class.java) ?: 0f
                        val r2 = item.child("ruangan2").getValue(Int::class.java) ?: 0f
                        batasRuangan1 = r1.toFloat()
                        batasRuangan2 = r2.toFloat()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Gagal ambil ambang batas", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun matikanRelayDanDisableTombol(relayPath: String, button: android.widget.Button) {
        // Matikan relay di Firebase
        database.child("monitoring").child(relayPath).child("relay").setValue(false)

        // Nonaktifkan tombol di UI
        button.isEnabled = false
        button.text = "OFF"
        button.setBackgroundResource(R.drawable.btn_off)
    }


    private fun listenToFirebaseData() {
        // Referensi ke node "ruangan1" di Firebase
        val ruangan1Ref = database.child("monitoring").child("ruangan1")
        // Referensi ke node "ruangan2" di Firebase
        val ruangan2Ref = database.child("monitoring").child("ruangan2")

        // Listener untuk mendengarkan perubahan data pada "ruangan1"
        ruangan1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val volt = snapshot.child("tegangan").getValue(Float::class.java) ?: 0f
                val amper = snapshot.child("arus").getValue(Float::class.java) ?: 0f
                val watt = snapshot.child("daya").getValue(Float::class.java) ?: 0f
                val relayStatus = snapshot.child("relay").getValue(Boolean::class.java) ?: false

                binding.txtVoltA.text = "$volt V"
                binding.txtAmperA.text = "$amper A"
                binding.txtWattA.text = "$watt W"

                if (watt > batasRuangan1) {
                    showNotification(
                        1,
                        "Peringatan Daya - Ruangan 1",
                        "Daya melebihi ${batasRuangan1}W di Ruangan 1!"
                    )
                    simpanRiwayat("ruangan1", volt, amper, watt)
//                    matikanRelayDanDisableTombol("ruangan1", binding.btnRelay1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal membaca data Ruangan 1", Toast.LENGTH_SHORT).show()
            }
        })


        // Listener untuk mendengarkan perubahan data pada "ruangan2"
        ruangan2Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val volt = snapshot.child("tegangan").getValue(Float::class.java) ?: 0f
                val amper = snapshot.child("arus").getValue(Float::class.java) ?: 0f
                val watt = snapshot.child("daya").getValue(Float::class.java) ?: 0f
                val relayStatus = snapshot.child("relay").getValue(Boolean::class.java) ?: false

                binding.txtVoltB.text = "$volt V"
                binding.txtAmperB.text = "$amper A"
                binding.txtWattB.text = "$watt W"

                if (watt > batasRuangan2) {
                    showNotification(
                        2,
                        "Peringatan Daya - Ruangan 2",
                        "Daya melebihi ${batasRuangan2}W di Ruangan 2!"
                    )
                    simpanRiwayat("ruangan2", volt, amper, watt)
//                    matikanRelayDanDisableTombol("ruangan2", binding.btnRelay2)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal membaca data Ruangan 2", Toast.LENGTH_SHORT).show()
            }
        })

    }


}
