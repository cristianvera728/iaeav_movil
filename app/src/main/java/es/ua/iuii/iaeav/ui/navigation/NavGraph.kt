package es.ua.iuii.iaeav.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.ua.iuii.iaeav.ui.auth.LoginScreen
import es.ua.iuii.iaeav.ui.auth.RegisterScreen
import es.ua.iuii.iaeav.ui.info.InfoScreen
import es.ua.iuii.iaeav.ui.profile.ProfileScreen
import es.ua.iuii.iaeav.ui.record.RecordScreen

/**
 * Define las rutas de navegación utilizadas en la aplicación.
 * El uso de un 'object' garantiza que los nombres de las rutas sean constantes y seguros.
 */
object Routes {
    const val Login = "login"
    const val Register = "register"
    const val Record = "record"
    const val Profile = "profile"
    const val Info = "info"
}

/**
 * # Contenedor de Navegación de la Aplicación (AppNavHost)
 *
 * Define el grafo de navegación (NavGraph) de la aplicación utilizando [NavHost].
 * Controla la pila de vistas y las transiciones entre pantallas.
 *
 * @param nav El controlador de navegación ([NavHostController]) proporcionado por la [MainActivity].
 * @param contentPadding El relleno (padding) de la barra inferior o del Scaffold principal.
 */
@Composable
fun AppNavHost(nav: NavHostController, contentPadding: PaddingValues) {
    NavHost(navController = nav, startDestination = Routes.Login) {

        // --- Ruta de Inicio de Sesión ---
        composable(Routes.Login) {
            LoginScreen(
                contentPadding = contentPadding,
                // Al iniciar sesión, navega a 'Record' y elimina todas las rutas anteriores (popUpTo(0))
                onLogged = { nav.navigate(Routes.Record) { popUpTo(0) } },
                onGoRegister = { nav.navigate(Routes.Register) }
            )
        }

        // --- Ruta de Registro ---
        composable(Routes.Register) {
            RegisterScreen(contentPadding = contentPadding) {
                // Tras un registro exitoso, vuelve a la pantalla anterior (Login)
                nav.popBackStack()
            }
        }

        // --- Ruta Principal (Grabación) ---
        composable(Routes.Record) {
            // RecordScreen contiene el control de navegación para las acciones de la barra superior.
            RecordScreen(
                onLogout = {
                    // Al cerrar sesión, navega a Login y borra toda la pila de navegación por seguridad
                    nav.navigate(Routes.Login) { popUpTo(0) }
                },
                onNavigateToProfile = {
                    nav.navigate(Routes.Profile)
                },
                onNavigateToInfo = {
                    nav.navigate(Routes.Info)
                }
            )
        }

        // --- Ruta de Información ---
        composable(Routes.Info) {
            InfoScreen(
                onBack = { nav.popBackStack() },
            )
        }

        // --- Ruta de Perfil ---
        composable(Routes.Profile) {
            // ProfileScreen gestiona la información del usuario y el cambio de contraseña.
            ProfileScreen(
                onBack = { nav.popBackStack() }
            )
        }
    }
}