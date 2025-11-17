package es.ua.iuii.iaeav.ui.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ua.iuii.iaeav.data.model.RecordingDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecordingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: MyRecordingsViewModel = viewModel(factory = MyRecordingsViewModel.Factory(context))

    val recordings by vm.recordings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Grabaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isLoading && recordings.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (recordings.isEmpty()) {
                Text(
                    text = "Aún no tienes grabaciones subidas.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recordings) { recording ->
                        RecordingListItem(recording = recording)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingListItem(recording: RecordingDto) {
    val statusColor: Color
    val statusIcon: ImageVector
    val statusText: String

    when (recording.status.uppercase()) {
        "ACCEPTED" -> {
            statusColor = Color(0xFF388E3C) // Verde
            statusIcon = Icons.Default.CheckCircle
            statusText = "Aceptada"
        }
        "REJECTED_SNR" -> {
            statusColor = MaterialTheme.colorScheme.error
            statusIcon = Icons.Default.Error
            statusText = "Rechazada (SNR bajo)"
        }
        else -> { // PENDING o cualquier otro
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
            statusIcon = Icons.Default.Schedule
            statusText = "Pendiente"
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatFriendlyDate(recording.created_at),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recording.filename ?: "Grabación ID: ${recording.id.take(8)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Mostrar SNR si está disponible
            recording.snr?.let { snr ->
                Text(
                    text = String.format(Locale.US, "%.1f dB", snr),
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper para formatear la fecha
private fun formatFriendlyDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        localDateTime.format(formatter)
    } catch (e: Exception) {
        isoDate // Devuelve el string original si falla el parseo
    }
}