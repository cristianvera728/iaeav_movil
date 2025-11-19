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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    // --- Firma de la función MODIFICADA ---
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val context = LocalContext.current
    val vm: RecordViewModel = viewModel(factory = RecordViewModel.Factory(context))

    var isRecording by remember { mutableStateOf(false) }
    val workInfo by vm.workInfo.collectAsState()

    // --- Estado para el menú desplegable ---
    var showMenu by remember { mutableStateOf(false) }

    // --- Tu lógica de 'status' (sin cambios) ---
    val status = remember(isRecording, workInfo) {
        when {
            isRecording -> "Grabando..."
            workInfo == null -> "Listo para grabar"
            else -> when (workInfo!!.state) {
                WorkInfo.State.ENQUEUED -> "En cola para subir..."
                WorkInfo.State.RUNNING -> "Subiendo..."
                WorkInfo.State.BLOCKED -> "Esperando red..."
                WorkInfo.State.SUCCEEDED -> {
                    val snr = workInfo!!.outputData.getDouble(UploadWorker.KEY_OUTPUT_SNR, 0.0)
                    String.format(Locale.US, "Grabación Aceptada (SNR: %.1f dB)", snr)
                }
                WorkInfo.State.FAILED -> {
                    val reason = workInfo!!.outputData.getString(UploadWorker.KEY_OUTPUT_ERROR) ?: "Fallo"
                    if ("low_snr" in reason) "Rechazada: SNR muy bajo" else "Rechazada: $reason"
                }
                WorkInfo.State.CANCELLED -> "Subida cancelada"
            }
        }
    }

    // --- Tu lógica de permisos (sin cambios) ---
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.startRecording()
            isRecording = true
        }
    }

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

    // --- Scaffold MODIFICADO con el TopAppBar y el Menú ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grabación Segura") }, // Título actualizado
                actions = {
                    // Icono de menú (tres puntos)
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }

                    // Menú desplegable
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mi Cuenta") },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile() // Llama a la navegación
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Información de la App") },
                            onClick = {
                                showMenu = false
                                onNavigateToInfo() // Llama a la navegación
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                onLogout() // Llama al logout
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding -> // Este 'innerPadding' es el que SÍ se usa
        // --- TODO TU CONTENIDO de la columna, ahora usa el 'innerPadding' del nuevo Scaffold ---
        Column(
            modifier = Modifier
                .padding(innerPadding) // <-- Se aplica el padding del Scaffold
                .fillMaxSize()
                .padding(16.dp), // <-- Tu padding adicional
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                "Grabación Segura",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            LargeFloatingActionButton(
                onClick = {
                    if (!isRecording) {
                        startOrAskPermission()
                    } else {
                        vm.stopAndEnqueueUpload()
                        isRecording = false
                    }
                },
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