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
