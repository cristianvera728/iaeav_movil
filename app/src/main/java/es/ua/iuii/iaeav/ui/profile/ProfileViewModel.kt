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

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Estado del usuario
    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    // Estado de carga y errores
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ====================================================================
    // 💡 CAMBIO CLAVE: Lógica para deshabilitar el cambio de contraseña
    // ====================================================================
    /**
     * Bandera que indica si el usuario tiene una cuenta local y puede cambiar su contraseña.
     * Solo es 'true' si el campo 'authProvider' en UserDto es "local".
     */
    val canChangePassword: StateFlow<Boolean> = user.map { userDto ->
        // Comprueba si el DTO existe y si el proveedor es "local"
        userDto?.authProvider == "local"
    }.stateIn(
        scope = viewModelScope,
        // Inicia la recolección tan pronto como se necesite
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    // ====================================================================

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            // Asumo que authRepository.getUserProfile() devuelve un UserDto actualizado con 'authProvider'
            authRepository.getUserProfile()
                .onSuccess { _user.value = it }
                .onFailure { _message.value = "Error cargando perfil: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun changePassword(current: String, newPass: String) {
        // 🛡️ VALIDACIÓN TEMPRANA EN EL CLIENTE
        if (user.value?.authProvider != "local") {
            _message.value = "Error: No se permite cambiar la contraseña a usuarios de Google."
            return
        }

        if (current.isBlank() || newPass.isBlank()) {
            _message.value = "Los campos no pueden estar vacíos"
            return
        }

        // Nota: Si has corregido el DTO para incluir 'confirm_password',
        // esta función en el ViewModel debería recibir los tres parámetros.
        // Asumo que 'newPass' contiene la confirmación o que el repositorio/DTO lo maneja.
        // Si tu vista usa 3 campos, la firma de esta función debe ser:
        // fun changePassword(current: String, newPass: String, confirmPass: String)

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

    fun clearMessage() {
        _message.value = null
    }

    // Factory para inyectar el repositorio
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = ServiceLocator.authRepository
            return ProfileViewModel(repo) as T
        }
    }
}