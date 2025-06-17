package ru.ridecorder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Темная тема
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1E88E5),    // Глубокий синий
    secondary = Color(0xFF37474F),  // Графитовый серый
    tertiary = Color(0xFFFB8C00),   // Оранжевый акцент
    background = Color(0xFF121212), // Очень тёмный фон
    surface = Color(0xFF1E1E1E),    // Тёмная поверхность
    onPrimary = Color.White,        // Текст и элементы на синем фоне
    onSecondary = Color.White,      // Текст и элементы на графитовом фоне
    onTertiary = Color.Black,       // Текст на оранжевом фоне
    onBackground = Color(0xFFE0E0E0), // Текст на тёмном фоне
    onSurface = Color(0xFFE0E0E0),   // Текст на поверхности

    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF90CAF9),

    secondaryContainer = Color(0xFF2B343A),
    onSecondaryContainer = Color.White,

    tertiaryContainer = Color(0xFFFFB74D),
    onTertiaryContainer = Color.Black,

    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFE0E0E0),
    surfaceTint = Color(0xFF1E88E5),

    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),

    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF2E2E2E),
    surfaceContainerHighest = Color(0xFF3A3A3A),
    surfaceContainerLow = Color(0xFF131313),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceDim = Color(0xFF121212)
)

// Светлая тема
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),    // Глубокий синий
    secondary = Color(0xFF8E9EAC),  // Светлый серый/графитовый
    tertiary = Color(0xFFFFC107),   // Светлый жёлто-оранжевый акцент
    background = Color(0xFFFFFBFE), // Светлый фон
    surface = Color(0xFFFFFFFF),    // Белая поверхность
    onPrimary = Color.White,        // Текст и элементы на синем фоне
    onSecondary = Color.Black,      // Текст и элементы на светло-сером фоне
    onTertiary = Color.Black,       // Текст на светлом жёлто-оранжевом фоне
    onBackground = Color(0xFF1C1B1F), // Тёмный текст на светлом фоне
    onSurface = Color(0xFF1C1B1F),  // Тёмный текст на белой поверхности

    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color.Black,
    inversePrimary = Color(0xFF1E88E5),

    secondaryContainer = Color(0xFFB0BEC5),
    onSecondaryContainer = Color.Black,

    tertiaryContainer = Color(0xFFFFECB3),
    onTertiaryContainer = Color.Black,

    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),
    surfaceTint = Color(0xFF1E88E5),

    inverseSurface = Color(0xFF2F2F2F),
    inverseOnSurface = Color(0xFFF5F5F5),

    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),

    outline = Color(0xFF737373),
    outlineVariant = Color(0xFFC6C6C6),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF4F4F4),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFDCDCDC),
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEAEAEA)
)

@Composable
fun RidecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
        typography = Typography,
        content = content
    )
}