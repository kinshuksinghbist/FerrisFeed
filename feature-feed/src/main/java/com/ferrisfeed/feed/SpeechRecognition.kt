package com.ferrisfeed.feed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Offline-first speech capture for the speaker-opening card flow (TODO 24).
 *
 * Platform [SpeechRecognizer] + RECORD_AUDIO only: no new dependencies, no
 * app-level cloud calls. The recognizer itself is an OS service and may use
 * the device's on-device model; the app never opens a socket here.
 *
 * Contract (`loading-states` + `doherty-threshold`): capture NEVER blocks
 * text readiness. Callers compose the lesson immediately and use the
 * callbacks only to flip [com.ferrisfeed.coreui.SpeakState] and to gate the
 * difficulty reward motion. Any error maps to Unavailable with a one-line
 * human reason — never a spinner loop.
 */
object SpeechRecognition {

    fun isAvailable(context: Context): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * One-shot listen for the hook phrase. Stops itself on the first result
     * or error. Caller owns the returned [SpeechRecognizer] and MUST call
     * [SpeechRecognizer.destroy] when the reel changes or the host disposes.
     */
    fun listenOnce(
        context: Context,
        onHeard: (transcript: String) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ): SpeechRecognizer? {
        if (!isAvailable(context)) {
            onUnavailable("Speech recognition is not available on this device.")
            return null
        }
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }
            .getOrNull()
        if (recognizer == null) {
            onUnavailable("Could not start the microphone.")
            return null
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Prefer the on-device model when the OS offers one; the app
            // itself still makes no network call for this flow.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val heard = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (heard.isNullOrEmpty()) {
                    onUnavailable("Did not catch that — reading works the same.")
                } else {
                    onHeard(heard)
                }
                runCatching { recognizer.destroy() }
            }

            override fun onError(error: Int) {
                onUnavailable(reasonFor(error))
                runCatching { recognizer.destroy() }
            }

            override fun onReadyForSpeech(params: Bundle) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle) = Unit
            override fun onEvent(eventType: Int, params: Bundle) = Unit
        })
        runCatching { recognizer.startListening(intent) }
            .onFailure { onUnavailable("Could not start the microphone.") }
        return recognizer
    }

    private fun reasonFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is off — reading works the same."
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Did not catch that — reading works the same."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_SERVER ->
            "Voice needs the system recognizer right now — reading works the same."
        else -> "Voice is off for now — reading works the same."
    }
}
