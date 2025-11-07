package es.ua.iuii.iaeav.ui.record

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import es.ua.iuii.iaeav.core.audio.AudioRecorder
import es.ua.iuii.iaeav.workers.UploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

class RecordViewModel(private val appContext: Context) : ViewModel() {
    private val recorder = AudioRecorder()
    private val workManager = WorkManager.getInstance(appContext)

    // --- Flujo de estado para la UI ---
    // Expone el WorkInfo para que la UI reaccione a él
    private val _workInfo = MutableStateFlow<WorkInfo?>(null)
    val workInfo: StateFlow<WorkInfo?> = _workInfo.asStateFlow()
    // ------------------------------------

    fun startRecording(): File {
        _workInfo.value = null // Limpia el estado anterior
        val dir = File(appContext.cacheDir, "rec").apply { mkdirs() }
        val file = File(dir, "rec-${System.currentTimeMillis()}.wav")
        recorder.start(file)
        return file
    }

    fun stopAndEnqueueUpload(): File {
        val wav = recorder.stop()

        // --- Metadatos para el upload ---
        val meta = parseWavMeta(wav)
        val pseudonym = "android-${System.currentTimeMillis()}"
        val taskId = "default"
        val clientSnr = 12.5

        val uniqueWorkName = UploadWorker.enqueue(
            context = appContext,
            filePath = wav.absolutePath,
            pseudonym = pseudonym,
            taskId = taskId,
            sampleRate = meta.sampleRate,
            channels = meta.channels,
            lengthSeconds = meta.lengthSeconds,
            clientSnr = clientSnr
        )

        // --- ¡NUEVO! Observa el trabajo que acabamos de encolar ---
        observeWork(uniqueWorkName)
        // ----------------------------------------------------

        return wav
    }

    private fun observeWork(workName: String) {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(workName).collect { workInfos ->
                // Obtenemos el WorkInfo más reciente para este trabajo
                val info = workInfos.firstOrNull()
                _workInfo.value = info
            }
        }
    }

    data class WavMeta(
        val sampleRate: Int,
        val channels: Int,
        val lengthSeconds: Double
    )

    /** Parser muy simple del header WAV (PCM LE). Fallbacks seguros si algo falla. */
    private fun parseWavMeta(file: File): WavMeta {
        val fallbackRate = 42_000
        val fallbackCh = 1
        val fallbackLen = max(0L, file.length() - 44L) // aprox.

        return try {
            FileInputStream(file).use { fis ->
                val hdr = ByteArray(44)
                val read = fis.read(hdr)
                if (read < 44) return WavMeta(fallbackRate, fallbackCh, fallbackLen / (fallbackRate * fallbackCh * 2.0))

                fun leU16(off: Int) =
                    (hdr[off].toInt() and 0xFF) or ((hdr[off + 1].toInt() and 0xFF) shl 8)

                fun leU32(off: Int) =
                    (hdr[off].toInt() and 0xFF) or
                            ((hdr[off + 1].toInt() and 0xFF) shl 8) or
                            ((hdr[off + 2].toInt() and 0xFF) shl 16) or
                            ((hdr[off + 3].toInt() and 0xFF) shl 24)

                val channels = leU16(22).coerceAtLeast(1)
                val sampleRate = leU32(24).coerceAtLeast(8000)
                val bitsPerSample = leU16(34).coerceAtLeast(16)

                val dataBytes = max(0L, file.length() - 44L) // simplificación
                val lengthSec = if (sampleRate > 0 && channels > 0 && bitsPerSample > 0)
                    dataBytes * 8.0 / (sampleRate.toDouble() * channels * bitsPerSample)
                else
                    fallbackLen / (fallbackRate * fallbackCh * 2.0)

                WavMeta(sampleRate, channels, lengthSec)
            }
        } catch (_: Throwable) {
            WavMeta(fallbackRate, fallbackCh, fallbackLen / (fallbackRate * fallbackCh * 2.0))
        }
    }

    companion object {
        fun Factory(ctx: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecordViewModel(ctx.applicationContext) as T
        }
    }
}