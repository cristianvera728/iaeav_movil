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

// DTO para recibir los datos del usuario (GET /users/me)
data class UserDto(
    val id: Int,
    val email: String,
    val username: String?, // <-- CAMBIADO de 'name' a 'username'
    val role: String
    // 'is_active' eliminado porque el servidor no lo envía
)

// DTO para cambiar la contraseña
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)