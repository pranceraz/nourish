package com.nourish.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nourish.app.R

@Immutable
internal data class NourishColors(
    val canvas: Color,
    val text: Color,
    val muted: Color,
    val panel: Color,
    val divider: Color,
    val ink: Color = Color(0xFF201E21),
    val cream: Color = Color(0xFFFBF5E8),
    val dockMuted: Color = Color(0xFFD1C7C8),
    val dockPanel: Color = Color(0xFF373238),
    val butter: Color = Color(0xFFF8DE70),
    val pink: Color = Color(0xFFF1B9D7),
    val lilac: Color = Color(0xFFD7C7F3),
    val blue: Color = Color(0xFFBBD3FA),
    val mint: Color = Color(0xFFB7CF94),
    val peach: Color = Color(0xFFF5C3A8),
)

private val DayColors = NourishColors(
    canvas = Color(0xFFFBF5E8),
    text = Color(0xFF201E21),
    muted = Color(0xFF655A56),
    panel = Color(0xFFF1E8D8),
    divider = Color(0xFFD7CBB8),
)

private val NightColors = NourishColors(
    canvas = Color(0xFF242124),
    text = Color(0xFFFBF5E8),
    muted = Color(0xFFCEC1C3),
    panel = Color(0xFF342F34),
    divider = Color(0xFF59515A),
    ink = Color(0xFF19171A),
)

private val LocalNourishColors = staticCompositionLocalOf { DayColors }
internal val nourishColors: NourishColors
    @Composable get() = LocalNourishColors.current

private val Display = FontFamily(Font(R.font.bricolage_extra_bold, FontWeight.ExtraBold))
private val Body = FontFamily.SansSerif

internal object NourishType {
    val hero = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 92.sp,
        lineHeight = 96.sp,
        letterSpacing = (-5).sp,
        fontFeatureSettings = "tnum",
    )
    val screen = display(46, 49)
    val section = display(30, 33)
    val metric = display(34, 37).copy(fontFeatureSettings = "tnum")
    val heading = display(23, 26)
    val body = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 21.sp)
    val strong = body.copy(fontWeight = FontWeight.Bold)
    val caption = body.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val eyebrow = caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
    val tab = caption.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)

    private fun display(size: Int, line: Int) = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = (-0.7).sp,
    )
}

internal object NourishShape {
    val small = RoundedCornerShape(12.dp)
    val panel = RoundedCornerShape(22.dp)
    val dock = RoundedCornerShape(26.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val pill = RoundedCornerShape(50)
}

internal object NourishMotion {
    private val easeOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val press = tween<Float>(durationMillis = 150, easing = easeOut)
    val settle = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val selection = tween<Float>(durationMillis = 120, easing = easeOut)
}

@Composable
internal fun NourishTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) NightColors else DayColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.pink,
            onPrimary = colors.ink,
            background = colors.canvas,
            onBackground = colors.text,
            surface = colors.panel,
            onSurface = colors.text,
            onSurfaceVariant = colors.muted,
            outline = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = colors.ink,
            onPrimary = colors.cream,
            background = colors.canvas,
            onBackground = colors.text,
            surface = colors.panel,
            onSurface = colors.text,
            onSurfaceVariant = colors.muted,
            outline = colors.divider,
        )
    }
    CompositionLocalProvider(LocalNourishColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography(titleLarge = NourishType.heading, bodyMedium = NourishType.body),
            shapes = Shapes(small = NourishShape.small, medium = NourishShape.panel, large = NourishShape.dock),
            content = content,
        )
    }
}
