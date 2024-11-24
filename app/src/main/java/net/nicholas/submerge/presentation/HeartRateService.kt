package net.nicholas.submerge.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class HeartRateService : Service(), SensorEventListener {
    private val channelId = "submerge_hr"
    private val notificationId = 1

    private lateinit var handler: Handler
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var lastReading = 0f
    private var readingsReliable = true // Reliability of data

    // This is the only listener, registered once when the service starts
    private val periodicHeartRateUpdate: Runnable = object : Runnable {
        override fun run() {
            if (lastReading >= 90) {
                sendHeartRateNotification("Warning! Heart rate currently at ${lastReading}BPM!")
            }

            // Re-run this task every 30 seconds
            handler.postDelayed(this, 30000) // 30 seconds interval
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize the sensor manager and heart rate sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor != null) {
            // Register the sensor listener once when the service starts
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.e("Submerge", "Heart rate sensor not found!")
        }

        handler = Handler(mainLooper)
        handler.post(periodicHeartRateUpdate) // Start the periodic task

        // Create and start the foreground notification
        val notification = buildNotification("Heart rate monitoring started")
        startForeground(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the sensor listener when the service is destroyed
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(periodicHeartRateUpdate) // Remove the periodic task
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_HEART_RATE && readingsReliable) {
            val heartRate = event.values[0]

            if (heartRate > 40) {
                lastReading = heartRate
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
            readingsReliable = false
            Log.d("Submerge", "Readings unreliable!")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Send a notification with the heart rate value
    private fun sendHeartRateNotification(content: String) {
        val notification = buildNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    // Build a notification with the given content
    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Submerge")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false) // Prevents swiping away the notification
            .build()
    }

    // Create a notification channel for Android 8.0 (API level 26) and above
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Submerge",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for heart rate updates"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
