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

/**
 * Excepción específica lanzada cuando un error de subida es de tipo
 * no reintentable (ej. validación de cliente, baja relación señal/ruido (low_snr) del audio).
 *
 * @param message Mensaje descriptivo del error.
 */
class NonRetryableUploadException(message: String) : Exception(message)

/**
 * # Repositorio de Grabaciones (RecordingRepository)
 *
 * Clase que orquesta el complejo flujo de grabación, **cifrado híbrido seguro**
 * y la subida de archivos de audio segmentada (chunked upload) al servidor.
 *
 * Sigue el patrón de "Sobres Digitales" (Digital Envelope) utilizando AES-GCM para
 * los datos y RSA-OAEP para la clave de cifrado (DEK).
 *
 * @property api Interfaz de Retrofit para interactuar con los endpoints de subida.
 * @property cryptoApi Interfaz para obtener la clave pública del servidor.
 */
class RecordingRepository(
    private val api: RecordingsApi,
    private val cryptoApi: CryptoApi
) {
    /** Generador de números aleatorios seguro para crear claves (DEK) e IVs. */
    private val rng = SecureRandom()

    /**
     * Procesa, cifra y sube un archivo WAV local al servidor en un flujo multi-paso.
     *
     * ## Flujo de Subida Cifrada:
     * 1. **Obtener Clave Pública:** Consulta a [/crypto/public-key] para obtener la clave
     * RSA del servidor (para cifrar la clave de datos AES) y su KID.
     * 2. **Cifrar Datos:** Genera una DEK AES-256-GCM aleatoria y cifra el archivo WAV local.
     * 3. **Iniciar Sesión:** Llama a [/recordings/init] enviando metadatos. Recibe `uploadId` y `uploadToken`.
     * 4. **Subir Chunks:** El archivo cifrado se divide en partes que se suben a [/recordings/{id}/chunk]
     * utilizando el `uploadToken` para la autorización de la subida.
     * 5. **Finalizar (Complete):** Envuelve la DEK AES con la clave pública RSA del servidor.
     * Llama a [/recordings/{id}/complete] enviando la clave envuelta (`wrappedKey`), el IV,
     * la etiqueta de autenticación (Tag) y el hash SHA-256 del archivo cifrado.
     *
     * @param wavFile Archivo de audio local a subir.
     * @param pseudonym Pseudónimo del participante.
     * @param taskId ID de la tarea o experimento.
     * @param sampleRate Frecuencia de muestreo (ej. 16000 Hz).
     * @param channels Número de canales (ej. 1).
     * @param lengthSeconds Duración del audio en segundos.
     * @param clientSnr Relación señal/ruido calculada por el cliente (métricas de calidad).
     * @return [CompleteRes] Objeto de respuesta que confirma que el proceso fue exitoso.
     * @throws NonRetryableUploadException Si el servidor rechaza la grabación (ej. low_snr).
     * @throws HttpException Para errores de red o servidor no 4xx.
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
        val kid = jwk.kid ?: "upl-prod-01" // Key ID (identificador de la clave pública)
        val pub = publicKeyFromPem(pem)

        // 2) AES-GCM sobre TODO el archivo
        val dek = ByteArray(32).also { rng.nextBytes(it) }  // Data Encryption Key (DEK) de 256 bits
        val iv  = ByteArray(12).also { rng.nextBytes(it) }  // Initialization Vector (IV) de 96 bits
        val pt  = wavFile.readBytes() // Plaintext (Audio sin cifrar)
        val ctWithTag = AesGcm.decryptOrEncrypt(dek, iv, pt, null, encrypt = true)
        val tag = ctWithTag.copyOfRange(ctWithTag.size - 16, ctWithTag.size) // Authentication Tag
        val ciphertext = ctWithTag.copyOfRange(0, ctWithTag.size - 16) // Datos cifrados

        // Cálculo del hash SHA-256 del *archivo cifrado* (ciphertext) para verificación de integridad
        val shaHex = MessageDigest.getInstance("SHA-256")
            .digest(ciphertext)
            .joinToString("") { "%02x".format(it) }

        // 3) INICIAR SUBIDA
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
        val uploadToken = initRes.uploadToken // Token de corta duración para la subida
        val maxChunk = (initRes.maxChunkSize ?: 1_048_576).coerceAtLeast(64 * 1024)

        // 4) SUBIDA DE CHUNKS (solo ciphertext)
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

            // Petición de subida de chunk
            api.chunk(
                uploadId = uploadId,
                auth = "Bearer $uploadToken",
                offset = offset,
                chunk = mp
            )

            offset = end.toLong()
            partIndex++
        }

        // 5) FINALIZAR (COMPLETE)
        // Envolver la clave de datos (DEK) con la clave pública del servidor (RSA-OAEP-256)
        val wrapped = Rsa.wrapAesKey(dek, pub)
        try {
            return api.complete(
                uploadId = uploadId,
                auth = "Bearer $uploadToken",
                body = CompleteReq(
                    expectedSha256 = shaHex,
                    wrappedKey = b64u(wrapped), // Clave cifrada (Envuelto)
                    iv = b64u(iv),              // Initialization Vector
                    tag = b64u(tag),            // Authentication Tag
                    alg = "RSA-OAEP-256",       // Algoritmo de Envoltura (RSA)
                    enc = "A256GCM",            // Algoritmo de Datos (AES)
                    kid = kid
                )
            )
        } catch (e: HttpException) {
            // Manejo de errores específicos del cliente (4xx) que no se deben reintentar
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

    // --- Lógica para obtener grabaciones (Funcionalidad Deshabilitada/Eliminada) ---
    /**
     * [NOTA DE LEGADO] Obtiene la lista de grabaciones de voz subidas por el usuario actual.
     *
     * Esta funcionalidad puede haber sido deshabilitada en la UI.
     * @return [Result] con la lista de [RecordingDto] si es exitoso.
     */
    suspend fun getMyRecordings(): Result<List<RecordingDto>> {
        return try {
            // Llama a la API (Asumo que el DTO de respuesta es RecordingsRes(recordings: List<RecordingDto>))
            val response = api.getMyRecordings()

            // Extrae la lista del objeto de respuesta
            Result.success(response.recordings)
        } catch (e: Exception) {
            // Captura cualquier error de red o parsing
            Result.failure(e)
        }
    }
}