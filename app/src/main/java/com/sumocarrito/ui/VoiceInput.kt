package com.sumocarrito.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Devuelve (estaEscuchando, iniciarODetener). Pide el micrófono solo al primer uso. */
@Composable
fun rememberVoiceInput(onText: (String) -> Unit, onError: (String) -> Unit): Pair<Boolean, () -> Unit> {
    val ctx = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    val text by rememberUpdatedState(onText)
    val err by rememberUpdatedState(onError)
    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    val begin: () -> Unit = begin@{
        val r = recognizer ?: run { err("Este teléfono no tiene reconocimiento de voz disponible"); return@begin }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                listening = false
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(text)
                    ?: err("No te escuché, intenta de nuevo")
            }
            override fun onError(code: Int) {
                listening = false
                err(if (code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    "No te escuché, intenta de nuevo" else "Error de voz ($code). Revisa la conexión o los datos de voz del teléfono")
            }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)   // usa el modelo local si está descargado
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
        listening = true
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else err("Permiso de micrófono denegado")
    }
    val toggle: () -> Unit = {
        when {
            listening -> { recognizer?.stopListening(); listening = false }
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> begin()
            else -> launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return listening to toggle
}
