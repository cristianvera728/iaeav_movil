package es.ua.iuii.iaeav.data.api

import es.ua.iuii.iaeav.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/login") suspend fun login(@Body body: LoginReq): LoginRes
    @POST("auth/register") suspend fun register(@Body body: LoginReq): Unit
    @POST("auth/google/token") // <-- AÑADIR (o la ruta que use tu backend)
    suspend fun googleLogin(@Body body: GoogleLoginReq): LoginRes
    // --- NUEVOS ENDPOINTS ---

    @GET("users/me")
    suspend fun getMe(): UserDto

    @PUT("users/me/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}
