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

/**
 * # ViewModel para la Pantalla de Grabación (RecordViewModel)
 *
 * Clase responsable de:
 * 1. Controlar el ciclo de vida del grabador de audio ([AudioRecorder]).
 * 2. Encolar el archivo grabado para su subida cifrada asíncrona mediante [UploadWorker].
 * 3. Observar y exponer el estado del trabajo de subida a la UI ([workInfo]).
 *
 * @property appContext El contexto de la aplicación, utilizado para acceder a directorios y WorkManager.
 */
class RecordViewModel(private val appContext: Context) : ViewModel() {

    /** Instancia del grabador de audio. */
    private val recorder = AudioRecorder()

    /** Instancia del sistema de gestión de tareas en segundo plano. */
    private val workManager = WorkManager.getInstance(appContext)

    // --- Flujo de estado para la UI ---

    /** Estado mutable que contiene la información más reciente sobre la tarea de subida. */
    private val _workInfo = MutableStateFlow<WorkInfo?>(null)

    /** [StateFlow] público para que la UI observe el estado del [WorkInfo]. */
    val workInfo: StateFlow<WorkInfo?> = _workInfo.asStateFlow()
    // ------------------------------------

    /**
     * Inicia una nueva grabación de audio.
     *
     * @return El archivo [File] donde se almacenará temporalmente la grabación.
     */
    fun startRecording(): File {
        _workInfo.value = null // Limpia el estado de subida anterior
        val dir = File(appContext.cacheDir, "rec").apply { mkdirs() } // Crea el directorio de caché
        val file = File(dir, "rec-${System.currentTimeMillis()}.wav")
        recorder.start(file)
        return file
    }

    /**
     * Detiene la grabación actual y encola el archivo WAV resultante para su subida cifrada.
     *
     * 1. Detiene la grabación.
     * 2. Extrae metadatos esenciales del archivo WAV.
     * 3. Encola un [UploadWorker] con la ruta del archivo y sus metadatos.
     * 4. Comienza a observar el estado del trabajo encolado.
     *
     * @return El archivo WAV [File] que se acaba de detener.
     */
    fun stopAndEnqueueUpload(): File {
        val wav = recorder.stop()

        // --- Metadatos para el upload ---
        val meta = parseWavMeta(wav)
        val pseudonym = "android-${System.currentTimeMillis()}" // Genera un pseudónimo único
        val taskId = "default" // ID de tarea por defecto
        val clientSnr = 12.5 // SNR pre-calculado o valor por defecto

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

        // Observa el estado del trabajo recién encolado para notificar a la UI
        observeWork(uniqueWorkName)

        return wav
    }

    /**
     * Comienza a observar el estado de una tarea de [WorkManager] específica.
     *
     * @param workName El nombre único ([uniqueWorkName]) del trabajo a observar.
     */
    private fun observeWork(workName: String) {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(workName).collect { workInfos ->
                // Actualiza el WorkInfo expuesto con la información más reciente
                val info = workInfos.firstOrNull()
                _workInfo.value = info
            }
        }
    }

    /**
     * Data class para almacenar los metadatos relevantes extraídos del encabezado WAV.
     */
    data class WavMeta(
        val sampleRate: Int,
        val channels: Int,
        val lengthSeconds: Double
    )

    /**
     * Parser muy simple y tolerante a fallos del encabezado WAV (PCM Little Endian).
     *
     * Intenta extraer la frecuencia de muestreo, canales y la duración aproximada.
     * Utiliza valores de reserva (fallbacks) si la lectura es incompleta o inválida.
     *
     * @param file El archivo WAV grabado.
     * @return [WavMeta] con los metadatos extraídos o valores de reserva.
     */
    private fun parseWavMeta(file: File): WavMeta {
        val fallbackRate = 42_000
        val fallbackCh = 1
        val fallbackLen = max(0L, file.length() - 44L) // aprox. bytes de datos

        return try {
            FileInputStream(file).use { fis ->
                val hdr = ByteArray(44)
                val read = fis.read(hdr)
                // Si no se pudo leer el encabezado completo, usa fallbacks
                if (read < 44) return WavMeta(fallbackRate, fallbackCh, fallbackLen / (fallbackRate * fallbackCh * 2.0))

                // Funciones internas para leer enteros Little Endian
                fun leU16(off: Int) =
                    (hdr[off].toInt() and 0xFF) or ((hdr[off + 1].toInt() and 0xFF) shl 8)

                fun leU32(off: Int) =
                    (hdr[off].toInt() and 0xFF) or
                            ((hdr[off + 1].toInt() and 0xFF) shl 8) or
                            ((hdr[off + 2].toInt() and 0xFF) shl 16) or
                            ((hdr[off + 3].toInt() and 0xFF) shl 24)

                val channels = leU16(22).coerceAtLeast(1)
                val sampleRate = leU32(24).coerceAtLeast(8000)
                val bitsPerSample = leU16(34).coerceAtLeast(16) // Bits por muestra (ej. 16)

                // Cálculo de la duración
                val dataBytes = max(0L, file.length() - 44L)
                val lengthSec = if (sampleRate > 0 && channels > 0 && bitsPerSample > 0)
                    dataBytes * 8.0 / (sampleRate.toDouble() * channels * bitsPerSample)
                else
                    fallbackLen / (fallbackRate * fallbackCh * 2.0)

                WavMeta(sampleRate, channels, lengthSec)
            }
        } catch (_: Throwable) {
            // Captura cualquier excepción de lectura o I/O y devuelve fallbacks
            WavMeta(fallbackRate, fallbackCh, fallbackLen / (fallbackRate * fallbackCh * 2.0))
        }
    }

    /**
     * Factory estático para la creación de [RecordViewModel].
     * Se encarga de pasar el contexto de la aplicación al ViewModel para WorkManager.
     */
    companion object {
        fun Factory(ctx: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecordViewModel(ctx.applicationContext) as T
        }
    }
}