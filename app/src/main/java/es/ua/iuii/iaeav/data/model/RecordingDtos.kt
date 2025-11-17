package es.ua.iuii.iaeav.data.model

import com.squareup.moshi.Json

// INIT
data class InitReq(
    val pseudonym: String,
    @Json(name = "task_id") val taskId: String,
    @Json(name = "sample_rate") val sampleRate: Int,
    val channels: Int,
    @Json(name = "length_seconds") val lengthSeconds: Double,
    @Json(name = "client_snr") val clientSnr: Double
)

data class InitRes(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "upload_token") val uploadToken: String,
    val crypto: CryptoInfo?,
    @Json(name = "max_chunk_size") val maxChunkSize: Int?
)

data class CryptoInfo(
    val alg: String?,
    val enc: String?,
    val kid: String?
)

// COMPLETE
data class CompleteReq(
    @Json(name = "expected_sha256") val expectedSha256: String,
    @Json(name = "wrapped_key") val wrappedKey: String,
    val iv: String,
    val tag: String,
    val alg: String,
    val enc: String,
    val kid: String
)

data class CompleteRes(
    val status: String,
    val snr: Double,
    @Json(name = "recording_id") val recordingId: String?
)

data class UploadTokenResponse(
    val upload_token: String,
    val upload_url: String
)

data class UploadResult(
    val id: String,
    val snr: Double,
    val status: String
)

// --- AÑADIR ESTA NUEVA DATA CLASS ---
data class RecordingDto(
    val id: String,
    val created_at: String, // Recibiremos la fecha como un String (ISO 8601)
    val snr: Double?, // Puede ser nulo si está pendiente
    val status: String, // Ej: "ACCEPTED", "REJECTED_SNR"
    val filename: String? // Nombre del fichero original
)

// DTO para el objeto "pagination" que viene en la respuesta
data class PaginationDto(
    val page: Int,
    val pages: Int,
    val per_page: Int,
    val total: Int,
    val has_next: Boolean,
    val has_prev: Boolean
)

// DTO para la respuesta completa de /recordings
data class RecordingsResponse(
    val recordings: List<RecordingDto>,
    val pagination: PaginationDto
)
