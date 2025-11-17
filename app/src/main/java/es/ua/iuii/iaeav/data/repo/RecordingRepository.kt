package es.ua.iuii.iaeav.data.repo

import es.ua.iuii.iaeav.core.crypto.AesGcm
import es.ua.iuii.iaeav.core.crypto.Rsa
import es.ua.iuii.iaeav.core.crypto.b64u
import es.ua.iuii.iaeav.core.crypto.publicKeyFromPem
import es.ua.iuii.iaeav.data.api.CryptoApi
import es.ua.iuii.iaeav.data.api.RecordingsApi
import es.ua.iuii.iaeav.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/** Excepción para errores NO reintentables (p.ej., low_snr) */
class NonRetryableUploadException(message: String) : Exception(message)

class RecordingRepository(
    private val api: RecordingsApi,
    private val cryptoApi: CryptoApi
) {
    private val rng = SecureRandom()

    /**
     * Flujo:
     *  1) GET /crypto/public-key -> pem (+kid)
     *  2) AES-GCM (archivo completo) => ciphertext + tag con IV único
     *  3) POST /recordings/init -> upload_id + upload_token (+max_chunk_size)
     *  4) POST /recordings/{id}/chunk (multipart) con X-Upload-Offset y Bearer upload_token
     *  5) POST /recordings/{id}/complete (wrapped_key, iv, tag, sha256, alg/enc/kid)
     *
     * Lanza NonRetryableUploadException en low_snr u otros 4xx; otros errores se propagan.
     */
    suspend fun uploadEncrypted(
        wavFile: File,
        pseudonym: String,
        taskId: String,
        sampleRate: Int,
        channels: Int,
        lengthSeconds: Double,
        clientSnr: Double = 12.5
    ): CompleteRes {
        // 1) Clave pública
        val jwk = cryptoApi.getPublicKey()              // espera objeto con {pem, kid, alg, enc}
        val pem = jwk.pem
        val kid = jwk.kid ?: "upl-prod-01"
        val pub = publicKeyFromPem(pem)

        // 2) AES-GCM sobre TODO el archivo
        val dek = ByteArray(32).also { rng.nextBytes(it) }  // 256-bit
        val iv  = ByteArray(12).also { rng.nextBytes(it) }  // 96-bit
        val pt  = wavFile.readBytes()
        val ctWithTag = AesGcm.decryptOrEncrypt(dek, iv, pt, null, encrypt = true)
        val tag = ctWithTag.copyOfRange(ctWithTag.size - 16, ctWithTag.size)
        val ciphertext = ctWithTag.copyOfRange(0, ctWithTag.size - 16)

        val shaHex = MessageDigest.getInstance("SHA-256")
            .digest(ciphertext)
            .joinToString("") { "%02x".format(it) }

        // 3) INIT
        val initRes = api.init(
            InitReq(
                pseudonym = pseudonym,
                taskId = taskId,
                sampleRate = sampleRate,
                channels = channels,
                lengthSeconds = lengthSeconds,
                clientSnr = clientSnr
            )
        )
        val uploadId = initRes.uploadId
        val uploadToken = initRes.uploadToken
        val maxChunk = (initRes.maxChunkSize ?: 1_048_576).coerceAtLeast(64 * 1024)

        // 4) CHUNKS (solo ciphertext; el TAG va en /complete)
        var offset = 0L
        var partIndex = 0
        while (offset < ciphertext.size) {
            val end = (offset + maxChunk).coerceAtMost(ciphertext.size.toLong()).toInt()
            val part = ciphertext.copyOfRange(offset.toInt(), end)

            val body: RequestBody = part.toRequestBody(null)
            val mp = MultipartBody.Part.createFormData(
                name = "chunk",
                filename = "part_${"%06d".format(partIndex)}.bin",
                body = body
            )

            api.chunk(
                uploadId = uploadId,
                auth = "Bearer $uploadToken",
                offset = offset,
                chunk = mp
            )

            offset = end.toLong()
            partIndex++
        }

        // 5) COMPLETE (envolver DEK con RSA-OAEP-256)
        val wrapped = Rsa.wrapAesKey(dek, pub)
        try {
            return api.complete(
                uploadId = uploadId,
                auth = "Bearer $uploadToken",
                body = CompleteReq(
                    expectedSha256 = shaHex,
                    wrappedKey = b64u(wrapped),
                    iv = b64u(iv),
                    tag = b64u(tag),
                    alg = "RSA-OAEP-256",
                    enc = "A256GCM",
                    kid = kid
                )
            )
        } catch (e: HttpException) {
            // Los 4xx no deberían reintentar; detectamos low_snr explícitamente
            if (e.code() in 400..499) {
                val msg = e.response()?.errorBody()?.string().orEmpty()
                if ("low_snr" in msg) {
                    throw NonRetryableUploadException("Recording rejected by server (low_snr)")
                }
                throw NonRetryableUploadException("Client error ${e.code()}")
            }
            throw e
        }
    }
    // --- AÑADIR ESTA NUEVA FUNCIÓN ---
    suspend fun getMyRecordings(): Result<List<RecordingDto>> {
        return try {
            // 1. Llama a la API, que devuelve el objeto completo
            val response = api.getMyRecordings()

            // 2. Extrae solo la lista de grabaciones de ese objeto
            Result.success(response.recordings)
        } catch (e: Exception) {
            // Captura cualquier error de red o parsing
            Result.failure(e)
        }
    }
}
