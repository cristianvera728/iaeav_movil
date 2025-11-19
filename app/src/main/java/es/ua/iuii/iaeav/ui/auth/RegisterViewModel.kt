package es.ua.iuii.iaeav.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * # ViewModel para la pantalla de Registro (RegisterViewModel)
 *
 * Clase responsable de manejar el estado de la UI durante el proceso de registro
 * de un nuevo usuario, comunicándose con el [es.ua.iuii.iaeav.data.repo.AuthRepository].
 */
class RegisterViewModel : ViewModel() {

    /** Referencia al Repositorio de Autenticación, obtenida a través del Service Locator. */
    private val repo = ServiceLocator.authRepository

    /** Estado reactivo que indica si el proceso de registro está en curso. */
    val loading = MutableStateFlow(false)

    /** Estado reactivo que almacena el mensaje de error de la última operación de registro. */
    val error = MutableStateFlow<String?>(null)

    /** * Estado reactivo que indica si el registro ha finalizado exitosamente.
     * Se usa para disparar la navegación en la vista.
     */
    val done = MutableStateFlow(false)

    /**
     * Intenta registrar un nuevo usuario en el sistema.
     *
     * @param username Nombre de usuario deseado.
     * @param email Correo electrónico del usuario.
     * @param pass Contraseña para la nueva cuenta.
     */
    fun submit(username: String, email: String, pass: String) {
        loading.value = true
        viewModelScope.launch {
            // Se realiza la llamada asíncrona al repositorio para registrar
            runCatching { repo.register(username, email, pass) }
                .onSuccess { done.value = true } // Éxito: marca como completado
                .onFailure { error.value = it.message ?: "Error desconocido al registrar" }
            loading.value = false
        }
    }

    /**
     * Factory estático para la creación de [RegisterViewModel].
     * Utiliza el [ServiceLocator] para obtener las dependencias necesarias.
     */
    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel() as T
        }
    }
}