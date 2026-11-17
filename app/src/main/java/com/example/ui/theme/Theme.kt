package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val PInkDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanOnContainer,
    secondary = PinkAccent,
    onSecondary = PinkOnAccent,
    secondaryContainer = PinkContainer,
    onSecondaryContainer = PinkOnContainer,
    tertiary = EmeraldDone,
    background = ObsidianBg,
    onBackground = IceWhite,
    surface = SlateSurface,
    onSurface = IceWhite,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CoolSlate,
    outline = BorderSubtle,
    error = RoseError,
    errorContainer = RoseErrorContainer,
    onErrorContainer = RoseOnErrorContainer,
)

private val PInkShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun PInkTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PInkDarkColorScheme,
        typography = PInkTypography,
        shapes = PInkShapes,
        content = content,
    )
}
