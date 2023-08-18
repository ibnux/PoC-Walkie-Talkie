package com.ibnux.poc

import android.app.Application
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log

import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.model.AudioParam
import java.util.*

class VoicePingClientApp : Application() {
    private val TAG = "VoicePingClientApp"

    override fun onCreate() {
        super.onCreate()
        context = this
        val audioSource = AudioSourceConfig.getSource()
        val audioParam = AudioParam.Builder()
            .setAudioSource(audioSource)
            .build()
        val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
        Log.d(TAG, "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
        VoicePing.init(this, audioParam)
        val ttse: TextToSpeech by lazy {
            // Pass in context and the listener.
            TextToSpeech(this,
                TextToSpeech.OnInitListener { status ->
                    // set our locale only if init was success.
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeechEngine.language = Locale.UK
                    }
                })
        }
        textToSpeechEngine = ttse;
    }

    companion object {
        lateinit var context: Context
        lateinit var textToSpeechEngine: TextToSpeech
        var activityVisible = false;
    }

    override fun onTerminate() {
        textToSpeechEngine.shutdown()
        super.onTerminate()
    }

}