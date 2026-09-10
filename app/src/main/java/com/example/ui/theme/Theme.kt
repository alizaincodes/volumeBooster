package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = Color.White,
    primaryContainer = ElectricIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = IndigoGrey80,
    onSecondary = Color(0xFF1E1A2C),
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    error = RiskRed,
    onError = Color.White,
    background = Color(0xFF131218),
    onBackground = Color(0xFFE5E1EC),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE5E1EC),
    surfaceVariant = Color(0xFF24222D),
    onSurfaceVariant = Color(0xFFC8C4D4)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = Color.White,
    primaryContainer = ElectricIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = IndigoGrey80,
    onSecondary = Color.Black,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    error = RiskRed,
    onError = Color.White,
    background = AmoledBackground,
    onBackground = Color.White,
    surface = AmoledSurface,
    onSurface = Color.White,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFFDCD8E8)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF),
    onPrimaryContainer = ElectricIndigoDark,
    secondary = IndigoGrey40,
    onSecondary = Color.White,
    tertiary = AmberWarningDark,
    onTertiary = Color.White,
    error = RiskRed,
    onError = Color.White,
    background = Color(0xFFF9F9FD),
    onBackground = Color(0xFF1A1920),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1920),
    surfaceVariant = Color(0xFFE6E3EE),
    onSurfaceVariant = Color(0xFF474455)
)

@Composable
fun VolumeBoostTheme(
    themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK, AMOLED
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK", "AMOLED" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }
    val isAmoled = themeMode == "AMOLED"

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAmoled -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isAmoled -> AmoledDarkColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
