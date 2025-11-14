package es.ua.iuii.iaeav.data.api

import es.ua.iuii.iaeav.data.model.LoginReq
import es.ua.iuii.iaeav.data.model.LoginRes
import es.ua.iuii.iaeav.data.model.GoogleLoginReq
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login") suspend fun login(@Body body: LoginReq): LoginRes
    @POST("auth/register") suspend fun register(@Body body: LoginReq): Unit
    @POST("auth/google/token") // <-- AÑADIR (o la ruta que use tu backend)
    suspend fun googleLogin(@Body body: GoogleLoginReq): LoginRes
}
