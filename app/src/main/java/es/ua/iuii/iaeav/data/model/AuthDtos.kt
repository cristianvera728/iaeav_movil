package es.ua.iuii.iaeav.data.model

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName // Aunque no se usa directamente en este archivo, se mantiene por si el proyecto lo requiere.

/**
 * # DTOs para el Flujo de Autenticación y Perfil
 *
 * Define los Data Transfer Objects (DTOs) utilizados para la comunicación
 * de datos de usuario, login, registro y gestión de la sesión con el backend.
 */

// --- LOGIN ---

/**
 * Petición enviada al endpoint de login (POST /auth/login).
 */
data class LoginReq(
    /** Identificador de inicio de sesión: puede ser el **nombre de usuario** o el **correo electrónico** del usuario. */
    @Json(name = "login_identifier") val loginIdentifier: String,
    /** Contraseña del usuario en texto plano. */
    val password: String
)

/**
 * Respuesta recibida tras un login o autenticación exitosa.
 */
data class LoginRes(
    /** Token JWT principal para la autenticación (Bearer Token), puede ser 'access_token'. */
    @Json(name = "access_token") val accessToken: String? = null,
    /** Token JWT principal para la autenticación (Bearer Token), puede ser 'token'. */
    @Json(name = "token") val token: String? = null,
    /** Token utilizado para renovar el token de acceso (opcional). */
    val refresh_token: String? = null,
    /** Estado de la respuesta (ej. "success"). */
    val status: String? = null
) {
    /**
     * Extrae el token JWT principal, priorizando 'accessToken' y luego 'token'.
     * @throws IllegalStateException si ninguno de los campos de token está presente.
     */
    fun jwt(): String =
        accessToken ?: token
        ?: throw IllegalStateException("Respuesta sin token válido")
}

// --- REGISTRO ---

/**
 * Petición enviada al endpoint de registro (POST /auth/register).
 */
data class RegisterReq(
    /** Nombre de usuario para la nueva cuenta. */
    val username: String,
    /** Correo electrónico, usado como identificador de contacto. */
    val email: String,
    /** Contraseña para la nueva cuenta. */
    val password: String
)

// --- AUTENTICACIÓN GOOGLE (OIDC) ---

/**
 * Petición enviada al backend para validar un ID Token de Google recibido en el móvil.
 */
data class GoogleLoginReq(
    /** ID Token emitido por el cliente de Google Sign-In, que el servidor debe validar. */
    @field:Json(name = "id_token")
    val idToken: String
)

// --- PERFIL DE USUARIO ---

/**
 * DTO que representa el perfil del usuario actual (GET /users/me).
 * Contiene los datos públicos y de control de la cuenta.
 */
data class UserDto(
    /** ID único del usuario en la base de datos. */
    val id: Int,
    /** Correo electrónico del usuario. */
    val email: String,
    /** Nombre de usuario. */
    val username: String?,
    /** Rol del usuario para control de acceso (ej. 'participant', 'admin'). */
    val role: String,
    /**
     * Proveedor de autenticación de la cuenta (ej. "local" o "google").
     * Es crucial para determinar si el usuario puede cambiar la contraseña.
     */
    @Json(name = "auth_provider") val authProvider: String
)

// --- CAMBIO DE CONTRASEÑA ---

/**
 * Petición enviada al endpoint para cambiar la contraseña (POST /users/me/change-password).
 */
data class ChangePasswordRequest(
    /** Contraseña actual del usuario (requerida por seguridad). */
    val current_password: String,
    /** La nueva contraseña deseada. */
    val new_password: String,
    /**
     * Confirmación de la nueva contraseña. Campo redundante para forzar
     * la validación de coincidencia en el backend.
     */
    val confirm_password: String
)