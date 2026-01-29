package es.ua.iuii.iaeav.ui.loading

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import es.ua.iuii.iaeav.core.ServiceLocator
import retrofit2.HttpException
import androidx.lifecycle.LiveData


class LoadingViewModel(
    private val recordingId: String
) : ViewModel() {

    private val repo = ServiceLocator.recordingRepository

    private val _uiState = MutableLiveData<LoadingUiState>(LoadingUiState.Loading)
    val uiState: LiveData<LoadingUiState> = _uiState

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        // Cancelar polling previo si existe
        pollingJob?.cancel()
        
        _uiState.value = LoadingUiState.Loading

        pollingJob = viewModelScope.launch {
            var delayMs = 1_000L
            val maxDelay = 5_000L
            val timeoutMs = 120_000L
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < timeoutMs) {
                try {
                    val rec = repo.getRecordingById(recordingId)

                    _uiState.value = LoadingUiState.InProgress(
                        analysisStatus = rec.analysisStatus
                    )

                    when (rec.analysisStatus) {
                        "completed" -> {
                            _uiState.value = LoadingUiState.Completed
                            return@launch
                        }

                        "failed" -> {
                            _uiState.value = LoadingUiState.Failed(
                                message = "El análisis ha fallado"
                            )
                            return@launch
                        }
                    }

                } catch (e: HttpException) {
                    when (e.code()) {
                        401 -> {
                            // Token inválido o expirado. No debería de ocurrir desde la aplicación móvil puesto que siempre manda autenticación válida
                            _uiState.value = LoadingUiState.Unauthorized
                            return@launch
                        }

                        404 -> {
                            delay(500L) // Esperar un poco antes de notificar
                            _uiState.value = LoadingUiState.Failed(
                                message = "Grabación no encontrada (404)"
                            )
                            return@launch
                        }

                        else -> {
                            _uiState.value = LoadingUiState.Failed(
                                message = "Error del servidor (${e.code()})"
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = LoadingUiState.Failed(
                        message = "Error de red: ${e.localizedMessage ?: "Desconocido"}"
                    )
                    return@launch
                }

                delay(delayMs)
                delayMs = minOf(maxDelay, (delayMs * 1.5).toLong())
            }

            // Si llegamos aquí, se agotó el timeout
            _uiState.value = LoadingUiState.Timeout
        }
    }

    fun retry() {
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

sealed class LoadingUiState {
    object Loading : LoadingUiState()

    data class InProgress(
        val analysisStatus: String
    ) : LoadingUiState()

    object Completed : LoadingUiState()

    data class Failed(
        val message: String
    ) : LoadingUiState()

    object Unauthorized : LoadingUiState()

    object Timeout : LoadingUiState()
}