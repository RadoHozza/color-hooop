package com.example.hopcolor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var rootView: android.view.View
    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var idleGroup: LinearLayout
    private lateinit var startButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var sequenceRunning = false

    // Sekvencia: farba -> trvanie v ms
    // R10, potom 5x [G3, R7], koncove R ostava (ziadny dalsi prechod)
    private val sequence: List<Pair<Int, Long>> = listOf(
        Color.RED to 10_000L,
        Color.GREEN to 3_000L,
        Color.RED to 7_000L,
        Color.GREEN to 3_000L,
        Color.RED to 7_000L,
        Color.GREEN to 3_000L,
        Color.RED to 7_000L,
        Color.GREEN to 3_000L,
        Color.RED to 7_000L,
        Color.GREEN to 3_000L,
        Color.RED to 7_000L
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rootView = findViewById(R.id.root)
        statusText = findViewById(R.id.status)
        countdownText = findViewById(R.id.countdown)
        idleGroup = findViewById(R.id.idleGroup)
        startButton = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            if (!sequenceRunning) {
                runSequence()
            }
        }

        showIdleState()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initSpeechRecognizer()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initSpeechRecognizer()
        }
        // Ak povolenie chyba, hlasove ovladanie nebude fungovat, ale START tlacidlo funguje vzdy.
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.joinToString(" ")?.lowercase() ?: ""
                if (heard.contains("hop") && !sequenceRunning) {
                    runSequence()
                } else {
                    restartListening()
                }
            }

            override fun onError(error: Int) = restartListening()
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        listenOnce()
    }

    private fun listenOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sk-SK")
        }
        speechRecognizer.startListening(intent)
    }

    private fun restartListening() {
        if (sequenceRunning) return
        if (!::speechRecognizer.isInitialized) return
        handler.postDelayed({ listenOnce() }, 300)
    }

    private fun showIdleState() {
        rootView.setBackgroundColor(Color.RED)
        idleGroup.visibility = View.VISIBLE
        countdownText.visibility = View.GONE
    }

    private fun runSequence(step: Int = 0) {
        sequenceRunning = true
        idleGroup.visibility = View.GONE
        countdownText.visibility = View.VISIBLE

        if (step >= sequence.size) {
            sequenceRunning = false
            showIdleState()
            restartListening()
            return
        }

        val (color, duration) = sequence[step]
        rootView.setBackgroundColor(color)
        val totalSeconds = (duration / 1000L).toInt()
        tickCountdown(totalSeconds) {
            runSequence(step + 1)
        }
    }

    private fun tickCountdown(secondsLeft: Int, onDone: () -> Unit) {
        if (secondsLeft <= 0) {
            onDone()
            return
        }
        countdownText.text = secondsLeft.toString()
        handler.postDelayed({
            tickCountdown(secondsLeft - 1, onDone)
        }, 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}
