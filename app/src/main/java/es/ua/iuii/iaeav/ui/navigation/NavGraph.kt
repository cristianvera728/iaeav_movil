package es.ua.iuii.iaeav.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.ua.iuii.iaeav.ui.auth.LoginScreen
import es.ua.iuii.iaeav.ui.auth.RegisterScreen
import es.ua.iuii.iaeav.ui.record.RecordScreen

object Routes { const val Login = "login"; const val Register = "register"; const val Record = "record" }

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
        composable(Routes.Record) { RecordScreen(contentPadding = contentPadding) }
    }
}

