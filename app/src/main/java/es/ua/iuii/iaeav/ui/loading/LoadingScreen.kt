package es.ua.iuii.iaeav.ui.loading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun LoadingScreen(
    recordingId: String,
    onNavigateToResult: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val vm: LoadingViewModel = viewModel(
        factory = LoadingViewModelFactory(recordingId)
    )

    val state by vm.uiState.observeAsState(LoadingUiState.Loading)

    // Bloquea botón atrás mientras está cargando o en progreso
    BackHandler(
        enabled = state is LoadingUiState.Loading ||
                  state is LoadingUiState.InProgress
    ) { }

    // Navegación reactiva
    LaunchedEffect(state) {
        when (state) {
            LoadingUiState.Completed -> onNavigateToResult(recordingId)
            LoadingUiState.Unauthorized -> onNavigateToLogin()
            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (state) {

                is LoadingUiState.Loading,
                is LoadingUiState.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 12.dp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Analizando grabación...",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Por favor espere mientras se analiza su grabación",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is LoadingUiState.Failed -> {
                    val msg = (state as LoadingUiState.Failed).message

                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = msg,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { vm.retry() }) {
                        Text("Reintentar",
                            style = MaterialTheme.typography.headlineSmall
                            )
                    }
                }

                LoadingUiState.Timeout -> {
                    Text(
                        text = "El análisis está tardando más de lo esperado",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { vm.retry() }) {
                        Text(
                            text = "Reintentar",
                            style = MaterialTheme.typography.bodyMedium
                            )
                    }
                }

                LoadingUiState.Unauthorized -> {
                    Text(
                        text = "Sesión caducada",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                LoadingUiState.Completed -> {
                    // No se muestra nada, navega automáticamente
                }
            }
        }
    }
}

// Factory para LoadingViewModel con parámetro
class LoadingViewModelFactory(
    private val recordingId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
            return LoadingViewModel(recordingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}