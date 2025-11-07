package es.ua.iuii.iaeav.data.api

import es.ua.iuii.iaeav.data.model.CompleteReq
import es.ua.iuii.iaeav.data.model.CompleteRes
import es.ua.iuii.iaeav.data.model.InitReq
import es.ua.iuii.iaeav.data.model.InitRes
import okhttp3.MultipartBody
import retrofit2.http.*

interface RecordingsApi {

    @POST("recordings/init")
    suspend fun init(@Body body: InitReq): InitRes

    @Multipart
    @POST("recordings/{uploadId}/chunk")
    suspend fun chunk(
        @Path("uploadId") uploadId: String,
        @Header("Authorization") auth: String,               // "Bearer <upload_token>"
        @Header("X-Upload-Offset") offset: Long,
        @Part chunk: MultipartBody.Part                      // campo form-data "chunk"
    )

    @POST("recordings/{uploadId}/complete")
    suspend fun complete(
        @Path("uploadId") uploadId: String,
        @Header("Authorization") auth: String,
        @Body body: CompleteReq
    ): CompleteRes
}
