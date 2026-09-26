package com.esp8266.gamecontroller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import java.net.HttpURLConnection
import java.net.URL

class GameControlService : AccessibilityService() {

    private var running = true
    private var lastCommand = "NONE"

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true

        Thread {
            while (running) {
                try {
                    val prefs =
                        getSharedPreferences("controller", MODE_PRIVATE)

                    val ip = prefs.getString(
                        "esp_ip",
                        "192.168.1.9"
                    ) ?: "192.168.1.9"

                    val url = URL("http://$ip/controller")

                    val connection =
                        url.openConnection() as HttpURLConnection

                    connection.connectTimeout = 500
                    connection.readTimeout = 500
                    connection.requestMethod = "GET"

                    val command =
                        connection.inputStream
                            .bufferedReader()
                            .readText()
                            .trim()
                            .uppercase()

                    connection.disconnect()

                    if (command != lastCommand) {
                        lastCommand = command
                        performCommand(command)
                    }

                } catch (_: Exception) {
                    // ESP may temporarily be unreachable
                }

                Thread.sleep(50)
            }
        }.start()
    }

    private fun performCommand(command: String) {

        when (command) {

            "FORWARD" -> tap(540f, 1650f)

            "BACKWARD" -> tap(540f, 1900f)

            "LEFT" -> tap(150f, 1750f)

            "RIGHT" -> tap(350f, 1750f)

            "NONE" -> {
                // No button pressed
            }
        }
    }

    private fun tap(x: Float, y: Float) {

        val path = Path()
        path.moveTo(x, y)

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        100
                    )
                )
                .build()

        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }
}
