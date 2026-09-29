package no.dte2503.volunteer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VolunteerColors = lightColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF19345F),
    secondary = Color(0xFF6F8FC7),
    onSecondary = Color.White,
    tertiary = Color(0xFF7D9BCB),
    background = Color.White,
    onBackground = Color(0xFF1F2933),
    surface = Color.White,
    onSurface = Color(0xFF1F2933),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D5DD),
)

@Composable
fun VolunteerHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VolunteerColors, content = content)
}
