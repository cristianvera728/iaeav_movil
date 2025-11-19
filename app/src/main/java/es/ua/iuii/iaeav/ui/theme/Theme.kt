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

/**
 * Esquema de colores para el tema oscuro.
 * Utiliza los colores definidos en [Color.kt] (PrimaryBlueLight, SecondaryBlueLight, TertiaryAquaLight).
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    secondary = SecondaryBlueLight,
    tertiary = TertiaryAquaLight
)

/**
 * Esquema de colores para el tema claro.
 * Utiliza los colores definidos en [Color.kt] (PrimaryBlue, SecondaryBlue, TertiaryAqua).
 */
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

/**
 * # Composable Principal del Tema (IAEAVTheme)
 *
 * Aplica el tema de diseño de Material 3 a toda la jerarquía de Composable.
 *
 * @param darkTheme Indica si el tema oscuro debe estar activo (por defecto, sigue la configuración del sistema).
 * @param dynamicColor Si es 'true' (y el dispositivo lo soporta - Android 12+), utiliza colores derivados del fondo de pantalla.
 * @param content El contenido de la aplicación al que se aplicará el tema.
 */
@Composable
fun IAEAVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 1. Prioridad: Color Dinámico (si está activado y el SO es compatible)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 2. Si no hay color dinámico, usa el esquema manual de tema oscuro
        darkTheme -> DarkColorScheme

        // 3. Por defecto, usa el esquema manual de tema claro
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 'Typography' viene del archivo Type.kt
        content = content
    )
}