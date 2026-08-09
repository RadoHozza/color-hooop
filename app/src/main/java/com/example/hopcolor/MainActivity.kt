package com.example.hopcolor

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: android.view.View
    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var startResetButton: Button
    private lateinit var endButton: Button
    private lateinit var countdownSwitch: Switch
    private lateinit var volumeSeekBar: SeekBar
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
        Color.RED to 3_000L
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rootView = findViewById(R.id.root)
        statusText = findViewById(R.id.status)
        countdownText = findViewById(R.id.countdown)
        startResetButton = findViewById(R.id.startResetButton)
        endButton = findViewById(R.id.endButton)
        countdownSwitch = findViewById(R.id.countdownSwitch)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)

        startResetButton.setOnClickListener {
            if (sequenceRunning) {
                resetSequence()
            } else {
                runSequence()
            }
        }

        endButton.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            finishAffinity()
            Process.killProcess(Process.myPid())
        }

        showIdleState()
    }

    private fun showIdleState() {
        rootView.setBackgroundColor(Color.RED)
        statusText.visibility = View.VISIBLE
        countdownText.visibility = View.GONE
        startResetButton.text = "START"
    }

    private fun resetSequence() {
        handler.removeCallbacksAndMessages(null)
        sequenceRunning = false
        showIdleState()
    }

    private fun runSequence(step: Int = 0) {
        sequenceRunning = true
        statusText.visibility = View.GONE
        countdownText.visibility = if (countdownSwitch.isChecked) View.VISIBLE else View.GONE
        startResetButton.text = "RESET"

        if (step >= sequence.size) {
            sequenceRunning = false
            showIdleState()
            return
        }

        val (color, duration) = sequence[step]
        rootView.setBackgroundColor(color)
        if (color == Color.GREEN) {
            playBeep(500)
        } else {
            playBeep(200)
        }
        val totalSeconds = (duration / 1000L).toInt()
        tickCountdown(totalSeconds) {
            runSequence(step + 1)
        }
    }

    private fun playBeep(durationMs: Int) {
        val linear = volumeSeekBar.progress / 100f
        if (linear <= 0f) return
        // Kvadraticka krivka - v polovici posuvnika je hlasitost citelne tichsia
        // (linearny gain by v polke znel takmer rovnako hlasno ako na maxime)
        val volume = linear * linear
        Thread {
            try {
                val sampleRate = 44100
                // 3000 Hz - blizko vrcholu citlivosti ludskeho sluchu (2-4 kHz),
                // preraz aj cez chranice sluchu/slucadla na strelnici
                val frequency = 3000.0
                val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
                val buffer = ShortArray(numSamples)
                val period = sampleRate / frequency
                for (i in buffer.indices) {
                    // Stvorcova vlna namiesto sinusu - vyssia vnimana hlasitost
                    // a viac harmonickych zloziek, lepsie prerazi hluk vystrelov
                    val phase = (i % period) / period
                    buffer[i] = if (phase < 0.5) Short.MAX_VALUE else Short.MIN_VALUE
                }
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            // ALARM stream - najhlasnejsi dostupny, casto ide aj cez stlmenie telefonu
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.setVolume(volume)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // ticho ignorovat, zvuk nie je kriticky pre beh programu
            }
        }.start()
    }

    private fun tickCountdown(secondsLeft: Int, onDone: () -> Unit) {
        if (secondsLeft <= 0) {
            onDone()
            return
        }
        if (countdownSwitch.isChecked) {
            countdownText.text = secondsLeft.toString()
        }
        handler.postDelayed({
            tickCountdown(secondsLeft - 1, onDone)
        }, 1000L)
    }
}
