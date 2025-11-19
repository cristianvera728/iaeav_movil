package es.ua.iuii.iaeav.data.api

import es.ua.iuii.iaeav.data.model.CompleteReq
import es.ua.iuii.iaeav.data.model.CompleteRes
import es.ua.iuii.iaeav.data.model.InitReq
import es.ua.iuii.iaeav.data.model.InitRes
import es.ua.iuii.iaeav.data.model.RecordingDto
import es.ua.iuii.iaeav.data.model.RecordingsResponse
import es.ua.iuii.iaeav.data.model.UploadTokenResponse
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * # Interfaz de la API de Grabaciones (RecordingsApi)
 *
 * Define los endpoints de Retrofit para el flujo de subida de audio cifrado,
 * el cual se realiza en tres pasos (Init, Chunked Upload, Complete), y la
 * consulta de grabaciones almacenadas.
 */
interface RecordingsApi {

    /**
     * Paso 1: Inicia una nueva sesión de subida cifrada.
     *
     * El servidor reserva espacio y devuelve un [InitRes] que contiene el
     * `uploadId` y el `uploadToken` necesarios para los siguientes pasos.
     * @param body El [InitReq] con los metadatos iniciales del audio.
     * @return [InitRes] Objeto que contiene los tokens de sesión.
     */
    @POST("recordings/init")
    suspend fun init(@Body body: InitReq): InitRes

    /**
     * Paso 2: Sube un segmento (chunk) del archivo cifrado.
     *
     * Esta es una petición **Multipart** que se repite hasta que todo el archivo
     * cifrado haya sido enviado. Utiliza el token de subida para la autorización.
     * @param uploadId ID de la sesión de subida, obtenido en [init].
     * @param auth Token de subida (Bearer <upload_token>).
     * @param offset Posición (en bytes) del inicio de este segmento dentro del archivo total.
     * @param chunk El cuerpo de la parte multipart que contiene los bytes del segmento.
     */
    @Multipart
    @POST("recordings/{uploadId}/chunk")
    suspend fun chunk(
        @Path("uploadId") uploadId: String,
        @Header("Authorization") auth: String,               // "Bearer <upload_token>"
        @Header("X-Upload-Offset") offset: Long,
        @Part chunk: MultipartBody.Part                      // campo form-data "chunk"
    )

    /**
     * Paso 3: Finaliza la sesión de subida, enviando el "sobre digital".
     *
     * El servidor utiliza los datos enviados en [CompleteReq] (clave envuelta, IV, Tag)
     * para verificar la integridad y desencriptar la clave de datos.
     * @param uploadId ID de la sesión de subida.
     * @param auth Token de subida.
     * @param body El [CompleteReq] que contiene el sobre digital y el hash.
     * @return [CompleteRes] Objeto con el resultado final (ej. SNR calculado).
     */
    @POST("recordings/{uploadId}/complete")
    suspend fun complete(
        @Path("uploadId") uploadId: String,
        @Header("Authorization") auth: String,
        @Body body: CompleteReq
    ): CompleteRes

    // --- ENDPOINT DE CONSULTA ---
    /**
     * Obtiene una lista paginada de las grabaciones de voz del usuario actual.
     *
     * @return [RecordingsResponse] Objeto que contiene la lista de [RecordingDto] y la paginación.
     */
    @GET("recordings")
    suspend fun getMyRecordings(): RecordingsResponse

}