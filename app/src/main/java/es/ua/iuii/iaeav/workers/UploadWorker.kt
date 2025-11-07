package es.ua.iuii.iaeav.workers

import android.content.Context
import androidx.work.*
import es.ua.iuii.iaeav.core.ServiceLocator
import es.ua.iuii.iaeav.data.repo.NonRetryableUploadException
import java.io.File
import java.util.concurrent.TimeUnit

class UploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)
        val repo = ServiceLocator.recordingRepository

        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        val pseudonym = inputData.getString(KEY_PSEUDONYM) ?: return Result.failure()
        val taskId = inputData.getString(KEY_TASK) ?: "default"
        val sampleRate = inputData.getInt(KEY_SR, 16000)
        val channels = inputData.getInt(KEY_CH, 1)
        val lengthSeconds = inputData.getDouble(KEY_LEN, 0.0)
        val clientSnr = inputData.getDouble(KEY_SNR, 0.0)

        val file = File(path)
        if (!file.exists()) return Result.failure()

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
            // Éxito -> Devuelve el SNR y estado
            val outputData = workDataOf(
                KEY_OUTPUT_STATUS to res.status,
                KEY_OUTPUT_SNR to res.snr
            )
            Result.success(outputData)
        } catch (e: NonRetryableUploadException) {
            // No reintentar (low_snr u otros 4xx) -> Devuelve el mensaje de error
            val outputData = workDataOf(KEY_OUTPUT_ERROR to (e.message ?: "Error no reintentable"))
            Result.failure(outputData)
        } catch (e: Exception) {
            // Caídas transitorias (5xx, red) -> reintento
            Result.retry()
        }
    }

    companion object {
        private const val KEY_PATH = "filePath"
        private const val KEY_PSEUDONYM = "pseudonym"
        private const val KEY_TASK = "taskId"
        private const val KEY_SR = "sampleRate"
        private const val KEY_CH = "channels"
        private const val KEY_LEN = "lengthSeconds"
        private const val KEY_SNR = "clientSnr"

        // --- Claves de Salida ---
        const val KEY_OUTPUT_STATUS = "output_status"
        const val KEY_OUTPUT_SNR = "output_snr"
        const val KEY_OUTPUT_ERROR = "output_error"
        // -------------------------

        fun enqueue(
            context: Context,
            filePath: String,
            pseudonym: String,
            taskId: String,
            sampleRate: Int,
            channels: Int,
            lengthSeconds: Double,
            clientSnr: Double
        ): String { // <-- Devuelve el nombre del trabajo
            val data = workDataOf(
                KEY_PATH to filePath,
                KEY_PSEUDONYM to pseudonym,
                KEY_TASK to taskId,
                KEY_SR to sampleRate,
                KEY_CH to channels,
                KEY_LEN to lengthSeconds,
                KEY_SNR to clientSnr
            )

            val req = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    20, TimeUnit.SECONDS
                )
                .build()

            // Evita colas duplicadas para el mismo fichero
            val uniqueWorkName = "upload:${filePath.hashCode()}"
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                req
            )
            return uniqueWorkName // <-- Devuelve el nombre para poder observarlo
        }
    }
}