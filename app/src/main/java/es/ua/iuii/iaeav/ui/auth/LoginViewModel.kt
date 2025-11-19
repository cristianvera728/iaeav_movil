package es.ua.iuii.iaeav.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * # ViewModel para la pantalla de Inicio de Sesión (LoginViewModel)
 *
 * Clase responsable de manejar el estado de la UI (carga, error) y orquestar
 * las llamadas a [es.ua.iuii.iaeav.data.repo.AuthRepository] para los flujos
 * de autenticación local y con Google.
 */
class LoginViewModel : ViewModel() {

    /** Referencia al Repositorio de Autenticación, obtenido a través del Service Locator. */
    private val repo = ServiceLocator.authRepository

    /**
     * Estado reactivo que indica si hay una operación de autenticación en curso.
     * Exportado como [StateFlow] para ser observado por Compose.
     */
    val loading = MutableStateFlow(false)

    /**
     * Estado reactivo que almacena el mensaje de error de la última operación.
     */
    val error = MutableStateFlow<String?>(null)

    /**
     * Intenta autenticar al usuario con credenciales locales.
     *
     * @param loginIdentifier Nombre de usuario o email.
     * @param pass Contraseña.
     * @param onSuccess Callback que se ejecuta si el login es exitoso.
     */
    fun submit(loginIdentifier: String, pass: String, onSuccess: () -> Unit) {
        loading.value = true
        viewModelScope.launch {
            // Se propaga el error (si lo hay) o se ejecuta onSuccess.
            runCatching { repo.login(loginIdentifier, pass) }
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "Error de Login" }
            loading.value = false
        }
    }

    /**
     * Intenta autenticar al usuario con un ID Token de Google (OIDC).
     *
     * Envía el token al backend para validación y creación de un JWT de sesión local.
     * @param idToken ID Token recibido del cliente de Google Sign-In.
     * @param onSuccess Callback que se ejecuta si el login es exitoso.
     */
    fun googleLogin(idToken: String, onSuccess: () -> Unit) {
        loading.value = true
        viewModelScope.launch {
            runCatching { repo.googleLogin(idToken) }
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "Error de Google Login" }
            loading.value = false
        }
    }

    /**
     * Factory estático para la creación de [LoginViewModel].
     * Utiliza el [ServiceLocator] para obtener las dependencias necesarias.
     */
    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel() as T
        }
    }
}