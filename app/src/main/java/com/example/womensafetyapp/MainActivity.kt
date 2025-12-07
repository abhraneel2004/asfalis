package com.example.womensafetyapp
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeThreshold = 12f // Refined sensitivity for real phone use

    // Use your ngrok URL or local IP if same Wi-Fi
    //private val serverUrl = "https://YOUR_NGROK_URL/predict"
    // private val serverUrl = "http://192.168.29.158:5000/predict"
    private val serverUrl = "https://unparcelled-eldon-nonglandular.ngrok-free.dev/predict"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.also { accel ->
            sensorManager.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = kotlin.math.abs(lastX - x)
            val deltaY = kotlin.math.abs(lastY - y)
            val deltaZ = kotlin.math.abs(lastZ - z)

            if (deltaX > shakeThreshold || deltaY > shakeThreshold || deltaZ > shakeThreshold) {
                // Shake/fall detected → send SOS
                sendSOS(x, y, z)
            }

            lastX = x
            lastY = y
            lastZ = z
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun sendSOS(x: Float, y: Float, z: Float) {

        // 1️⃣ Get location (may be "Unknown")
        val location = getLocation()  // <-- you already created this earlier

        // 2️⃣ Create JSON payload
        val json = JSONObject()

        val motionArray = JSONArray()
        motionArray.put(JSONArray().put(x).put(y).put(z))

        json.put("window", motionArray)
        json.put("location", location)   // <-- NEW FIELD

        val requestBody = json.toString()

        // 3️⃣ Send request in thread
        Thread {
            try {
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestBody.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val resString = response.body?.string()

                println("🔥 SOS SENT TO SERVER")
                println("📌 Motion: x=$x  y=$y  z=$z")
                println("📌 Location: $location")
                println("📌 Server Response: $resString")

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "SOS Sent!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to send SOS", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorListener)
    }
    private fun getLocation(): String {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val hasFine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) return "unknown"

        val location: Location? =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        return if (location != null) {
            "${location.latitude},${location.longitude}"
        } else {
            "unknown"
        }
    }

}
