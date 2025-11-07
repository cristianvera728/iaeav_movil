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

// --- Importaciones añadidas ---
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
// ------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val vm: RecordViewModel = viewModel(factory = RecordViewModel.Factory(context))

    var isRecording by remember { mutableStateOf(false) }

    // --- ¡NUEVO! Observamos el WorkInfo del ViewModel ---
    val workInfo by vm.workInfo.collectAsState()

    // --- El texto de estado ahora depende de 'isRecording' y 'workInfo' ---
    val status = remember(isRecording, workInfo) {
        when {
            isRecording -> "Grabando..."
            workInfo == null -> "Listo para grabar"
            else -> when (workInfo!!.state) {
                WorkInfo.State.ENQUEUED -> "En cola para subir..."
                WorkInfo.State.RUNNING -> "Subiendo..."
                WorkInfo.State.BLOCKED -> "Esperando red..."
                WorkInfo.State.SUCCEEDED -> {
                    // Leemos los datos de éxito
                    val snr = workInfo!!.outputData.getDouble(UploadWorker.KEY_OUTPUT_SNR, 0.0)
                    String.format(Locale.US, "Grabación Aceptada (SNR: %.1f dB)", snr)
                }
                WorkInfo.State.FAILED -> {
                    // Leemos el error
                    val reason = workInfo!!.outputData.getString(UploadWorker.KEY_OUTPUT_ERROR) ?: "Fallo"
                    if ("low_snr" in reason) "Rechazada: SNR muy bajo" else "Rechazada: $reason"
                }
                WorkInfo.State.CANCELLED -> "Subida cancelada"
            }
        }
    }
    // -----------------------------------------------------------------

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.startRecording()
            isRecording = true
        } else {
            // El estado se actualizará solo porque 'isRecording' no cambió
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nueva Grabación") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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

            LargeFloatingActionButton(
                onClick = {
                    if (!isRecording) {
                        startOrAskPermission()
                    } else {
                        vm.stopAndEnqueueUpload()
                        isRecording = false
                        // Ya no ponemos "Subida encolada" aquí, el 'workInfo' se encargará
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

            // --- La Card ahora muestra el estado dinámico ---
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