package es.ua.iuii.iaeav.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ua.iuii.iaeav.R

/**
 * # Pantalla de Información (InfoScreen)
 *
 * Composable que muestra la información estática de la aplicación,
 * detalles del desarrollador y enlaces a recursos externos (ej. política de privacidad).
 *
 * @param onBack Callback de navegación para volver a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    /**
     * Función de ayuda para abrir una URL en un navegador externo.
     * @param url La dirección web a abrir.
     */
    val openUrl = { url: String ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Información") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Permite el scroll si el contenido excede la pantalla
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo y versión de la aplicación
            Image(
                painter = painterResource(id = R.drawable.logo_iaeav),
                contentDescription = "Logo IAEAV",
                modifier = Modifier
                    .size(120.dp)
                    .padding(top = 16.dp)
            )

            Text(
                text = "IAEAV Voice App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "Versión 1.0.0 (Build 1)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección "Acerca de"
            SectionTitle("Acerca de esta App")
            Text(
                text = "Plataforma de Inteligencia Artificial para la Detección Temprana de la Enfermedad de Alzheimer a través de la Voz (IAEAV). Su objetivo es la recopilación segura y cifrada de muestras de voz para el entrenamiento de modelos de inteligencia artificial.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección "Desarrollo"
            SectionTitle("Desarrollado por")
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instituto Universitario de Investigación en Informática (IUII-UA)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Plataforma de Inteligencia Artificial para la Detección Temprana de la Enfermedad de Alzheimer a través de la Voz (IAEAV)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección "Enlaces"
            SectionTitle("Legal y Soporte")
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // Enlace a Política de Privacidad
                InfoLink(
                    text = "Política de Privacidad",
                    icon = Icons.Default.Policy,
                    onClick = {
                        openUrl("https://www.ua.es/es/aviso-legal-web/politica-de-privacidad.html") // URL de ejemplo
                    }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                // Enlace para Contactar con Soporte (abre el cliente de email)
                InfoLink(
                    text = "Contactar con Soporte",
                    icon = Icons.Default.Support,
                    onClick = {
                        // Crea un intent para enviar un correo electrónico con campos pre-rellenados
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:") // Solo apps de email
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("soporte.iaeav@ua.es")) // Email de ejemplo
                            putExtra(Intent.EXTRA_SUBJECT, "Soporte App IAEAV")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                // Enlace a la web de la plataforma
                InfoLink(
                    text = "Visitar web IAEAV",
                    icon = Icons.AutoMirrored.Filled.Launch,
                    onClick = {
                        openUrl("https://iaeav.lucentia.es") // URL real
                    }
                )
            }
        }
    }
}

/**
 * Composable de ayuda para mostrar un título de sección.
 * @param title Texto del título.
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

/**
 * Composable de ayuda para mostrar un elemento clickable con icono y flecha.
 *
 * Utiliza [ListItem] de Material3 con un efecto 'ripple' explícito.
 * @param text Texto principal del enlace.
 * @param icon Icono de material para el inicio del elemento.
 * @param onClick Acción a realizar al hacer clic (ej. abrir URL o Intent).
 */
@Composable
private fun InfoLink(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            // Aplicar el efecto ripple de Material3
            indication = androidx.compose.material3.ripple(),
            onClick = onClick
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = Color.Gray
            )
        }
    )
}