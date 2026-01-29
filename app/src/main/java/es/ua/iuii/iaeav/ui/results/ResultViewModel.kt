package es.ua.iuii.iaeav.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ua.iuii.iaeav.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados de la UI para ResultScreen
 */
sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Success(
        val predictionResult: String,
        val predictionConfidence: String, // Ya formateado como porcentaje
        val predictionTranscription: String
    ) : ResultUiState
    data class Error(val message: String) : ResultUiState
}

/**
 * ViewModel para la pantalla de resultados
 */
class ResultViewModel(
    private val recordingId: String
) : ViewModel() {
    private val repo = ServiceLocator.recordingRepository

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    /**
     * Carga los detalles de una grabación por su ID
     */
    fun loadRecordingDetails(recordingId: String) {
        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading

            try {
                val recording = repo.getRecordingById(recordingId)

                _uiState.value = ResultUiState.Success(
                    predictionResult = recording.predictionResult ?: "Desconocido",
                    predictionConfidence = formatConfidence(recording.predictionConfidence),
                    predictionTranscription = recording.predictionTranscription?.takeIf { it.isNotBlank() }
                        ?: "No disponible"
                )
            } catch (e: Exception) {
                _uiState.value = ResultUiState.Error(
                    message = e.message ?: "Error al cargar los detalles de la grabación"
                )
            }
        }
    }

    /**
     * Formatea la confianza de la predicción a porcentaje
     */
    private fun formatConfidence(confidence: Double?): String {
        return if (confidence != null) {
            "${(confidence * 100).toInt()}%"
        } else {
            "N/A"
        }
    }
}