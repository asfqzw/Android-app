package com.example.safesmarthome

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SmartHomeActivity : AppCompatActivity() {

    data class Room(
        val lightSwitch: MaterialSwitch,
        val fanSwitch: MaterialSwitch? = null,
        val fire: TextView? = null,
        val smoke: TextView? = null,
        val gas: TextView? = null
    )

    private val rooms = mutableMapOf<String, Room>()
    private lateinit var database: FirebaseDatabase
    private val activeDatabaseListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    companion object {
        // Change this if your database uses a different root path
        // Example: "users/{uid}/home1/sensors" or "apps/safesmarthome/sensors"
        private const val DB_ROOT_SENSORS = "sensors"

        // Change these to match the exact sensor keys in your database
        private const val SENSOR_FIRE_KEY = "fire"
        private const val SENSOR_SMOKE_KEY = "smoke"
        private const val SENSOR_GAS_KEY = "gas"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_home)

        // 🔧 Toolbar
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        val settingsPanel = findViewById<View>(R.id.settingsPanel)  // <- from include layout

// 🔐 Initialize the clickable items inside the settings panel
        val logoutLayout = settingsPanel.findViewById<View>(R.id.logout)
        val fingerprintLayout = settingsPanel.findViewById<View>(R.id.fingerprint)


        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_settings -> {
                    // Toggle settings panel visibility
                    if (settingsPanel.isVisible) {
                        settingsPanel.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                settingsPanel.visibility = View.GONE
                            }.start()
                    } else {
                        settingsPanel.alpha = 0f
                        settingsPanel.visibility = View.VISIBLE
                        settingsPanel.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }

// 🧠 Listeners for Logout and Fingerprint (stay outside the toolbar listener)
        logoutLayout.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("OK") { dialog, _ ->
                    // 🚪 Simulate logout (frontend only)
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, Signin::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        fingerprintLayout.setOnClickListener {
            val options = arrayOf("Add Fingerprint", "Delete Fingerprint")

            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Manage Fingerprint")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> Toast.makeText(this, "Add Fingerprint clicked", Toast.LENGTH_SHORT).show()
                        1 -> Toast.makeText(this, "Delete Fingerprint clicked", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }


        // 🏠 Initialize all rooms
        // IMPORTANT: Change the map keys (e.g., "Living", "Dining") to match
        // the room names used in your database under $DB_ROOT_SENSORS.
        rooms["Living"] = Room(
            lightSwitch = findViewById(R.id.switchlivingLight),
            fanSwitch = findViewById(R.id.switchlivingFan),
            fire = findViewById(R.id.livingroomFire),
            smoke = findViewById(R.id.livingSmoke)
        )

        rooms["Dining"] = Room(
            lightSwitch = findViewById(R.id.switchdiningLight),
            fanSwitch = findViewById(R.id.switchdiningFan),
            fire = findViewById(R.id.diningFire),
            smoke = findViewById(R.id.diningSmoke)
        )

        rooms["Bedroom"] = Room(
            lightSwitch = findViewById(R.id.switchbedroomLight),
            fanSwitch = findViewById(R.id.switchbedroomFan),
            fire = findViewById(R.id.bedroomFire),
            smoke = findViewById(R.id.bedroomSmoke)
        )

        rooms["Bathroom"] = Room(
            lightSwitch = findViewById(R.id.switchbathroomLight)
        )

        rooms["Kitchen"] = Room(
            lightSwitch = findViewById(R.id.switchkitchenLight),
            fire = findViewById(R.id.kitchenFire),
            smoke = findViewById(R.id.kitchenSmoke),
            gas = findViewById(R.id.kitchenGas)
        )

        // Firebase Realtime Database subscription for sensor values
        // If you use a non-default instance, pass your DB URL:
        // database = FirebaseDatabase.getInstance("https://your-db-id.europe-west1.firebasedatabase.app")
        database = FirebaseDatabase.getInstance()
        rooms.forEach { (roomName, room) ->
            // Change SENSOR_* constants above if your keys differ
            room.fire?.let { subscribeToSensorValue(roomName, SENSOR_FIRE_KEY, it) }
            room.smoke?.let { subscribeToSensorValue(roomName, SENSOR_SMOKE_KEY, it) }
            room.gas?.let { subscribeToSensorValue(roomName, SENSOR_GAS_KEY, it) }
        }

        // 🔌 Set listeners
        rooms.forEach { (_, room) ->
            room.lightSwitch.setOnCheckedChangeListener { _, _ ->
                // TODO: Light control
            }

            room.fanSwitch?.setOnCheckedChangeListener { _, _ ->
                // TODO: Fan control
            }
        }
    }

    private fun subscribeToSensorValue(
        roomName: String,
        sensorKey: String,
        targetView: TextView
    ) {
        // Reads from: sensors/{roomName}/{sensorKey} (or your adjusted path)
        val referencePath = "$DB_ROOT_SENSORS/$roomName/$sensorKey"
        val reference = database.getReference(referencePath)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stringValue: String? = when {
                    snapshot.getValue(String::class.java) != null -> snapshot.getValue(String::class.java)
                    snapshot.getValue(Boolean::class.java) != null -> snapshot.getValue(Boolean::class.java)?.toString()
                    snapshot.getValue(Number::class.java) != null -> snapshot.getValue(Number::class.java)?.toString()
                    else -> null
                }
                targetView.text = stringValue ?: targetView.text
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep current text if read fails
            }
        }
        reference.addValueEventListener(listener)
        activeDatabaseListeners.add(reference to listener)
    }

    override fun onDestroy() {
        // Detach all active database listeners to avoid leaks
        activeDatabaseListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeDatabaseListeners.clear()
        super.onDestroy()
    }

    fun updateRoomValues(
        roomName: String,
        fireValue: String? = null,
        smokeValue: String? = null,
        gasValue: String? = null
    ) {
        rooms[roomName]?.apply {
            fire?.text = fireValue ?: fire.text
            smoke?.text = smokeValue ?: smoke.text
            gas?.text = gasValue ?: gas.text
        }
    }
}
