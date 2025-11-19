package es.ua.iuii.iaeav.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.navigation.compose.rememberNavController
import es.ua.iuii.iaeav.core.ServiceLocator // Importación clave
import es.ua.iuii.iaeav.ui.navigation.AppNavHost
import es.ua.iuii.iaeav.ui.theme.IAEAVTheme

/**
 * # Actividad Principal (MainActivity)
 *
 * Clase principal que actúa como punto de entrada (entry point) de la aplicación Android.
 * Es responsable de:
 * 1. Inicializar el [ServiceLocator] para la inyección de dependencias.
 * 2. Albergar el grafo de navegación de Jetpack Compose ([AppNavHost]).
 */
class MainActivity : ComponentActivity() {

    // @OptIn(ExperimentalMaterial3Api::class) // La anotación ya no es necesaria en este contexto
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** * Inicialización crítica: Configura todas las dependencias singleton
         * de la aplicación (APIs, Repositorios, SecurePrefs, OkHttp).
         */
        ServiceLocator.init(applicationContext)

        setContent {
            // 1. Aplica el tema Material 3 personalizado a toda la aplicación
            IAEAVTheme {
                // 2. Inicializa el controlador de navegación y lo pasa al NavHost
                val nav = rememberNavController()

                // Muestra el grafo de navegación principal, iniciando el flujo de la UI
                AppNavHost(nav = nav, contentPadding = PaddingValues())
            }
        }
    }
}