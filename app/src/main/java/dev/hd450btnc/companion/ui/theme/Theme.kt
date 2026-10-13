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

/** Studio palette: graphite layers, cool-white text, one lime accent used sparingly. */
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
    background = Color(0xFF0B0C0D),
    surface = Color(0xFF131517),
    surfaceRaised = Color(0xFF1A1D20),
    surfaceHigh = Color(0xFF23272B),
    border = Color(0xFF262A2F),
    borderStrong = Color(0xFF383E45),
    text = Color(0xFFE9EDF0),
    textMuted = Color(0xFF9AA3AB),
    textFaint = Color(0xFF6A737B),
    accent = Color(0xFFC6F24E),
    accentSoft = Color(0x24C6F24E),
    onAccent = Color(0xFF111600),
    warning = Color(0xFFF2B84B),
    warningSoft = Color(0x1FF2B84B),
    error = Color(0xFFFF7A70),
    isDark = true,
)

val LightStudio = StudioColors(
    background = Color(0xFFF3F4F2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF8F9F7),
    surfaceHigh = Color(0xFFECEEEA),
    border = Color(0xFFE0E3DE),
    borderStrong = Color(0xFFC4C9C2),
    text = Color(0xFF111315),
    textMuted = Color(0xFF545B60),
    textFaint = Color(0xFF7C8389),
    accent = Color(0xFF4A6600),
    accentSoft = Color(0x1A4A6600),
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
