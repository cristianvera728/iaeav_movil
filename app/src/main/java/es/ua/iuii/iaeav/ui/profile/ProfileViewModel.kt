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

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.getUserProfile()
                .onSuccess { _user.value = it }
                .onFailure { _message.value = "Error cargando perfil: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun changePassword(current: String, newPass: String) {
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

    fun clearMessage() {
        _message.value = null
    }

    // Factory para inyectar el repositorio
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
            // Accedemos directamente a la propiedad, asumiendo que ServiceLocator.init() ya se ha llamado
            val repo = ServiceLocator.authRepository
            return ProfileViewModel(repo) as T
        }
    }
}