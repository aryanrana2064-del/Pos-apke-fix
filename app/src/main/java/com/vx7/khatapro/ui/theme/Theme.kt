package com.vx7.khatapro.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Tighter, more rectilinear corners than Material's bubbly defaults —
// reads as a ledger/finance app rather than a generic rounded-everything template.
val KhataShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = KhataPrimaryDark,
    onPrimary = KhataOnPrimaryDark,
    primaryContainer = KhataPrimaryContainerDark,
    onPrimaryContainer = KhataOnPrimaryContainerDark,
    secondary = KhataSecondaryDark,
    onSecondary = KhataOnSecondaryDark,
    secondaryContainer = KhataSecondaryContainerDark,
    onSecondaryContainer = KhataOnSecondaryContainerDark,
    tertiary = KhataTertiaryDark,
    onTertiary = KhataOnTertiaryDark,
    tertiaryContainer = KhataTertiaryContainerDark,
    onTertiaryContainer = KhataOnTertiaryContainerDark,
    error = KhataErrorDark,
    onError = KhataOnErrorDark,
    errorContainer = KhataErrorContainerDark,
    onErrorContainer = KhataOnErrorContainerDark,
    background = KhataBackgroundDark,
    onBackground = KhataOnBackgroundDark,
    surface = KhataSurfaceDark,
    onSurface = KhataOnSurfaceDark,
    surfaceVariant = KhataSurfaceVariantDark,
    onSurfaceVariant = KhataOnSurfaceVariantDark,
    outline = KhataOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = KhataPrimaryLight,
    onPrimary = KhataOnPrimaryLight,
    primaryContainer = KhataPrimaryContainerLight,
    onPrimaryContainer = KhataOnPrimaryContainerLight,
    secondary = KhataSecondaryLight,
    onSecondary = KhataOnSecondaryLight,
    secondaryContainer = KhataSecondaryContainerLight,
    onSecondaryContainer = KhataOnSecondaryContainerLight,
    tertiary = KhataTertiaryLight,
    onTertiary = KhataOnTertiaryLight,
    tertiaryContainer = KhataTertiaryContainerLight,
    onTertiaryContainer = KhataOnTertiaryContainerLight,
    error = KhataErrorLight,
    onError = KhataOnErrorLight,
    errorContainer = KhataErrorContainerLight,
    onErrorContainer = KhataOnErrorContainerLight,
    background = KhataBackgroundLight,
    onBackground = KhataOnBackgroundLight,
    surface = KhataSurfaceLight,
    onSurface = KhataOnSurfaceLight,
    surfaceVariant = KhataSurfaceVariantLight,
    onSurfaceVariant = KhataOnSurfaceVariantLight,
    outline = KhataOutlineLight
)

@Composable
fun KhataProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded colors consistent
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
        shapes = KhataShapes,
        content = content
    )
}
