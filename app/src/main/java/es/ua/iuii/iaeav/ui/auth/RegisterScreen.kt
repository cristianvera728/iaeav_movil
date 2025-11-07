package es.ua.iuii.iaeav.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// --- Importaciones añadidas ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import es.ua.iuii.iaeav.R
// ------------------------------

@Composable
fun RegisterScreen(contentPadding: PaddingValues, onRegistered: () -> Unit) {
    val vm: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
    var username by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val loading by vm.loading.collectAsState()
    val err by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    LaunchedEffect(done) { if (done) onRegistered() }

    // 1. Columna centrada que ocupa toda la pantalla (igual que Login)
    Column(
        modifier = Modifier
            .padding(contentPadding) // Mantenemos el padding original
            .fillMaxSize()           // Ocupa toda la pantalla
            .padding(16.dp),         // Añade un padding interno
        verticalArrangement = Arrangement.Center, // Centra verticalmente
        horizontalAlignment = Alignment.CenterHorizontally // Centra horizontalmente
    ) {

        // --- Logo añadido ---
        Image(
            painter = painterResource(id = R.drawable.logo_iaeav),
            contentDescription = "Logo de la App",
            modifier = Modifier.size(120.dp) // Puedes ajustar el tamaño
        )
        Spacer(Modifier.height(24.dp))
        // -----------------------

        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium) // Título más grande
        Spacer(Modifier.height(24.dp))

        // 2. Campos de texto mejorados (igual que Login)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho
            leadingIcon = { // Añade un icono
                Icon(Icons.Default.Person, contentDescription = "Icono de usuario")
            }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho
            leadingIcon = { // Añade un icono
                Icon(Icons.Default.Lock, contentDescription = "Icono de contraseña")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Teclado de contraseña
            visualTransformation = PasswordVisualTransformation() // Oculta la contraseña
        )
        Spacer(Modifier.height(20.dp))

        // 3. Botón mejorado (igual que Login)
        Button(
            enabled = !loading,
            onClick = { vm.submit(username, pass) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho
        ) {
            Text(if (loading) "Creando..." else "Registrar")
        }

        // Texto de error
        if (err != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Error: $err",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}