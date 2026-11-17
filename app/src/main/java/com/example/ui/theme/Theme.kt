package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val PulsoDarkColorScheme = darkColorScheme(
    primary = PulsoEmerald,
    onPrimary = Color(0xFF002116),
    primaryContainer = PulsoEmeraldContainerDark,
    onPrimaryContainer = Color(0xFF79F8C7),
    secondary = PulsoTealAccent,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = PulsoLiveCoral,
    onTertiary = Color.White,
    tertiaryContainer = PulsoLiveCoralContainer,
    onTertiaryContainer = Color(0xFFFFD9E0),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1F3338)
)

private val PulsoLightColorScheme = lightColorScheme(
    primary = PulsoEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = PulsoEmeraldContainerLight,
    onPrimaryContainer = Color(0xFF00281B),
    secondary = PulsoTealAccent,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = PulsoLiveCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4EA),
    onTertiaryContainer = Color(0xFF4A0016),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFDCEAE6)
)

val PulsoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulsoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PulsoDarkColorScheme
        else -> PulsoLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulsoTypography,
        shapes = PulsoShapes,
        content = content
    )
}
