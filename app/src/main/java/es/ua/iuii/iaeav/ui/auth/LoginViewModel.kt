package es.ua.iuii.iaeav.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repo = ServiceLocator.authRepository
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun submit(username: String, pass: String, onSuccess: () -> Unit) {
        loading.value = true
        viewModelScope.launch {
            runCatching { repo.login(username, pass) }
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "Error" }
            loading.value = false
        }
    }

    // --- AÑADIR ESTA FUNCIÓN ---
    fun googleLogin(idToken: String, onSuccess: () -> Unit) {
        loading.value = true
        viewModelScope.launch {
            runCatching { repo.googleLogin(idToken) }
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "Error de Google Login" }
            loading.value = false
        }
    }
    // ---------------------------

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel() as T
        }
    }
}