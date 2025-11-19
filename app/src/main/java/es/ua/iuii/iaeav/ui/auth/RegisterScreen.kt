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
import androidx.compose.material.icons.filled.Email // Icono para el campo de email
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock // Icono para el campo de contraseña
import androidx.compose.material.icons.filled.Person // Icono para el campo de usuario
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import es.ua.iuii.iaeav.R
// ------------------------------

/**
 * # Pantalla de Registro (RegisterScreen)
 *
 * Composable que presenta el formulario para que un nuevo usuario cree una cuenta.
 *
 * Gestiona los estados locales del formulario, observa el estado de la lógica de negocio
 * a través de [RegisterViewModel] (loading, error, done), y coordina la llamada al proceso de registro.
 *
 * @param contentPadding Relleno (padding) aplicado por el Scaffold contenedor.
 * @param onRegistered Callback de navegación que se ejecuta cuando el registro es exitoso.
 */
@Composable
fun RegisterScreen(contentPadding: PaddingValues, onRegistered: () -> Unit) {
    // Inicialización del ViewModel
    val vm: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)

    // Estados del formulario local
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    // Estados reactivos del ViewModel
    val loading by vm.loading.collectAsState()
    val err by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    /**
     * Efecto secundario que se lanza cuando el estado 'done' del ViewModel cambia a true.
     * Dispara la navegación a la siguiente pantalla (onRegistered).
     */
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
        /** Muestra el logo de la aplicación. */
        Image(
            painter = painterResource(id = R.drawable.logo_iaeav),
            contentDescription = "Logo de la App",
            modifier = Modifier.size(120.dp) // Puedes ajustar el tamaño
        )
        Spacer(Modifier.height(24.dp))
        // -----------------------

        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium) // Título más grande
        Spacer(Modifier.height(24.dp))

        // Campo: Usuario
        /** Campo de texto para introducir el nombre de usuario. */
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

        // Campo: Correo Electrónico
        /** Campo de texto para introducir la dirección de correo electrónico. */
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Icono de correo")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email) // Optimización para email
        )
        Spacer(Modifier.height(8.dp))

        // Campo: Contraseña
        /** Campo de texto para introducir la contraseña. Utiliza PasswordVisualTransformation para ocultar la entrada. */
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

        // Botón de Registro
        /**
         * Botón de acción principal. Se deshabilita mientras el ViewModel está en estado de carga.
         * Al hacer clic, llama a [RegisterViewModel.submit] con los datos del formulario.
         */
        Button(
            enabled = !loading,
            onClick = { vm.submit(username, email, pass) }, // <-- EMAIL INCLUIDO EN LA LLAMADA
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho
        ) {
            Text(if (loading) "Creando..." else "Registrar")
        }

        // Texto de error
        /** Muestra un mensaje de error si el ViewModel ha reportado un fallo en el proceso de registro. */
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