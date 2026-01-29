package es.ua.iuii.iaeav.data.model

import com.squareup.moshi.Json

/**
 * # DTO Detallado de Grabación para la Respuesta del Polling
 *
 * Representa la información detallada de una grabación almacenada,
 * incluyendo metadatos, estado de procesamiento y estadísticas.
 */

/**
 * DTO que representa los detalles de una grabación.
 */
 data class RecordingDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "analysis_status") val analysisStatus: String,
    @Json(name = "prediction_result") val predictionResult: String?,
    @Json(name = "prediction_confidence") val predictionConfidence: Double?,
    @Json(name = "prediction_transcription") val predictionTranscription: String?,
    @Json(name = "snr_db") val snrDb: Double?,
    @Json(name = "created_at") val createdAt: String?
)
