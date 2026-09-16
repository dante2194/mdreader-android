package com.mdreader.data.tts

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mdreader.MainActivity
import com.mdreader.R
import com.mdreader.data.repository.PrefsRepository

/**
 * Foreground service for TTS playback, so it can continue in the background.
 */
class TtsService : Service() {

    private lateinit var ttsEngine: TtsEngine
    private var currentUtteranceId: String? = null
    private var isPlaying = false
    private val notificationId = 1

    // Binder for clients (activities) to interact with the service
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TtsService = this@TtsService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsEngine = TtsEngine(this, PrefsRepository(this))
        ttsEngine.setOnUtteranceCompleted {
            // When an utterance completes, we can move to the next chunk if any
            // For now, we just stop after each utterance (we speak full text at once)
            stopSelf()
        }
        ttsEngine.setOnError { error ->
            Log.e("TtsService", "TTS error: $error")
            stopSelf()
        }
        startForeground(notificationId, buildNotification("Tap to return to app"))
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "MD Reader TTS"
            val descriptionText = "Text-to-speech playback"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle incoming commands from activities
        val action = intent?.action
        when (action) {
            "ACTION_SPEAK" -> {
                val text = intent.getStringExtra("EXTRA_TEXT") ?: return START_NOT_STICKY
                val utteranceId = intent.getStringExtra("EXTRA_UTTERANCE_ID") ?: java.util.UUID.randomUUID().toString()
                speak(text, utteranceId)
            }
            "ACTION_STOP" -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun speak(text: String, utteranceId: String) {
        currentUtteranceId = utteranceId
        isPlaying = true
        ttsEngine.speak(text, utteranceId)
        updateNotification("Speaking...", true)
    }

    fun stop() {
        ttsEngine.stop()
        isPlaying = false
        updateNotification("Stopped", false)
        stopSelf()
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MD Reader")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher) // Use your launcher icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String, ongoing: Boolean) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MD Reader")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(ongoing)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "mdreader_tts_channel"
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsEngine.shutdown()
        // Remove the notification
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}
