package es.ua.iuii.iaeav.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.ua.iuii.iaeav.ui.auth.LoginScreen
import es.ua.iuii.iaeav.ui.auth.RegisterScreen
import es.ua.iuii.iaeav.ui.info.InfoScreen // <-- Importar InfoScreen
import es.ua.iuii.iaeav.ui.profile.ProfileScreen
import es.ua.iuii.iaeav.ui.record.RecordScreen

object Routes {
    const val Login = "login"
    const val Register = "register"
    const val Record = "record"
    // --- NUEVAS RUTAS ---
    const val Profile = "profile"
    const val MyRecordings = "myRecordings"
    const val Info = "info"
}

@Composable
fun AppNavHost(nav: NavHostController, contentPadding: PaddingValues) {
    NavHost(navController = nav, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(
                contentPadding = contentPadding,
                onLogged = { nav.navigate(Routes.Record) { popUpTo(0) } },
                onGoRegister = { nav.navigate(Routes.Register) }
            )
        }
        composable(Routes.Register) {
            RegisterScreen(contentPadding = contentPadding) {
                // tras registro, vuelve a login
                nav.popBackStack()
            }
        }

        // --- COMPOSABLE DE RECORD ACTUALIZADO ---
        composable(Routes.Record) {
            // Ya no se pasa contentPadding
            RecordScreen(
                onLogout = {
                    // Navega a Login y borra toda la pila de navegación
                    nav.navigate(Routes.Login) { popUpTo(0) }
                },
                onNavigateToProfile = {
                    nav.navigate(Routes.Profile)
                },
                onNavigateToMyRecordings = {
                    nav.navigate(Routes.MyRecordings)
                },
                onNavigateToInfo = {
                    nav.navigate(Routes.Info)
                }
            )
        }


        composable(Routes.Info) {
            // --- CAMBIO: Usar la pantalla real ---
            InfoScreen(
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() }
            )
        }
    }

}