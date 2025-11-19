package es.ua.iuii.iaeav.data.api

import es.ua.iuii.iaeav.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * # Interfaz de la API de Autenticación y Perfil (AuthApi)
 *
 * Define los endpoints de Retrofit para todas las operaciones relacionadas con:
 * - Inicio de Sesión (Local y Google OIDC)
 * - Registro de Usuarios
 * - Obtención y Gestión del Perfil de Usuario
 *
 * Todas las rutas son relativas a la URL base configurada en [es.ua.iuii.iaeav.core.ServiceLocator].
 */
interface AuthApi {

    /**
     * Inicia sesión con credenciales locales.
     * @param body El [LoginReq] que contiene el identificador y la contraseña.
     * @return [LoginRes] Objeto que contiene el token JWT si la autenticación es exitosa.
     */
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): LoginRes

    /**
     * Registra un nuevo usuario en el sistema.
     * @param req El [RegisterReq] con el username, email y password.
     * @return [Unit] si el registro es exitoso (cuerpo de respuesta vacío).
     * @throws HttpException si la validación falla o el usuario ya existe (ej. 409 Conflict).
     */
    @POST("/api/v1/auth/register")
    suspend fun register(@Body req: RegisterReq)

    /**
     * Valida un ID Token de Google (OIDC) con el backend e inicia la sesión.
     * @param body El [GoogleLoginReq] que encapsula el ID Token.
     * @return [LoginRes] Objeto que contiene el token JWT emitido por nuestro servidor.
     */
    @POST("auth/google/token")
    suspend fun googleLogin(@Body body: GoogleLoginReq): LoginRes

    // --- ENDPOINTS DE PERFIL ---

    /**
     * Obtiene los detalles del perfil del usuario actualmente autenticado.
     * @return [UserDto] que contiene los datos de la cuenta (id, email, role, authProvider).
     */
    @GET("users/me")
    suspend fun getMe(): UserDto

    /**
     * Permite al usuario actualmente autenticado cambiar su contraseña.
     * @param request El [ChangePasswordRequest] con la contraseña actual y la nueva (incluyendo confirmación).
     * @return [Response<Unit>] Un código de respuesta 200 (OK) sin cuerpo si es exitoso.
     * @throws HttpException si el usuario es de Google (403 Forbidden) o si la contraseña actual es incorrecta (401 Unauthorized).
     */
    @POST("users/me/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}