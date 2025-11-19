package es.ua.iuii.iaeav.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * # Pantalla de Perfil de Usuario (ProfileScreen)
 *
 * Composable que muestra la información del usuario actual y proporciona la interfaz
 * para cambiar la contraseña. La sección de cambio de contraseña se oculta
 * para usuarios que se autenticaron con Google.
 *
 * @param onBack Callback de navegación para volver a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Inicialización del ViewModel con el Factory que inyecta dependencias
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(context))

    // Colección de estados reactivos desde el ViewModel
    val user by vm.user.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()

    // Control de acceso para la visibilidad del formulario de contraseña
    val canChangePassword by vm.canChangePassword.collectAsState()

    // Estados locales para los campos del formulario de contraseña
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    // Nota: Si el DTO usa 3 campos, se requeriría un tercer estado aquí.

    // Mostrar Snackbar si hay mensajes (éxito o error)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
            // Limpiar campos si el cambio de contraseña fue un éxito
            if (it.contains("correctamente")) {
                currentPass = ""
                newPass = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // Indicador de carga inicial mientras se obtiene el perfil
        if (isLoading && user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Tarjeta de Información del Usuario ---
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user?.username ?: "Usuario",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = user?.email ?: "Cargando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestionChip(
                            onClick = { },
                            label = { Text(text = "Rol: ${user?.role ?: "Usuario"}") }
                        )
                    }
                }

                Divider()

                // --- Lógica de Renderizado Condicional de Cambio de Contraseña ---
                if (canChangePassword) {
                    // Muestra el formulario si el usuario es de tipo "local"
                    Text(
                        "Cambiar Contraseña",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Campo Contraseña Actual
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Contraseña Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Campo Nueva Contraseña
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { vm.changePassword(currentPass, newPass) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading // Deshabilitado durante la operación
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Actualizar Contraseña")
                        }
                    }
                } else {
                    // Muestra un mensaje para usuarios de Google
                    Text(
                        "Gestión de Contraseña",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Tu contraseña es gestionada por tu proveedor externo (Google). Por favor, cambia tu contraseña directamente en la configuración de tu cuenta de Google.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}