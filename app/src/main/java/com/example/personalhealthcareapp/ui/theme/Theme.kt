package com.example.personalhealthcareapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IntelligentMonolithColorScheme = darkColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Tertiary,
    tertiary = Tertiary,
    background = Surface,
    surface = Surface,
    surfaceVariant = SurfaceContainerLow,
    onPrimary = OnPrimary,
    onSecondary = Surface,
    onTertiary = Surface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outlineVariant = OutlineVariantGhost
)

@Composable
fun PersonalHealthCareAppTheme(
    darkTheme: Boolean = true, // We enforce intelligent monolith dark scheme
    dynamicColor: Boolean = false, // Turn off dynamic colors to keep pure design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IntelligentMonolithColorScheme,
        typography = Typography,
        content = content
    )
}