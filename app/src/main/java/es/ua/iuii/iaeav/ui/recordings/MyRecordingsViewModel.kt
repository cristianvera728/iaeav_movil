package es.ua.iuii.iaeav.ui.recordings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import es.ua.iuii.iaeav.data.model.RecordingDto
import es.ua.iuii.iaeav.data.repo.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyRecordingsViewModel(private val recordingRepository: RecordingRepository) : ViewModel() {

    // Estado de la lista de grabaciones
    private val _recordings = MutableStateFlow<List<RecordingDto>>(emptyList())
    val recordings: StateFlow<List<RecordingDto>> = _recordings.asStateFlow()

    // Estado de carga y errores
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        fetchMyRecordings()
    }

    private fun fetchMyRecordings() {
        viewModelScope.launch {
            _isLoading.value = true
            recordingRepository.getMyRecordings()
                .onSuccess {
                    // Ordenamos por fecha, de más nueva a más antigua
                    _recordings.value = it.sortedByDescending { it.created_at }
                }
                .onFailure {
                    _message.value = "Error cargando grabaciones: ${it.message}"
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
            // Usamos recordingRepository
            val repo = ServiceLocator.recordingRepository
            return MyRecordingsViewModel(repo) as T
        }
    }
}