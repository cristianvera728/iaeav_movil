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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import es.ua.iuii.iaeav.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha

//---------- Debug de subir wav
/* 
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import java.io.File
import java.io.InputStream
*/
//----------

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
    onNavigateToLoading: (String) -> Unit
) {
    val context = LocalContext.current
    // Inicialización del ViewModel que orquesta la grabación y la subida
    val vm: RecordViewModel = viewModel(factory = RecordViewModel.Factory(context))

    // --- Estados Locales y de ViewModel ---

    /** Estado local que refleja si el grabador está actualmente activo. */
    var isRecording by remember { mutableStateOf(false) }

    /** Estado local de audio de pregunta en reproducción. */
    var audioPlaying by remember { mutableStateOf(false) }

    /** Estado reactivo del [WorkInfo] para la tarea de subida, observado desde el ViewModel. */
    val workInfo by vm.workInfo.collectAsState()

    /** Estado local para controlar la visibilidad del menú desplegable (tres puntos). */
    var showMenu by remember { mutableStateOf(false) }

    /** Estado local para controlar cuando está parado el audio durante la prueba */
    var isAudioStopped by remember { mutableStateOf(false) }

    /** Estado para controlar la visibilidad del diálogo de cancelación. */
    var showCancelDialog by remember { mutableStateOf(false) }

    /** Estado para controlar la visibilidad del diálogo de siguiente pregunta. */
    var showNextQuestionDialog by remember { mutableStateOf(false) }

    /** Estado para detener el tiempo y el audio durante el AlertDialog. */
    var isStoppingTimeAndAudio by remember { mutableStateOf(false) }

    /** Estado para almacenar el ID de la grabación subida. */
    var recordingId by remember { mutableStateOf<String?>(null) }


    // --- Estados para las Pruebas ---
    var currentTest by remember { mutableStateOf<TestItem?>(null) }
    var testIndex by remember { mutableStateOf(0) }
    var timeRemaining by remember { mutableStateOf(0) }
    var isShowingQuestion by remember { mutableStateOf(false) }

    // Estado para el índice de imagen actual (para pruebas con múltiples imágenes)
    var currentImageIndex by remember { mutableStateOf(0) }
    // Lista de drawables para la prueba 3
    val test3Images = listOf(
        R.drawable.pruebas_objetos_1,
        R.drawable.pruebas_objetos_2,
        R.drawable.pruebas_objetos_3,
        R.drawable.pruebas_objetos_4,
        R.drawable.pruebas_objetos_5,
        R.drawable.pruebas_objetos_6
    )

    // Lista de pruebas con texto, imagen y duración
    val testList = listOf(
        TestItem(
            title = "Feedback 0",
            description = "Buenos días",
            durationMs = 1500,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 1: Memoria",
            description = "Cuénteme, cómo está su memoria.\n¿Cuáles son las cosas que más le cuesta recordar?",
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta1
        ),
        TestItem(
            title = "Feedback 1",
            description = "Muy bien",
            durationMs = 1500,
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
            durationMs = 1500,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 3: Reconocimiento de Imágenes",
            description = "Ahora va a ver seis objetos, diga el nombre de cada uno.\nLuego, tendrá que recordar los objetos cuando se lo pregunte.",
            durationMs = 30000,
            questionAudioResId = R.raw.pregunta3,
            drawableResIds = test3Images
        ),
        TestItem(
            title = "Feedback 3",
            description = "Muy bien",
            durationMs = 1500,
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
            durationMs = 1500,
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
            durationMs = 1500,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 6: Memoria de Imágenes",
            description = "Ahora, ¿recuerda los objetos que anteriormente aparecieron?\nPor favor, menciónelos.",
            durationMs = 60000,
            questionAudioResId = R.raw.pregunta6,
            drawableResIds = listOf(R.drawable.recordar_objetos)
        ),
        TestItem(
            title = "Feedback 6",
            description = "Muy bien",
            durationMs = 1500,
            isFeedback = true
        ),
        TestItem(
            title = "Prueba 7: Análisis de Imagen",
            description = "Por último, mire detalladamente la siguiente imagen.\nCuénteme con detalle lo que ve y lo que está ocurriendo.",
            durationMs = 20000,
            questionAudioResId = R.raw.pregunta7,
            drawableResIds = listOf(R.drawable.imagen_accidente)
        ),
        TestItem(
            title = "Feedback 7",
            description = "Muy bien",
            durationMs = 1500,
            isFeedback = true
        ),
        TestItem(
            title = "Fin de la Prueba",
            description = "Ha finalizado la prueba.\nGracias por su colaboración.",
            durationMs = 5000,
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
        if (!isRecording || testIndex >= testList.size ) return@LaunchedEffect

        val test = testList[testIndex]

        // Reproduce audio de la pregunta si existe
        if (test.questionAudioResId != null && !test.isFeedback) {
            isShowingQuestion = true
            currentTest = test
            vm.pauseRecording()
            isAudioStopped = true
            audioPlaying = true

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
            delay(1000)

            // Esperar a que termine el audio
            while (exoPlayer.isPlaying) {
                delay(200)
            }

            isShowingQuestion = false
            vm.resumeRecording()
            isAudioStopped = false
            audioPlaying = false
        } else {
            currentTest = test
        }

        // Resetear índice de imagen al inicio de cada prueba
        currentImageIndex = 0

        // Tiempo de respuesta
        if (!test.isFeedback) {
            val totalSeconds = test.durationMs / 1000
            
            // Si hay múltiples imágenes, cambiar cada 5 segundos
            val images = test.drawableResIds
            if (images != null && images.size > 1) {
                val imagesPerSecond = images.size.toFloat() / totalSeconds
                for (second in totalSeconds downTo 0) {
                    // Esperar si hay un diálogo abierto
                    while (isStoppingTimeAndAudio) {
                        delay(100)
                    }
                    timeRemaining = second
                    
                    // Calcular índice de imagen basado en el tiempo
                    val newImageIndex = ((totalSeconds - second) * imagesPerSecond).toInt().coerceIn(0, images.size - 1)
                    currentImageIndex = newImageIndex
                    
                    delay(1_000)
                }
            } else {
                for (second in totalSeconds downTo 0) {
                    // Esperar si hay un diálogo abierto
                    while (isStoppingTimeAndAudio) {
                        delay(100)
                    }
                    timeRemaining = second
                    delay(1_000)
                }
            }
        } else {
            delay(test.durationMs.toLong())
        }

        // Pasar a la siguiente prueba
        testIndex++

        // Si terminaron las pruebas
        if (testIndex >= testList.size) {
            vm.stopAndEnqueueUpload()
            isRecording = false
        }
    }

    // Navegar a la pantalla de carga si la subida fue exitosa o a la de grabación si falló
    LaunchedEffect(workInfo) {
        workInfo?.let { info ->
            if (info.state == WorkInfo.State.SUCCEEDED) {

                val recordingId =
                    info.outputData.getString(UploadWorker.KEY_OUTPUT_RECORDING_ID)

                onNavigateToLoading(recordingId ?: "unknown")

                delay(1000)
                currentTest = null
                testIndex = 0

            } else if (info.state == WorkInfo.State.FAILED) {
                currentTest = null
                testIndex = 0
                recordingId = null
            }
        }
    }

    // --- Lógica de Estado de la UI ---

    /**
     * Variable calculada que proporciona un mensaje descriptivo para el usuario
     * basado en el estado de grabación local y el estado del worker de subida.
     */
    val status = remember(isRecording, workInfo, isAudioStopped) {
        when {
            isRecording && !isAudioStopped -> "Grabando..."
            isRecording && isAudioStopped -> "Grabación pausada"
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

    // Animación de parpadeo para el texto "Grabando..."
    var blinkTarget by remember { mutableStateOf(1f) }
    val blinkingAlpha by animateFloatAsState(
        targetValue = blinkTarget,
        animationSpec = tween(durationMillis = 800),
        label = "blinking"
    )
    
    LaunchedEffect(status) {
        if (status == "Grabando...") {
            while (true) {
                blinkTarget = 0.3f
                delay(800)
                blinkTarget = 1f
                delay(800)
            }
        } else {
            blinkTarget = 1f
        }
    }

    // Selector de archivo WAV (modo debug)
    /*val pickWavLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Copiar el WAV seleccionado a cache (WorkManager necesita File real)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val dir = File(context.cacheDir, "imports").apply { mkdirs() }
                val outFile = File(dir, "import-${System.currentTimeMillis()}.wav")

                inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        input?.copyTo(output)
                    }
                }

                // Encolar subida usando el mismo flujo normal
                vm.enqueueUploadFromWavFile(outFile)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/


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
                                onNavigateToLoading("0b7d6d1cb0cb") // ad621c97da0e
                            }
                        )
                        //DropdownMenuItem(
                        //    text = { Text("DEBUG: Subir audio WAV") },
                          //  onClick = {
                            //    showMenu = false
                              //  pickWavLauncher.launch(
                                //    arrayOf(
                                  //      "audio/wav",
                                    //    "audio/x-wav",
                                      //  "audio/*"
                                 //   )
                            //    )
                         //   }
                      //  )
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
            // Indicador de grabación (punto rojo parpadeante) - encima del timer (no mostrar durante feedback)
            AnimatedVisibility(
                visible = isRecording && currentTest != null && !currentTest!!.isFeedback,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Punto rojo parpadeante
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .alpha(blinkingAlpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Texto "Grabando..." o "Grabación pausada"
                    Text(
                        text = if (isAudioStopped) "Grabación pausada" else "Grabando...",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.alpha(blinkingAlpha)
                    )
                }
            }

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
                    if (!isRecording && testIndex == 0) {
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
                                    textAlign = if (test.isFeedback) TextAlign.Center else TextAlign.Justify,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Imagen asociada a la prueba, si existe
                                val images = test.drawableResIds
                                if (images != null && images.isNotEmpty()) {
                                    // Usar AnimatedContent para transiciones suaves entre imágenes
                                    AnimatedContent(
                                        targetState = currentImageIndex,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(500)) togetherWith
                                            fadeOut(animationSpec = tween(500))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { imageIndex ->
                                        Image(
                                            painter = painterResource(id = images[imageIndex]),
                                            contentDescription = "${test.title} - Imagen ${imageIndex + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Botones en línea - Botón Flotante Grande (FAB) de Cancelar Grabación y Pasar Pregunta - Solo visibles durante grabación
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón de Cancelar
                    LargeFloatingActionButton(
                        onClick = {
                            showCancelDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Cancelar grabación",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Botón de Pasar pregunta
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!audioPlaying) { // Verificar que no haya audio sonando
                                showNextQuestionDialog = true
                            }
                        },
                        //containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.height(60.dp),
                    ) {
                        Text(
                            text = "Siguiente pregunta",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Pasar pregunta",
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            // Tarjeta de Estado (Solo muestra estados de subida cuando NO está grabando)
            if (!isRecording) {
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

    // Diálogo de confirmación para cancelar
    if (showCancelDialog) {
        LaunchedEffect(showCancelDialog) {
            isStoppingTimeAndAudio = true
        }
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
                isStoppingTimeAndAudio = false
            },
            title = { Text("Cancelar prueba") },
            text = { Text("Si da a Aceptar la prueba se cancelará y volverá a la pantalla inicial.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        isStoppingTimeAndAudio = false
                        isRecording = false
                        currentTest = null
                        testIndex = 0
                        exoPlayer.stop()
                        vm.cancelRecording()
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        isStoppingTimeAndAudio = false
                    }
                ) {
                    Text("Continuar prueba")
                }
            }
        )
    }

    // Diálogo de confirmación para siguiente pregunta
    if (showNextQuestionDialog) {
        LaunchedEffect(showNextQuestionDialog) {
            isStoppingTimeAndAudio = true
        }
        AlertDialog(
            onDismissRequest = {
                showNextQuestionDialog = false
                isStoppingTimeAndAudio = false
            },
            title = { Text("Siguiente pregunta") },
            text = { Text("Si da a Aceptar pasará a la siguiente pregunta, si no ha terminado de responder pulse Cancelar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNextQuestionDialog = false
                        isStoppingTimeAndAudio = false
                        testIndex++
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNextQuestionDialog = false
                        isStoppingTimeAndAudio = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Data class para las pruebas
data class TestItem(
    val title: String,
    val description: String,
    val durationMs: Int, // Tiempo de respuesta
    val drawableResIds: List<Int>? = null, // Lista de imágenes (para pruebas con múltiples imágenes)
    val isFeedback: Boolean = false,
    val questionAudioResId: Int? = null
)