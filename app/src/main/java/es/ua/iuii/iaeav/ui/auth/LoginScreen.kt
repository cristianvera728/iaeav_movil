package es.ua.iuii.iaeav.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import es.ua.iuii.iaeav.R
import kotlinx.coroutines.launch


// ----------------------------------------------
// !! IMPORTANTE !!
// REEMPLAZA ESTO CON TU ID DE CLIENTE WEB DE GOOGLE CLOUD
// (El que es de tipo "Aplicación Web")
// ----------------------------------------------
/**
 * ID de Cliente Web de Google Cloud. Es vital para solicitar el ID Token
 * necesario para la autenticación OIDC con el backend.
 */
private const val WEB_CLIENT_ID = "935818959464-v44nfgs5vs5o4ivr1bcct7t75frk1vio.apps.googleusercontent.com"

/**
 * Pantalla principal de inicio de sesión.
 *
 * Muestra el formulario de credenciales locales y el botón de Google Sign-In.
 *
 * @param contentPadding Relleno (padding) aplicado por el Scaffold contenedor.
 * @param onLogged Callback para navegar una vez que el usuario inicia sesión exitosamente.
 * @param onGoRegister Callback para navegar a la pantalla de registro.
 */
@Composable
fun LoginScreen(contentPadding: PaddingValues, onLogged: () -> Unit, onGoRegister: () -> Unit) {
    // Inicialización del ViewModel
    val vm: LoginViewModel = viewModel(factory = LoginViewModel.Factory)

    // Estados del formulario local
    var loginIdentifier by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    // Estados reactivos del ViewModel
    val loading by vm.loading.collectAsState()
    val err by vm.error.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- LÓGICA DE GOOGLE SIGN-IN ---

    // 1. Configurar el cliente de Google Sign-In
    val googleSignInClient = remember {
        // Opciones requeridas: solicitar ID Token y el email del usuario
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // 2. Crear el lanzador para el resultado de la actividad de Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // El usuario seleccionó la cuenta exitosamente
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task, vm, onLogged) {
                // En caso de error en nuestro backend, cerramos sesión en Google
                coroutineScope.launch {
                    googleSignInClient.signOut()
                }
            }
        } else {
            // Error o cancelación en la ventana de Google
            vm.error.value = "Inicio con Google cancelado"
        }
    }
    // --------------------------------

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Logo de la aplicación
        Image(
            painter = painterResource(id = R.drawable.logo_iaeav),
            contentDescription = "Logo de la App",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(24.dp))

        Text("Inicia sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        // --- Login con Usuario/Contraseña ---
        OutlinedTextField(
            value = loginIdentifier,
            onValueChange = { loginIdentifier = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "Icono de usuario")
            }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Icono de contraseña")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(20.dp))

        // Botón principal de Login local
        Button(
            enabled = !loading,
            onClick = { vm.submit(loginIdentifier, pass, onLogged) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Entrando..." else "Entrar")
        }

        // Botón para ir a registro
        TextButton(
            onClick = onGoRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }

        // --- Separador y Botón de Google ---
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(" O ", modifier = Modifier.padding(horizontal = 8.dp))
            Divider(modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        // Botón de Google Sign-In
        OutlinedButton(
            onClick = {
                vm.error.value = null // Limpiar errores anteriores
                // Lanza la actividad que muestra las cuentas de Google
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icono de Google
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = "Logo de Google",
                modifier = Modifier.size(20.dp)
            )

            // Separador y texto
            Spacer(Modifier.width(8.dp))
            Text("Continuar con Google")
        }

        // Muestra el mensaje de error si existe
        if (err != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Error: $err",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Función auxiliar para manejar el resultado de la actividad de Google Sign-In.
 *
 * @param completedTask Tarea que contiene la cuenta de Google.
 * @param vm [LoginViewModel] para llamar al login en el backend.
 * @param onSuccess Callback de navegación en caso de éxito completo.
 * @param onBackendError Callback para cerrar sesión de Google si la validación del backend falla.
 */
private fun handleGoogleSignInResult(
    completedTask: Task<GoogleSignInAccount>,
    vm: LoginViewModel,
    onSuccess: () -> Unit,
    onBackendError: () -> Unit
) {
    try {
        val account = completedTask.getResult(ApiException::class.java)
        val idToken = account.idToken // El ID Token es lo que se valida contra el backend

        if (idToken != null) {
            // El token se obtuvo correctamente, se envía al ViewModel para su validación en el servidor
            vm.googleLogin(idToken, onSuccess)
        } else {
            // Fallo en la obtención del token tras seleccionar la cuenta
            vm.error.value = "No se pudo obtener el token de Google."
        }
    } catch (e: ApiException) {
        // Fallo en el flujo de Google (ej. problemas de configuración o red)
        Log.w("LoginScreen", "signInResult:failed code=" + e.statusCode)
        vm.error.value = "Error de Google: ${e.statusCode}"
        onBackendError()
    }
}