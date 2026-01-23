package es.ua.iuii.iaeav.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ua.iuii.iaeav.ui.results.ExpandableCard


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
    onNavigateToInfo: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf(true) }
    val predictionText by remember { mutableStateOf(if (prediction) "padece de Alzheimer" else "no padece de Alzheimer") }
    val confidence by remember { mutableStateOf(0.80f) }
    val confidencePercentage by remember { mutableStateOf((confidence * 100).toInt()) }
    val results by remember { mutableStateOf("El modelo considera que el paciente $predictionText con una confianza del $confidencePercentage%") }
    val transcription by remember {
        mutableStateOf(
            """
            Bueno… mi memoria ya no es como antes. A veces se me olvidan cosas recientes, lo que hice ayer o con quién hablé. 
            Los nombres me cuestan mucho, y a veces entro a una habitación y no sé para qué iba. Las cosas de cuando era joven 
            las recuerdo mejor, pero lo de ahora se me mezcla.

            A ver… hoy creo que estamos a… no estoy muy seguro… diría que es martes… o miércoles. El mes… puede que sea marzo… 
            no, febrero… no sé bien. El año… 2020 y algo… 2022 quizá. Perdón, se me confunde.

            Las fotos… veo una manzana, un perro, un coche y una casa. Sí, creo que eso era.

            Nombres de hombres… a ver… Juan, José, Manuel, Antonio… también estaba Paco, como un vecino que tuve.

            Nombres de mujeres… María, Carmen, Ana, Rosa… mi hermana se llamaba Pilar… creo.

            De las fotos de antes me acuerdo de… el perro seguro… y la manzana… había algo más… una casa, sí… 
            el coche no sé si estaba o lo estoy inventando.

            En el dibujo… veo una casa por dentro. Hay una señora en la cocina, parece que está fregando o cocinando. 
            Un niño está cerca, como jugando o pidiendo algo. Creo que hay agua en el suelo, como si se hubiera derramado, 
            y la señora no se da cuenta. Todo parece un poco desordenado, como que algo está pasando pero nadie se da cuenta del todo. 
            Me cuesta verlo bien, pero es lo que alcanzo a entender.
            """.trimIndent()
        )
    }

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
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = results,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Transcripción",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Transcripción de las respuestas del paciente.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExpandableCard(
                content = transcription,
                expanded = transcriptionExpanded,
                onToggle = { transcriptionExpanded = !transcriptionExpanded }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Explicabilidad",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = " Explicación de en que se basa el modelo para generar su resultados.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExpandableCard(
                content = explicability,
                expanded = explainabilityExpanded,
                onToggle = { explainabilityExpanded = !explainabilityExpanded }
            )
        }
    }
}