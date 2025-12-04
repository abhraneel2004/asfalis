package com.example.womensafetyapp

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
        val json = JSONObject()
        val motionArray = JSONArray()
        motionArray.put(JSONArray().put(x).put(y).put(z))
        json.put("window", motionArray)

        val requestBody = json.toString()

        Thread {
            try {
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestBody.toRequestBody(mediaType)
                val request = Request.Builder().url(serverUrl).post(body).build()
                val response = client.newCall(request).execute()
                val resString = response.body?.string()
                println("Server Response: $resString")

                // Optional: show toast on main thread
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
}
