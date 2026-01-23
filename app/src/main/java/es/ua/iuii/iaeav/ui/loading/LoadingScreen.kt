package es.ua.iuii.iaeav.ui.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

/**
 * # Pantalla de Carga (LoadingScreen)
 *
 * Composable que muestra una pantalla de carga con un indicador de progreso circular
 * mientras el modelo analiza la grabación de audio y genera un diagnóstico.
 * 
 * @param isLoading Estado de carga. Cuando es false, navega a la pantalla de resultados.
 * @param onNavigateToResult Callback para navegar a la pantalla de resultados cuando se complete la carga.
 */

@Composable
fun LoadingScreen(
    isLoading: Boolean = true,
    onNavigateToResult: () -> Unit = {}
) {
    // Bloquea el botón atrás mientras esté cargando
    BackHandler(enabled = isLoading) { }

    // Navegación automática cuando termina la carga
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onNavigateToResult()
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
                text = "Por favor espere mientras se analiza su grabación y se obtiene un diagnóstico",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 
            Button(
                onClick = onNavigateToResult
            ) {
                Text("Ver resultados")
            }
            //
        }
    }
}