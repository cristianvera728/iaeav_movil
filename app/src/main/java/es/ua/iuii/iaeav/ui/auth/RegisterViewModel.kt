package es.ua.iuii.iaeav.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val repo = ServiceLocator.authRepository
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val done = MutableStateFlow(false)

    fun submit(username: String, pass: String) {
        loading.value = true
        viewModelScope.launch {
            runCatching { repo.register(username, pass) }
                .onSuccess { done.value = true }
                .onFailure { error.value = it.message ?: "Error" }
            loading.value = false
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel() as T
        }
    }
}
