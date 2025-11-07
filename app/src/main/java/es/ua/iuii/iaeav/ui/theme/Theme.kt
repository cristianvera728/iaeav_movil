package es.ua.iuii.iaeav.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 1. Usa los nuevos colores del logo para el TEMA OSCURO
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    secondary = SecondaryBlueLight,
    tertiary = TertiaryAquaLight
)

// 2. Usa los nuevos colores del logo para el TEMA CLARO
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = TertiaryAqua

    /* Puedes definir más colores aquí si los necesitas:
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun IAEAVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // El color dinámico (Android 12+) usará los colores del wallpaper.
    // Ponlo en 'false' si quieres FORZAR siempre tus colores azules.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 'Typography' viene de tu archivo Type.kt
        content = content
    )
}