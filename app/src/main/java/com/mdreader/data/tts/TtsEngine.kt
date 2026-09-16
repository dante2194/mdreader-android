package com.mdreader.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.mdreader.data.repository.PrefsRepository
import java.util.Locale

/**
 * Wrapper around Android's TTS engine with settings and progress callbacks.
 */
class TtsEngine(
    private val context: Context,
    private val prefs: PrefsRepository
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private var onUtteranceCompleted: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var isSpeaking = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English as default, can be made configurable
            tts.setLanguage(Locale.US)
            // Apply saved settings
            updateSettings()
        } else {
            Log.e("TtsEngine", "TTS Initialization failed")
            onError?.invoke("TTS initialization failed")
        }
    }

    fun setOnUtteranceCompleted(callback: () -> Unit) {
        onUtteranceCompleted = callback
    }

    fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }

    fun speak(text: String, utteranceId: String) {
        if (!isSpeaking) {
            isSpeaking = true
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        tts.stop()
        isSpeaking = false
    }

    fun isSpeaking(): Boolean {
        return isSpeaking
    }

    fun setPitch(pitch: Float) {
        tts.setPitch(pitch)
    }

    fun setRate(rate: Float) {
        tts.setSpeechRate(rate)
    }

    private fun updateSettings() {
        val rate = prefs.ttsRate.first() ?: 1.0f
        val pitch = prefs.ttsPitch.first() ?: 1.0f
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
    }

    // Shutdown
    fun shutdown() {
        tts.shutdown()
    }

    // UtteranceProgressListener is set in the service
    fun setUtteranceListener(listener: UtteranceProgressListener) {
        tts.setOnUtteranceProgressListener(listener)
    }
}
