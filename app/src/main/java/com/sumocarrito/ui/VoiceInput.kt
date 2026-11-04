package com.sumocarrito.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Idiomas a probar en orden. Error 12/13 = idioma no soportado/no disponible en ese teléfono. "" = idioma del sistema. */
private val LANGS = listOf("es-VE", "es-419", "es-US", "es-ES", "")

/** Devuelve (estaEscuchando, iniciarODetener). Pide el micrófono solo al primer uso. */
@Composable
fun rememberVoiceInput(onText: (String) -> Unit, onError: (String) -> Unit): Pair<Boolean, () -> Unit> {
    val ctx = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    val text by rememberUpdatedState(onText)
    val err by rememberUpdatedState(onError)
    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null }
    val handler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { handler.removeCallbacksAndMessages(null); recognizer?.destroy() } }

    fun start(idx: Int) {
        val r = recognizer ?: run { err("Este teléfono no tiene reconocimiento de voz disponible"); return }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                listening = false
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(text)
                    ?: err("No te escuché, intenta de nuevo")
            }
            override fun onError(code: Int) {
                if ((code == 12 || code == 13) && idx + 1 < LANGS.size) {      // idioma no soportado → siguiente variante
                    handler.postDelayed({ start(idx + 1) }, 250); return
                }
                listening = false
                err(when (code) {
                    12, 13 -> "Tu teléfono no tiene el idioma español para dictado. Ve a Ajustes → Sistema → Idioma y entrada → Voz (Google) y descarga Español."
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché, intenta de nuevo"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para el dictado. Descarga el español sin conexión en los ajustes de voz de Google."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado, intenta otra vez"
                    else -> "Error de voz ($code). Intenta de nuevo"
                })
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
            if (LANGS[idx].isNotEmpty()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGS[idx])
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
        listening = true
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start(0) else err("Permiso de micrófono denegado")
    }
    val toggle: () -> Unit = {
        when {
            listening -> { handler.removeCallbacksAndMessages(null); recognizer?.stopListening(); listening = false }
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> start(0)
            else -> launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return listening to toggle
}
