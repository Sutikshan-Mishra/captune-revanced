package dev.hd450btnc.companion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** CapTune palette: deep navy-charcoal layers, cool-white text, one medium blue accent used sparingly. */
@Immutable
data class StudioColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHigh: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val isDark: Boolean,
)

val DarkStudio = StudioColors(
    background = Color(0xFF0A0D12),
    surface = Color(0xFF10151C),
    surfaceRaised = Color(0xFF161C25),
    surfaceHigh = Color(0xFF1E2631),
    border = Color(0xFF222B37),
    borderStrong = Color(0xFF34404F),
    text = Color(0xFFE8EDF4),
    textMuted = Color(0xFF98A4B4),
    textFaint = Color(0xFF7E8A9C),
    accent = Color(0xFF4C9AFF),
    accentSoft = Color(0x264C9AFF),
    onAccent = Color(0xFF04101F),
    warning = Color(0xFFF2B84B),
    warningSoft = Color(0x1FF2B84B),
    error = Color(0xFFFF7A70),
    isDark = true,
)

val LightStudio = StudioColors(
    background = Color(0xFFF2F4F7),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF7F9FB),
    surfaceHigh = Color(0xFFE8ECF1),
    border = Color(0xFFDCE2E9),
    borderStrong = Color(0xFFC2CBD6),
    text = Color(0xFF0F141A),
    textMuted = Color(0xFF4E5A68),
    textFaint = Color(0xFF636E7C),
    accent = Color(0xFF1F5FCC),
    accentSoft = Color(0x1A1F5FCC),
    onAccent = Color(0xFFFFFFFF),
    warning = Color(0xFF8F5B00),
    warningSoft = Color(0x1A8F5B00),
    error = Color(0xFFB3261E),
    isDark = false,
)

object StudioType {
    private val sans = FontFamily.SansSerif
    val mono = FontFamily.Monospace

    val display = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp)
    val title = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp)
    val heading = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val label = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)
    val overline = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp)
    val monoSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    val monoValue = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp)
    val monoLarge = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
}

object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val screen = 20.dp
}

object Shapes {
    val card = RoundedCornerShape(16.dp)
    val control = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(6.dp)
}

val LocalStudioColors = staticCompositionLocalOf { DarkStudio }

object Studio {
    val colors: StudioColors
        @Composable get() = LocalStudioColors.current
}

@Composable
fun StudioTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkStudio else LightStudio
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent, onSecondary = c.onAccent,
            background = c.background, onBackground = c.text, surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textMuted, outline = c.borderStrong,
            outlineVariant = c.border, error = c.error,
            surfaceContainerLowest = c.surface, surfaceContainerLow = c.surfaceRaised, surfaceContainer = c.surfaceRaised,
            surfaceContainerHigh = c.surfaceRaised, surfaceContainerHighest = c.surfaceHigh,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent, onSecondary = c.onAccent,
            background = c.background, onBackground = c.text, surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textMuted, outline = c.borderStrong,
            outlineVariant = c.border, error = c.error,
            surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surfaceHigh,
        )
    }
    CompositionLocalProvider(LocalStudioColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
