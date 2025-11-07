package es.ua.iuii.iaeav.core.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(
    private val sampleRate: Int = 42000
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var pcmFile: File? = null
    private var wavFile: File? = null

    fun start(outputWav: File) {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = (minBuffer * 2).coerceAtLeast(4096)

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord not initialized")
        }

        pcmFile = File(outputWav.parentFile, outputWav.nameWithoutExtension + ".raw")
        wavFile = outputWav

        isRecording = true
        recorder?.startRecording()

        recordingThread = Thread {
            FileOutputStream(pcmFile!!).use { fos ->
                val buf = ByteArray(bufferSize)
                while (isRecording) {
                    val read = recorder?.read(buf, 0, buf.size) ?: 0
                    if (read > 0) fos.write(buf, 0, read)
                }
            }
        }.also { it.start() }
    }

    fun stop(): File {
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingThread?.join()
        recordingThread = null

        // Escribir WAV (cabecera + PCM)
        val raw = pcmFile ?: throw IllegalStateException("No PCM file")
        val wav = wavFile ?: throw IllegalStateException("No WAV target")

        val pcmData = raw.readBytes()
        writeWav(wav, pcmData, sampleRate)
        raw.delete()
        return wav
    }

    private fun writeWav(wav: File, pcm: ByteArray, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcm.size
        val chunkSize = 36 + dataSize

        FileOutputStream(wav).use { out ->
            out.write("RIFF".toByteArray())
            out.write(intLE(chunkSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intLE(16))                // Subchunk1Size for PCM
            out.write(shortLE(1))               // AudioFormat PCM
            out.write(shortLE(channels.toShort()))
            out.write(intLE(sampleRate))
            out.write(intLE(byteRate))
            out.write(shortLE(blockAlign))
            out.write(shortLE(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intLE(dataSize))
            out.write(pcm)
        }
    }

    private fun intLE(v: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    private fun shortLE(v: Short) = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()
}
