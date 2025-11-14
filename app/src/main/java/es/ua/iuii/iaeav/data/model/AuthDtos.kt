package es.ua.iuii.iaeav.data.model

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName

data class LoginReq(
    val username: String,
    val password: String
)

data class LoginRes(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token") val token: String? = null,
    val refresh_token: String? = null,
    val status: String? = null
) {
    fun jwt(): String =
        accessToken ?: token
        ?: throw IllegalStateException("Respuesta sin token válido")
}
data class GoogleLoginReq(
    // 👇 2. AÑADE ESTA ANOTACIÓN
    @field:Json(name = "id_token")
    val idToken: String
)