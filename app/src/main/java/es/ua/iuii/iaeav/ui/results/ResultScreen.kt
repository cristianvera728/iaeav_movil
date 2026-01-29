package es.ua.iuii.iaeav.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider

/**
 * # Pantalla de Resultados (ResultScreen)
 *
 * Composable que muestra los resultados de la grabación tras completarse
 * el procesamiento en la pantalla de carga.
 *
 * @param onBack Callback para volver atrás al registro.
 * @param onLogout Callback para navegar a la pantalla de Login y cerrar la sesión.
 * @param onNavigateToProfile Callback para navegar a la pantalla de perfil.
 * @param onNavigateToInfo Callback para navegar a la pantalla de información.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    recordingId: String
) {
    val context = LocalContext.current

    val vm: ResultViewModel = viewModel(
        factory = ResultViewModelFactory(recordingId)
    )

    val uiState by vm.uiState.collectAsState()

    // Cargar datos al entrar
    LaunchedEffect(recordingId) {
        vm.loadRecordingDetails(recordingId)
    }

    var showMenu by remember { mutableStateOf(false) }

    val explicability by remember { mutableStateOf("El modelo ha analizado características acústicas y prosódicas del audio, como la fluidez del habla, pausas, entonación y ritmo. Además, ha tenido en cuenta patrones lingüísticos en la transcripción, como la complejidad sintáctica y el vocabulario utilizado. Estas características son indicativas de posibles deterioros cognitivos asociados con el Alzheimer.") }

    var transcriptionExpanded by remember { mutableStateOf(false) }
    var explainabilityExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados de la prueba") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (uiState is ResultUiState.Success) {
                        val success = uiState as ResultUiState.Success

                        IconButton(onClick = {
                            generarPDFResultados(
                                context = context,
                                prediction = success.predictionResult,
                                confidence = success.predictionConfidence,
                                transcription = success.predictionTranscription,
                                explicability = explicability
                            )
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Descargar PDF")
                        }
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mi Cuenta") },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Información de la App") },
                            onClick = {
                                showMenu = false
                                onNavigateToInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        when (uiState) {

            ResultUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ResultUiState.Error -> {
                val msg = (uiState as ResultUiState.Error).message

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is ResultUiState.Success -> {
                val data = uiState as ResultUiState.Success

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "El modelo considera que el paciente ${ if (data.predictionResult == "positive") "padece de Alzheimer" else "no padece de Alzheimer" } con una confianza del ${data.predictionConfidence}",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Transcripción",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExpandableCard(
                        content = data.predictionTranscription,
                        expanded = transcriptionExpanded,
                        onToggle = { transcriptionExpanded = !transcriptionExpanded }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Explicabilidad",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExpandableCard(
                        content = explicability,
                        expanded = explainabilityExpanded,
                        onToggle = { explainabilityExpanded = !explainabilityExpanded }
                    )
                }
            }
        }
    }
}

// Factory para ResultViewModel con parámetro
class ResultViewModelFactory(
    private val recordingId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            return ResultViewModel(recordingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}