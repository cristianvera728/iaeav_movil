package es.ua.iuii.iaeav.data.model

import com.squareup.moshi.Json

/**
 * # DTOs para Grabaciones y Subida Cifrada
 *
 * Define los Data Transfer Objects (DTOs) para el flujo de subida segmentada
 * de audio y cifrado híbrido (basado en el patrón de Sobres Digitales),
 * así como para la consulta de grabaciones existentes.
 */

// --- INICIAR SUBIDA (INIT) ---

/**
 * Petición para iniciar una sesión de subida de audio.
 * Proporciona los metadatos necesarios antes de empezar a enviar los chunks.
 */
data class InitReq(
    /** Pseudónimo del participante, usado para indexar el archivo. */
    val pseudonym: String,
    /** Identificador de la tarea o experimento. */
    @Json(name = "task_id") val taskId: String,
    /** Frecuencia de muestreo del audio original (ej. 16000). */
    @Json(name = "sample_rate") val sampleRate: Int,
    /** Número de canales de audio (ej. 1 para mono). */
    val channels: Int,
    /** Duración total del audio en segundos. */
    @Json(name = "length_seconds") val lengthSeconds: Double,
    /** Relación señal/ruido (SNR) calculada por el cliente. */
    @Json(name = "client_snr") val clientSnr: Double
)

/**
 * Respuesta del servidor tras iniciar una sesión de subida.
 * Contiene identificadores y límites para la subida.
 */
data class InitRes(
    /** ID de la sesión de subida temporal. Se usa en peticiones posteriores. */
    @Json(name = "upload_id") val uploadId: String,
    /** Token JWT de corta duración requerido para subir cada chunk. */
    @Json(name = "upload_token") val uploadToken: String,
    /** Información opcional sobre el cifrado. */
    val crypto: CryptoInfo?,
    /** Tamaño máximo recomendado (en bytes) para cada chunk. */
    @Json(name = "max_chunk_size") val maxChunkSize: Int?
)

/**
 * Contiene metadatos sobre el cifrado esperado/usado, como algoritmos y Key ID.
 */
data class CryptoInfo(
    /** Algoritmo de clave envuelta (ej. 'RSA-OAEP-256'). */
    val alg: String?,
    /** Algoritmo de cifrado de contenido (ej. 'A256GCM'). */
    val enc: String?,
    /** Identificador de la clave pública (KID) usada para cifrar la DEK. */
    val kid: String?
)

// --- FINALIZAR SUBIDA (COMPLETE) ---

/**
 * Petición para finalizar la subida, enviando el sobre digital (DEK envuelta, IV, Tag, Hash).
 */
data class CompleteReq(
    /** Hash SHA256 del archivo CIFRADO (ciphertext) enviado por chunks. */
    @Json(name = "expected_sha256") val expectedSha256: String,
    /** Clave de cifrado de datos (DEK) envuelta con la clave pública del servidor (RSA). */
    @Json(name = "wrapped_key") val wrappedKey: String,
    /** Vector de Inicialización (IV) usado para el cifrado AES-GCM. */
    val iv: String,
    /** Etiqueta de Autenticación (Tag) del cifrado AES-GCM. */
    val tag: String,
    /** Algoritmo de cifrado de clave (ej. RSA-OAEP-256). */
    val alg: String,
    /** Algoritmo de cifrado de datos (ej. A256GCM). */
    val enc: String,
    /** Key ID de la clave pública utilizada. */
    val kid: String
)

/**
 * Respuesta del servidor tras procesar la sesión completa y guardar el archivo.
 */
data class CompleteRes(
    /** Estado final del procesamiento (ej. "SUCCESS", "REJECTED"). */
    val status: String,
    /** Relación Señal/Ruido (SNR) calculada por el servidor para el audio. */
    val snr: Double,
    /** ID único de la grabación final en la base de datos de grabaciones. */
    @Json(name = "recording_id") val recordingId: String?
)

// --- DTOs AUXILIARES DE SUBIDA ---

/**
 * Respuesta para un endpoint que solo devuelve el token de subida y la URL.
 */
data class UploadTokenResponse(
    val upload_token: String,
    val upload_url: String
)

/**
 * Resultado de una subida individual (utilizado a menudo en resultados de WorkManager o listas).
 */
data class UploadResult(
    val id: String,
    val snr: Double,
    val status: String
)

// --- DTOs para Listar Grabaciones ---

/**
 * Representación de una grabación de audio almacenada en el servidor.
 */
data class RecordingDto(
    val id: String,
    /** Fecha y hora de creación, en formato ISO 8601. */
    val created_at: String,
    /** SNR calculado por el servidor (puede ser nulo si el procesamiento está pendiente). */
    val snr: Double?,
    /** Estado de la grabación (ej: "ACCEPTED", "REJECTED_SNR", "PENDING"). */
    val status: String,
    /** Nombre del fichero original (puede ser nulo). */
    val filename: String?
)

/**
 * Metadatos de paginación para las listas de resultados.
 */
data class PaginationDto(
    /** Número de página actual. */
    val page: Int,
    /** Número total de páginas. */
    val pages: Int,
    /** Cantidad de elementos por página. */
    val per_page: Int,
    /** Cantidad total de elementos disponibles. */
    val total: Int,
    /** Indica si hay una página siguiente. */
    val has_next: Boolean,
    /** Indica si hay una página anterior. */
    val has_prev: Boolean
)

/**
 * Objeto de respuesta para las peticiones de listado de grabaciones (ej. /recordings).
 */
data class RecordingsResponse(
    /** Lista de grabaciones en la página actual. */
    val recordings: List<RecordingDto>,
    /** Objeto que contiene la información de paginación. */
    val pagination: PaginationDto
)