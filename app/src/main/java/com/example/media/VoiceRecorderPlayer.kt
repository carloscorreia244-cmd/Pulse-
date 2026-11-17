package com.example.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class VoiceRecorderPlayer(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startMillis: Long = 0L

    private var player: MediaPlayer? = null
    private var playingMessageId: String? = null

    fun startRecording(): Boolean {
        stopRecording(discard = true)
        return try {
            val outDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outFile = File(outDir, "voice_${UUID.randomUUID()}.m4a")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            currentFile = outFile
            startMillis = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            currentFile = null
            recorder = null
            false
        }
    }

    fun stopRecording(discard: Boolean = false): Pair<String?, Int> {
        val elapsedSec = (((System.currentTimeMillis() - startMillis) / 1000L).toInt()).coerceAtLeast(1)
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Safe ignore if stopped immediately
        } finally {
            recorder = null
        }
        val path = currentFile?.absolutePath
        if (discard) {
            currentFile?.delete()
            currentFile = null
            return null to 0
        }
        currentFile = null
        return path to elapsedSec
    }

    fun togglePlayback(
        messageId: String,
        mediaUri: String?,
        onStateChanged: (playingId: String?) -> Unit
    ) {
        if (playingMessageId == messageId) {
            stopPlayback()
            onStateChanged(null)
            return
        }
        stopPlayback()
        if (mediaUri.isNullOrBlank() || !File(mediaUri).exists()) {
            // Seeded voice notes without local file simulate short playback timer via caller
            onStateChanged(messageId)
            return
        }
        try {
            val mp = MediaPlayer().apply {
                setDataSource(mediaUri)
                prepare()
                setOnCompletionListener {
                    playingMessageId = null
                    onStateChanged(null)
                    release()
                }
                start()
            }
            player = mp
            playingMessageId = messageId
            onStateChanged(messageId)
        } catch (_: Exception) {
            playingMessageId = null
            onStateChanged(null)
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        } finally {
            player = null
            playingMessageId = null
        }
    }

    fun release() {
        stopRecording(discard = true)
        stopPlayback()
    }
}
