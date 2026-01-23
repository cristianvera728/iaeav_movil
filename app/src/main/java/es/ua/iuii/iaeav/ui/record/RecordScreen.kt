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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import es.ua.iuii.iaeav.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem

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
 * @param onNavigateToLoading Callback para navegar a la pantalla de carga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToLoading: () -> Unit
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

    // --- Estados para las Pruebas ---
    var currentTest by remember { mutableStateOf<TestItem?>(null) }
    var testIndex by remember { mutableStateOf(0) }
    var timeRemaining by remember { mutableStateOf(0) }
    var isShowingQuestion by remember { mutableStateOf(false) }

    // Lista de pruebas con texto, imagen y duración
    val testList = listOf(
        TestItem(
            title = "Feedback 0",
            description = "Buenos días",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 1: Memoria",
            description = "Cuénteme, cómo está su memoria. ¿Cuáles son las cosas que más le cuesta recordar?",
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta1
        ),
        TestItem(
            title = "Feedback 1",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 2: Orientación",
            description = "Por favor, dígame la fecha de hoy, que día de la semana es, en que mes estamos y en que año.",
            durationMs = 20000,
            questionAudioResId = R.raw.pregunta2
        ),
        TestItem(
            title = "Feedback 2",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 3: Reconocimiento de Imágenes",
            description = "Ahora va a ver cuatro objetos, diga el nombre de cada uno. Luego, tendrá que recordar los objetos cuando se lo pregunte.", // Son 6 objetos
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta3,
            drawableResId = R.drawable.pruebas_ejemplos
        ),
        TestItem(
            title = "Feedback 3",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 4: Nombres de Hombres",
            description = "Ahora me tiene que decir nombres propios de hombres, tiene 30 segundos para ello.",
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta4
        ),
        TestItem(
            title = "Feedback 4",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 5: Nombres de Mujeres",
            description = "Ahora en 30 segundos dígame nombres propios de mujeres.",
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta5
        ),
        TestItem(
            title = "Feedback 5",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 6: Memoria de Imágenes",
            description = "Ahora, ¿recuerda los objetos que anteriormente aparecieron? Por favor, menciónelos.",
            durationMs = 60000,
            questionAudioResId = R.raw.pregunta6
        ),
        TestItem(
            title = "Feedback 6",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 7: Análisis de Imagen",
            description = "Por último, mire detalladamente la siguiente imagen. Cuénteme con detalle lo que ve y lo que está ocurriendo.",
            durationMs = 20000,
            questionAudioResId = R.raw.pregunta7,
            drawableResId = R.drawable.imagen_accidente
        ),
        TestItem(
            title = "Feedback 7",
            description = "Muy bien",
            durationMs = 1000,
            isFeedback = true
        ),
        TestItem(
            title = "Fin de la Prueba",
            description = "Ha finalizado la prueba. Gracias por su colaboración.",
            durationMs = 4000,
            isFeedback = true
        )
    )

    // ExoPlayer para reproducir audio de preguntas
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Detener audio si se detiene la grabación
    LaunchedEffect(isRecording) {
        if (!isRecording && exoPlayer.isPlaying) {
            exoPlayer.stop()
            isShowingQuestion = false
        }
    }

    // Lógica para avanzar entre pruebas con temporizador
    LaunchedEffect(testIndex, isRecording) {
        if (!isRecording || testIndex >= testList.size) return@LaunchedEffect

        val test = testList[testIndex]

        // Reproduce audio de la pregunta si existe
        if (test.questionAudioResId != null && !test.isFeedback) {
            isShowingQuestion = true
            currentTest = test
            vm.pauseRecording()

            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            val mediaItem = MediaItem.fromUri(
                "android.resource://${context.packageName}/${test.questionAudioResId}"
            )
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            
            // Esperar a que el reproductor esté listo
            while (!exoPlayer.isCommandAvailable(ExoPlayer.COMMAND_PLAY_PAUSE)) {
                delay(100)
            }
            
            exoPlayer.play()
            
            // Esperar más tiempo a que comience la reproducción y el audio esté realmente sonando
            delay(1000)

            // Esperar a que termine el audio
            while (exoPlayer.isPlaying) {
                delay(200)
            }

            isShowingQuestion = false
            vm.resumeRecording()
        } else {
            // Para feedback o pruebas sin audio, asignar directamente
            currentTest = test
        }

        // Después empieza el tiempo de respuesta
        if (!test.isFeedback) {
            val totalSeconds = test.durationMs / 1000
            for (second in totalSeconds downTo 0) {
                timeRemaining = second
                delay(1_000)
            }
        } else {
            // Para feedback, simplemente esperar el tiempo especificado
            delay(test.durationMs.toLong())
        }

        //Pasar a la siguiente prueba
        testIndex++
        
        // Si se acabaron todas las pruebas, detener la grabación automáticamente y navegar a la pantalla de carga
        if (testIndex >= testList.size) {
            vm.stopAndEnqueueUpload()
            isRecording = false
            currentTest = null
            testIndex = 0
            
            onNavigateToLoading()
        }
    }


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
                title = { Text("Grabación segura") },
                // Colores personalizados para la barra superior
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
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
                        //
                        DropdownMenuItem(
                            text = { Text("Ir a Pantalla de Carga") },
                            onClick = {
                                showMenu = false
                                onNavigateToLoading() // Navegación directa a carga
                            }
                        )
                        //
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
            verticalArrangement = Arrangement.Top
        ) {
            // Mostrar tiempo restante arriba si esta grabando, hay pregunta (no feedback) y no se está mostrando la pregunta
            AnimatedVisibility(
                visible = isRecording && currentTest != null && !currentTest!!.isFeedback && !isShowingQuestion,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(1500)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(1500)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tiempo restante: ${timeRemaining}s",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            AnimatedContent(
                targetState = currentTest,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(600)) togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(600))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { test ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isRecording) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Para iniciar la prueba, pulse el botón de micrófono.",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LargeFloatingActionButton(
                                onClick = {
                                    testIndex = 0
                                    currentTest = null
                                    startOrAskPermission()
                                },
                                modifier = Modifier.size(120.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Iniciar grabación",
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    } else if (isRecording && test != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (test.isFeedback) MaterialTheme.colorScheme.background
                                else CardDefaults.cardColors().containerColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Pregunta de la prueba
                                Text(
                                    text = test.description,
                                    style = if (test.isFeedback) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Imagen asociada a la prueba, si existe
                                if (test?.drawableResId != null) {
                                    Image(
                                        painter = painterResource(id = test!!.drawableResId!!),
                                        contentDescription = test!!.title,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botón Flotante Grande (FAB) de Iniciar/Detener Grabación - Visible durante grabación
            if (isRecording) {
                LargeFloatingActionButton(
                    onClick = {
                        if (!isRecording) {
                            testIndex = 0 // Reinicia las pruebas
                            currentTest = null
                            startOrAskPermission()
                        } else {
                            vm.stopAndEnqueueUpload() // Detiene y pone la subida en cola
                            isRecording = false
                            currentTest = null
                            testIndex = 0
                        }
                    },
                    // Cambia de color basado en el estado de grabación
                    containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    modifier = if (isRecording) Modifier.size(90.dp) else Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Detener grabación" else "Iniciar grabación",
                        modifier = if (isRecording) Modifier.size(50.dp) else Modifier.size(80.dp)
                    )
                }
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

// Data class para las pruebas
data class TestItem(
    val title: String,
    val description: String,
    val durationMs: Int, // Tiempo de respuesta
    val drawableResId: Int? = null,
    val isFeedback: Boolean = false,
    val questionAudioResId: Int? = null
)