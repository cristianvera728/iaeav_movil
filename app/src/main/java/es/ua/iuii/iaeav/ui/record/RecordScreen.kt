package es.ua.iuii.iaeav.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

// --- Importaciones que ya tenías ---
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.work.WorkInfo
import es.ua.iuii.iaeav.workers.UploadWorker
import java.util.Locale

// --- Importaciones NUEVAS para el menú ---
import androidx.compose.material.icons.filled.MoreVert

/**
 * # Pantalla de Grabación (RecordScreen)
 *
 * Composable principal de la aplicación. Gestiona la interfaz de usuario para
 * iniciar y detener la grabación de audio, la solicitud de permisos de micrófono,
 * y la visualización del estado de la subida asíncrona mediante [WorkManager].
 *
 * @param onLogout Callback para navegar a la pantalla de Login y cerrar la sesión.
 * @param onNavigateToProfile Callback para navegar a la pantalla de perfil.
 * @param onNavigateToInfo Callback para navegar a la pantalla de información.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val context = LocalContext.current
    // Inicialización del ViewModel que orquesta la grabación y la subida
    val vm: RecordViewModel = viewModel(factory = RecordViewModel.Factory(context))

    // --- Estados Locales y de ViewModel ---

    /** Estado local que refleja si el grabador está actualmente activo. */
    var isRecording by remember { mutableStateOf(false) }

    /** Estado reactivo del [WorkInfo] para la tarea de subida, observado desde el ViewModel. */
    val workInfo by vm.workInfo.collectAsState()

    /** Estado local para controlar la visibilidad del menú desplegable (tres puntos). */
    var showMenu by remember { mutableStateOf(false) }

    // --- Lógica de Estado de la UI ---

    /**
     * Variable calculada que proporciona un mensaje descriptivo para el usuario
     * basado en el estado de grabación local y el estado del worker de subida.
     */
    val status = remember(isRecording, workInfo) {
        when {
            isRecording -> "Grabando..."
            workInfo == null -> "Listo para grabar"
            else -> when (workInfo!!.state) {
                WorkInfo.State.ENQUEUED -> "En cola para subir..."
                WorkInfo.State.RUNNING -> "Subiendo..."
                WorkInfo.State.BLOCKED -> "Esperando red..."
                WorkInfo.State.SUCCEEDED -> {
                    // Muestra el SNR (Relación Señal/Ruido) si la subida fue exitosa
                    val snr = workInfo!!.outputData.getDouble(UploadWorker.KEY_OUTPUT_SNR, 0.0)
                    String.format(Locale.US, "Grabación Aceptada (SNR: %.1f dB)", snr)
                }
                WorkInfo.State.FAILED -> {
                    // Muestra el motivo del fallo, incluyendo rechazo por SNR bajo
                    val reason = workInfo!!.outputData.getString(UploadWorker.KEY_OUTPUT_ERROR) ?: "Fallo"
                    if ("low_snr" in reason) "Rechazada: SNR muy bajo" else "Rechazada: $reason"
                }
                WorkInfo.State.CANCELLED -> "Subida cancelada"
            }
        }
    }

    // --- Lógica de Permisos ---

    /** Lanzador de actividad para solicitar el permiso RECORD_AUDIO. */
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.startRecording()
            isRecording = true
        }
    }

    /**
     * Comprueba el permiso del micrófono y lo solicita si no está concedido.
     * Si el permiso está OK, inicia la grabación.
     */
    fun startOrAskPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.startRecording()
            isRecording = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // --- Interfaz de Usuario (UI) ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grabación Segura") },
                actions = {
                    // Icono de menú (tres puntos) que controla la visibilidad de [DropdownMenu]
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }

                    // Menú desplegable con opciones de navegación y sesión
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false } // Se cierra al tocar fuera
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mi Cuenta") },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile() // Navegación al perfil
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Información de la App") },
                            onClick = {
                                showMenu = false
                                onNavigateToInfo() // Navegación a información
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                onLogout() // Cierre de sesión y navegación al login
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // Aplica el relleno de la barra superior
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                "Grabación Segura",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            // Botón Flotante Grande (FAB) de Iniciar/Detener Grabación
            LargeFloatingActionButton(
                onClick = {
                    if (!isRecording) {
                        startOrAskPermission()
                    } else {
                        vm.stopAndEnqueueUpload() // Detiene y pone la subida en cola
                        isRecording = false
                    }
                },
                // Cambia de color basado en el estado de grabación
                containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Detener grabación" else "Iniciar grabación",
                    modifier = Modifier.size(60.dp)
                )
            }

            // Tarjeta de Estado (Muestra el status)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}