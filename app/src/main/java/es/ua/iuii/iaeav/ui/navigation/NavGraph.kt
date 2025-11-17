package es.ua.iuii.iaeav.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.ua.iuii.iaeav.ui.auth.LoginScreen
import es.ua.iuii.iaeav.ui.auth.RegisterScreen
import es.ua.iuii.iaeav.ui.profile.ProfileScreen
import es.ua.iuii.iaeav.ui.record.RecordScreen
import es.ua.iuii.iaeav.ui.recordings.MyRecordingsScreen

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

        // --- COMPOSABLES PARA LAS NUEVAS PANTALLAS (PLACEHOLDERS) ---

        composable(Routes.Profile) {
            // Aquí iría tu ProfileScreen(contentPadding)
            Column(modifier = Modifier.padding(contentPadding)) {
                Text("Pantalla de Mi Cuenta / Perfil")
            }
        }

        composable(Routes.MyRecordings) {
            MyRecordingsScreen(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.Info) {
            // Aquí iría tu InfoScreen(contentPadding)
            Column(modifier = Modifier.padding(contentPadding)) {
                Text("Pantalla de Información de la App")
            }
        }

        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() }
            )
        }
    }

}