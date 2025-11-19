package es.ua.iuii.iaeav.workers

import android.content.Context
import androidx.work.*
import es.ua.iuii.iaeav.core.ServiceLocator
import es.ua.iuii.iaeav.data.repo.NonRetryableUploadException
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * # Worker de Subida Cifrada (UploadWorker)
 *
 * [CoroutineWorker] responsable de ejecutar el flujo completo de cifrado
 * y subida segmentada ([es.ua.iuii.iaeav.data.repo.RecordingRepository.uploadEncrypted])
 * en segundo plano, incluso si la aplicación se cierra.
 *
 * Maneja la lógica de reintento basada en el tipo de error retornado por el servidor.
 */
class UploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // Inicialización necesaria del ServiceLocator para acceder al repositorio
        ServiceLocator.init(applicationContext)
        val repo = ServiceLocator.recordingRepository

        // 1. Lectura de Metadatos del archivo de entrada
        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        val pseudonym = inputData.getString(KEY_PSEUDONYM) ?: return Result.failure()
        val taskId = inputData.getString(KEY_TASK) ?: "default"
        val sampleRate = inputData.getInt(KEY_SR, 16000)
        val channels = inputData.getInt(KEY_CH, 1)
        val lengthSeconds = inputData.getDouble(KEY_LEN, 0.0)
        val clientSnr = inputData.getDouble(KEY_SNR, 0.0)

        val file = File(path)
        // Validación de existencia del archivo local
        if (!file.exists()) return Result.failure()

        // 2. Ejecución del flujo de subida
        return try {
            val res = repo.uploadEncrypted(
                wavFile = file,
                pseudonym = pseudonym,
                taskId = taskId,
                sampleRate = sampleRate,
                channels = channels,
                lengthSeconds = lengthSeconds,
                clientSnr = clientSnr
            )
            // Éxito -> Devuelve el SNR y estado como datos de salida
            val outputData = workDataOf(
                KEY_OUTPUT_STATUS to res.status,
                KEY_OUTPUT_SNR to res.snr
            )
            Result.success(outputData)
        } catch (e: NonRetryableUploadException) {
            // Error No Reintentable (ej. low_snr o error de validación 4xx).
            // Devuelve FAILURE para no intentar de nuevo, notificando el error.
            val outputData = workDataOf(KEY_OUTPUT_ERROR to (e.message ?: "Error no reintentable"))
            Result.failure(outputData)
        } catch (e: Exception) {
            // Errores transitorios (ej. servidor caído 5xx, fallo de red).
            // Devuelve RETRY para que WorkManager intente la subida más tarde.
            Result.retry()
        }
    }

    /**
     * Objeto estático que define las claves para los datos de entrada/salida y la lógica
     * para encolar el trabajo.
     */
    companion object {
        // --- Claves de Entrada (Input Data) ---
        private const val KEY_PATH = "filePath"
        private const val KEY_PSEUDONYM = "pseudonym"
        private const val KEY_TASK = "taskId"
        private const val KEY_SR = "sampleRate"
        private const val KEY_CH = "channels"
        private const val KEY_LEN = "lengthSeconds"
        private const val KEY_SNR = "clientSnr"

        // --- Claves de Salida (Output Data) ---
        const val KEY_OUTPUT_STATUS = "output_status"
        const val KEY_OUTPUT_SNR = "output_snr"
        const val KEY_OUTPUT_ERROR = "output_error"

        /**
         * Encola una tarea de subida única en el [WorkManager].
         *
         * @param context Contexto de la aplicación.
         * @param filePath Ruta del archivo WAV local a subir.
         * @param pseudonym Pseudónimo del usuario.
         * @param taskId ID de la tarea.
         * @param sampleRate Frecuencia de muestreo.
         * @param channels Número de canales.
         * @param lengthSeconds Duración del audio.
         * @param clientSnr SNR calculado por el cliente.
         * @return El nombre único del trabajo encolado, útil para la observación.
         */
        fun enqueue(
            context: Context,
            filePath: String,
            pseudonym: String,
            taskId: String,
            sampleRate: Int,
            channels: Int,
            lengthSeconds: Double,
            clientSnr: Double
        ): String {
            // Prepara los datos de entrada
            val data = workDataOf(
                KEY_PATH to filePath,
                KEY_PSEUDONYM to pseudonym,
                KEY_TASK to taskId,
                KEY_SR to sampleRate,
                KEY_CH to channels,
                KEY_LEN to lengthSeconds,
                KEY_SNR to clientSnr
            )

            // Configura la petición (solicitud de trabajo)
            val req = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    // Define la política de reintento: exponencial, iniciando tras 20 segundos
                    BackoffPolicy.EXPONENTIAL,
                    20, TimeUnit.SECONDS
                )
                .build()

            // Define un nombre único basado en el hash del archivo para evitar trabajos duplicados
            val uniqueWorkName = "upload:${filePath.hashCode()}"

            // Encola la petición, reemplazando cualquier trabajo pendiente con el mismo nombre
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                req
            )

            return uniqueWorkName
        }
    }
}