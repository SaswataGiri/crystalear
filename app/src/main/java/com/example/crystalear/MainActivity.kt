package com.example.crystalear

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.crystalear.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var mBinding : ActivityMainBinding
    private var isRecording : Boolean = false
    private var amplificationFactor = 1.0
    private var noiseCancellation : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(mBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION)
        }

        mBinding.button.setOnClickListener {
            mBinding.button.text = getString(R.string.started)
            mBinding.button.setEnabled(false)
            mBinding.button2.setEnabled(true)
            mBinding.switch1.isEnabled = false
            startRealtimeAudio()
        }

        mBinding.button2.setOnClickListener {
            isRecording = false
            mBinding.button.text = getString(R.string.start)
            mBinding.switch1.isEnabled = true
            mBinding.button2.setEnabled(false)
            mBinding.button.setEnabled(true)
        }

        mBinding.switch1.setOnCheckedChangeListener { _, isChecked ->
                noiseCancellation = isChecked
        }

        mBinding.slider.addOnChangeListener { _, fl, _ ->
            amplificationFactor = fl.toDouble()
        }
    }

    private fun startRealtimeAudio() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission denied!", Toast.LENGTH_SHORT).show()
        }
        else {
            val recordingThread = Thread {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                isRecording = true
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize =
                    AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                val audioRecord = if(noiseCancellation) {
                    AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_PERFORMANCE)
                        .setAudioFormat(
                            AudioFormat.Builder().setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .setEncoding(audioFormat).build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                } else {
                    AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(
                            AudioFormat.Builder().setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .setEncoding(audioFormat).build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(audioFormat)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                val audioBuffer = ShortArray(bufferSize / 2)

                audioRecord.startRecording()
                audioTrack.play()

                while (isRecording) {
                    val readResult = audioRecord.read(audioBuffer, 0, audioBuffer.size)

                    if (readResult > 0) {
                        for (i in 0 until readResult) {
                            val amplifiedSample = (audioBuffer[i] * amplificationFactor).toInt()
                            audioBuffer[i] = amplifiedSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                        audioTrack.write(audioBuffer, 0, readResult)
                    }
                }

                audioRecord.stop()
                audioRecord.release()
                audioTrack.stop()
                audioTrack.release()
            }

            recordingThread.start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION = 100
    }
}