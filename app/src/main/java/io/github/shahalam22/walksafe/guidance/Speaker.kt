package io.github.shahalam22.walksafe.guidance

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the user hears. A new instruction waits for the current one to
 * finish (only the newest waiting one is kept); an urgent one cuts it off.
 * Lives for the whole app, so a last message is not cut off when guidance stops.
 */
@Singleton
class Speaker @Inject constructor(@ApplicationContext context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var speaking = false
    private var pending: String? = null
    private var utteranceId = 0

    var rate: Float = 1f

    private val tts: TextToSpeech = TextToSpeech(context) { status -> main.post { onReady(status) } }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                main.post { next() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                main.post { next() }
            }
        })
    }

    fun say(text: String, interrupt: Boolean = false) {
        if (text.isBlank()) return
        main.post {
            when {
                !ready -> pending = text
                interrupt || !speaking -> {
                    pending = null
                    utter(text)
                }
                else -> pending = text
            }
        }
    }

    fun silence() {
        main.post {
            pending = null
            speaking = false
            if (ready) tts.stop()
        }
    }

    /** Two short beeps, before "Stop". */
    fun alarm() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            main.postDelayed({ tone.release() }, 400)
        }
    }

    private fun onReady(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts.language = Locale.US
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        pending?.let {
            pending = null
            utter(it)
        }
    }

    private fun utter(text: String) {
        speaking = true
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "walksafe-${++utteranceId}")
    }

    private fun next() {
        val text = pending
        pending = null
        if (text != null) utter(text) else speaking = false
    }
}
