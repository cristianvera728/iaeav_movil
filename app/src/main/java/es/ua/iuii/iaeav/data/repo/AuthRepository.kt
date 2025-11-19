package es.ua.iuii.iaeav.data.repo

import es.ua.iuii.iaeav.core.ServiceLocator.authApi
import es.ua.iuii.iaeav.core.storage.SecurePrefs
import es.ua.iuii.iaeav.data.api.AuthApi
import es.ua.iuii.iaeav.data.model.ChangePasswordRequest
import es.ua.iuii.iaeav.data.model.GoogleLoginReq
import es.ua.iuii.iaeav.data.model.LoginReq
import es.ua.iuii.iaeav.data.model.RegisterReq
import es.ua.iuii.iaeav.data.model.UserDto
import es.ua.iuii.iaeav.ui.navigation.Routes.Register

/**
 * # Repositorio de Autenticación (AuthRepository)
 *
 * Clase responsable de orquestar la lógica de autenticación de usuarios.
 * Maneja las peticiones de red a la [AuthApi] y la gestión del [SecurePrefs]
 * para almacenar los tokens de sesión.
 *
 * @property api Interfaz de Retrofit para interactuar con los endpoints de autenticación.
 * @property sp Almacenamiento seguro para guardar el token JWT del usuario.
 */
class AuthRepository(
    private val api: AuthApi,
    private val sp: SecurePrefs
) {
    /**
     * Inicia sesión con credenciales locales (usuario/email y contraseña).
     *
     * Si la petición es exitosa, guarda el token JWT en [SecurePrefs].
     * @param loginIdentifier Nombre de usuario o correo electrónico del usuario.
     * @param pass Contraseña en texto plano.
     * @throws Exception Si el login falla (ej. credenciales inválidas, error de red).
     */
    suspend fun login(loginIdentifier: String, pass: String) {
        val res = api.login(LoginReq(loginIdentifier, pass))
        sp.saveJwt(res.jwt())
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param username Nombre de usuario deseado.
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @throws Exception Si el registro falla (ej. usuario ya existe, validación).
     */
    suspend fun register(username: String, email: String, password: String): Unit {
        // Enviar el nuevo RegisterReq al ApiClient/AuthApi
        authApi.register(RegisterReq(username, email, password))
    }

    /**
     * Inicia sesión usando el flujo de Google (Google Sign-In).
     *
     * Valida el ID Token de Google con el backend y, si es exitoso,
     * guarda el JWT emitido por nuestro servidor en [SecurePrefs].
     * @param idToken ID Token emitido por el cliente de Google.
     * @throws Exception Si la validación o el proceso falla.
     */
    suspend fun googleLogin(idToken: String) {
        val res = api.googleLogin(GoogleLoginReq(idToken))
        sp.saveJwt(res.jwt())
    }

    /**
     * Obtiene la información del perfil del usuario actualmente autenticado (GET /users/me).
     *
     * @return [Result] que contiene el [UserDto] si es exitoso, o una excepción si falla.
     */
    suspend fun getUserProfile(): Result<UserDto> {
        return try {
            val user = api.getMe()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permite al usuario actualmente autenticado cambiar su contraseña.
     *
     * **NOTA:** Esta operación es bloqueada por el servidor para usuarios de Google.
     * @param current Contraseña actual del usuario.
     * @param newPass La nueva contraseña deseada (asumo que incluye la confirmación en el DTO).
     * @return [Result.success] si el cambio es exitoso, [Result.failure] en caso contrario.
     */
    suspend fun changePassword(current: String, newPass: String): Result<Unit> {
        return try {
            // Nota: Se asume que el DTO 'ChangePasswordRequest' ya fue corregido
            // para incluir los tres campos (current, newPass, confirmPass) si es necesario.
            val request = ChangePasswordRequest(
                current, newPass,
                confirm_password =newPass
            )
            val response = api.changePassword(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // Captura errores de la API (ej. 401 si la contraseña actual es incorrecta)
                Result.failure(Exception("Error al cambiar contraseña: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}