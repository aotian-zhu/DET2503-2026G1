package no.dte2503.volunteer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.dte2503.volunteer.data.ThemeMode

val ArcticNavy = Color(0xFF0A2547)
val ArcticBlue = Color(0xFF125BD4)
val Glacier = Color(0xFF8CE2F1)

private val LightColors = lightColorScheme(
    primary = ArcticBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EEFF), onPrimaryContainer = Color(0xFF103C82),
    secondary = Color(0xFF087D92), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F4F7), onSecondaryContainer = Color(0xFF005360),
    tertiary = Color(0xFF387151), tertiaryContainer = Color(0xFFDFF3E6), onTertiaryContainer = Color(0xFF194B2B),
    background = Color(0xFFF3F7FC), onBackground = ArcticNavy,
    surface = Color.White, onSurface = ArcticNavy,
    surfaceVariant = Color(0xFFEAF0F8), onSurfaceVariant = Color(0xFF53677F),
    outline = Color(0xFF7E90A5), outlineVariant = Color(0xFFDCE5F0),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C8FF), onPrimary = Color(0xFF063675),
    primaryContainer = Color(0xFF153A68), onPrimaryContainer = Color(0xFFDCE9FF),
    secondary = Glacier, secondaryContainer = Color(0xFF084654), onSecondaryContainer = Color(0xFFBCECF3),
    tertiary = Color(0xFF9BD5AF), tertiaryContainer = Color(0xFF204832), onTertiaryContainer = Color(0xFFD4F3DE),
    background = Color(0xFF0B1524), onBackground = Color(0xFFE4EDFA),
    surface = Color(0xFF122238), onSurface = Color(0xFFE4EDFA),
    surfaceVariant = Color(0xFF1B2E47), onSurfaceVariant = Color(0xFFAEBDD0),
    outline = Color(0xFF7E90A5), outlineVariant = Color(0xFF293D56),
)
private val ArenaTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun VolunteerHubTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = ArenaTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp)),
        content = content,
    )
}
