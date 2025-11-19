package es.ua.iuii.iaeav.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import es.ua.iuii.iaeav.data.model.UserDto
import es.ua.iuii.iaeav.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map // Importación necesaria
import kotlinx.coroutines.flow.stateIn // Importación necesaria
import kotlinx.coroutines.flow.SharingStarted // Importación necesaria
import kotlinx.coroutines.launch

/**
 * # ViewModel para la Pantalla de Perfil (ProfileViewModel)
 *
 * Clase responsable de:
 * 1. Cargar y exponer los datos del perfil del usuario ([UserDto]).
 * 2. Determinar si el usuario tiene permiso para cambiar la contraseña (solo cuentas "local").
 * 3. Ejecutar la lógica de negocio para el cambio de contraseña.
 *
 * @property authRepository El repositorio para realizar las operaciones de red de perfil.
 */
class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // --- Estados Reactivos Principales ---

    /** Estado mutable que contiene el objeto de perfil del usuario. */
    private val _user = MutableStateFlow<UserDto?>(null)
    /** [StateFlow] público del usuario para ser observado por la vista. */
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    /** Estado mutable que indica si una operación (ej. carga, cambio de contraseña) está activa. */
    private val _isLoading = MutableStateFlow(false)
    /** [StateFlow] público del estado de carga. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Estado mutable que contiene mensajes de notificación (éxito o error) para la Snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    /** [StateFlow] público de los mensajes. */
    val message: StateFlow<String?> = _message.asStateFlow()

    // --- Lógica de Control de Acceso (Visibilidad de UI) ---

    /**
     * Bandera reactiva que indica si el usuario tiene una cuenta local y, por lo tanto,
     * puede cambiar su contraseña.
     *
     * Este [StateFlow] se deriva del flujo [user] utilizando el operador [map].
     */
    val canChangePassword: StateFlow<Boolean> = user.map { userDto ->
        // Solo permite el cambio si el DTO existe y el proveedor de autenticación es "local"
        userDto?.authProvider == "local"
    }.stateIn(
        scope = viewModelScope,
        // Inicia la recolección al ser observado y la detiene 5 segundos después de que el último observador se vaya.
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // --- Inicialización ---

    init {
        // Se carga el perfil del usuario inmediatamente al crear el ViewModel
        fetchUserProfile()
    }

    /**
     * Realiza la llamada asíncrona al repositorio para obtener los datos del perfil del usuario actual.
     */
    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.getUserProfile()
                .onSuccess { _user.value = it }
                .onFailure { _message.value = "Error cargando perfil: ${it.message}" }
            _isLoading.value = false
        }
    }

    /**
     * Ejecuta la lógica para cambiar la contraseña del usuario.
     *
     * Incluye validación temprana en el cliente para cuentas de Google y campos vacíos.
     * @param current Contraseña actual.
     * @param newPass Nueva contraseña. (Nota: Si el DTO usa confirmación, la firma de la función debe ajustarse).
     */
    fun changePassword(current: String, newPass: String) {
        // 🛡️ VALIDACIÓN TEMPRANA: Bloquea la acción si no es una cuenta local.
        if (user.value?.authProvider != "local") {
            _message.value = "Error: No se permite cambiar la contraseña a usuarios de Google."
            return
        }

        // Validación de campos no vacíos
        if (current.isBlank() || newPass.isBlank()) {
            _message.value = "Los campos no pueden estar vacíos"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            authRepository.changePassword(current, newPass)
                .onSuccess {
                    _message.value = "¡Contraseña actualizada correctamente!"
                }
                .onFailure {
                    _message.value = "Fallo al actualizar: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Limpia el mensaje de notificación ([message]) para que la Snackbar pueda ocultarse.
     */
    fun clearMessage() {
        _message.value = null
    }

    // --- Factory ---

    /**
     * Factory estático para la creación de [ProfileViewModel] con inyección manual de dependencias.
     * Permite inyectar el repositorio sin depender de bibliotecas complejas.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Obtiene la instancia singleton de AuthRepository desde el ServiceLocator
            val repo = ServiceLocator.authRepository
            return ProfileViewModel(repo) as T
        }
    }
}