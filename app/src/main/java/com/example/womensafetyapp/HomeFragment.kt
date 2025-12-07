package com.example.womensafetyapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {

    private val serverUrl = "https://unparcelled-eldon-nonglandular.ngrok-free.dev/predict"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val sosButton = view.findViewById<Button>(R.id.sos_button)
        val braceletImage = view.findViewById<ImageView>(R.id.bracelet_image)
        val braceletToggle = view.findViewById<ToggleButton>(R.id.bracelet_toggle)

        braceletToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                braceletImage.setImageResource(R.drawable.ic_bracelet_color)
            } else {
                braceletImage.setImageResource(R.drawable.ic_bracelet_grayscale)
            }
        }

        sosButton.setOnClickListener {
            sendSOS(0f, 0f, 0f) // Sending dummy values on button click
        }

        return view
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
                activity?.runOnUiThread {
                    Toast.makeText(context, "SOS Sent!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to send SOS", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}