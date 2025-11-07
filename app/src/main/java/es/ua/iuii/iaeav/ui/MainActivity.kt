package es.ua.iuii.iaeav.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.navigation.compose.rememberNavController
import es.ua.iuii.iaeav.core.ServiceLocator
import es.ua.iuii.iaeav.ui.navigation.AppNavHost
import es.ua.iuii.iaeav.ui.theme.IAEAVTheme

class MainActivity : ComponentActivity() {
    // @OptIn(ExperimentalMaterial3Api::class) // Ya no es necesario aquí
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)

        setContent {
            // 1. Llama directamente a TU tema personalizado
            IAEAVTheme {
                // 2. Pon el NavController y el AppNavHost DENTRO del tema
                val nav = rememberNavController()
                AppNavHost(nav = nav, contentPadding = PaddingValues())
            }
        }
    }
}
