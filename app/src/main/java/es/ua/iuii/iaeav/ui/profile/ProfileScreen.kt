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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(context))

    val user by vm.user.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()

    // 💡 CAMBIO CLAVE: Recoger el nuevo estado de control de acceso
    val canChangePassword by vm.canChangePassword.collectAsState()

    // Estados locales para el formulario
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    // NOTA: Si corregiste el DTO a 3 campos, deberías tener aquí 'confirmPass'
    // y usarlo en la llamada a vm.changePassword. Mantenemos el código actual
    // por simplicidad.

    // Mostrar Snackbar si hay mensajes
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
            // Limpiar campos si fue éxito
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

                // =========================================================
                // 💡 CAMBIO CLAVE: Renderizado Condicional
                // =========================================================
                if (canChangePassword) {
                    Text(
                        "Cambiar Contraseña",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // --- Formulario de Cambio de Contraseña ---
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Contraseña Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Asegúrate de añadir el tercer campo si lo implementaste

                    Button(
                        onClick = { vm.changePassword(currentPass, newPass) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
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
                    // Mostrar un mensaje claro en lugar del formulario
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
                // =========================================================
            }
        }
    }
}