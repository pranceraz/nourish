package com.nourish.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.theme.nourishColors

internal enum class Glyph(vararg outlines: String) {
    Today("M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0 -18z", "M12 8v4l2.5 1.5"),
    Diary("M5 4h12a2 2 0 0 1 2 2v14H6a2 2 0 0 1 -2 -2V6q0 -2 1 -2z", "M8 4v16M11 8h5M11 12h5"),
    Insights("M4 19h16M7 15v-4m5 4V6m5 9V9"),
    Settings("M4 7h16M4 17h16", "M8 4v6m8 4v6"),
    Add("M12 5v14M5 12h14"),
    Close("M6 6l12 12M18 6L6 18"),
    Check("M5 12l4 4L19 6"),
    Arrow("M5 12h13m-5-5 5 5-5 5"),
    Back("M19 12H6m5 5-5-5 5-5"),
    Text("M4 5h16v11h-9l-5 3v-3H4z", "M8 9h8m-8 3h5"),
    Camera("M4 7h4l2-3h4l2 3h4v13H4z", "M12 10a3 3 0 1 0 0 6a3 3 0 0 0 0-6z"),
    Photo("M4 4h16v16H4z", "M5 17l5-5 3 3 2-2 4 4M15 8h.01"),
    Search("M10 4a6 6 0 1 0 0 12a6 6 0 0 0 0-12z", "M14.5 14.5 20 20"),
    Barcode("M4 8V5h4m8 0h4v3M4 16v3h4m8 0h4v-3M8 9v6m4-6v6m4-6v6"),
    Bowl("M4 12h16c0 4-3 7-8 7s-8-3-8-7z", "M8 9l5-5m-2 5 5-5"),
    Sun("M12 8a4 4 0 1 0 0 8a4 4 0 0 0 0-8z", "M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2"),
    Brand("M12 8c-7-5-10 7-3 10 2 1 4 0 5 0 7-2 5-13-2-10z", "M12 8c0-3 2-5 5-5-1 3-2 4-5 5z"),
    ;

    val paths: List<Path> = outlines.map { PathParser().parsePathString(it).toPath() }
}

@Composable
internal fun NourishIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            glyph.paths.forEach { path -> drawPath(path, color, style = stroke) }
        }
    }
}

internal enum class Sticker(val outline: String) {
    Petal("M49 13C61-4 84 3 87 24c17 2 20 24 6 35 10 18-7 34-26 30-10 17-35 13-39-5C6 86-3 66 10 52-2 34 13 14 33 20c4-6 9-9 16-7z"),
    Burst("M48 3 61 23 85 11 86 35 99 49 83 61 87 86 64 80 49 98 35 81 11 89 15 65 1 51 19 37 15 14 38 21z"),
    Heart("M49 22C28-3 2 14 4 37c2 24 24 38 48 56 25-17 45-38 45-59C96 9 66 3 49 22z"),
    ;

    val path: Path = PathParser().parsePathString(outline).toPath()
}

@Composable
internal fun StickerShape(sticker: Sticker, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            drawPath(sticker.path, color)
        }
    }
}

private fun path(data: String) = PathParser().parsePathString(data).toPath()

private val fruitShadow = path("M25 59C35 44 45 45 52 45c15-11 42-12 57 4 13 14 11 38-5 54-15 14-40 21-62 12C20 106 12 78 25 59z")
private val fruitRind = path("M29 52c13-12 40-17 59-14 24 5 34 21 32 36-3 26-32 43-58 40-30-2-53-21-48-41 2-8 7-15 15-21z")
private val fruitFlesh = path("M31 58c13-11 38-15 56-12 19 3 28 15 26 27-4 21-27 35-51 32-25-2-45-17-40-32 1-6 4-11 9-15z")
private val fruitLeaf = path("M67 39c-6-19 6-34 29-35 1 17-8 33-29 35z")
private val fruitStem = path("M68 44c1-14 12-28 22-34M66 44C62 32 57 28 50 24")
private val fruitSegments = path("M65 72 51 52m14 20 11-22M65 72l38-8m-38 8 35 14M65 72l11 28M65 72 43 97m22-25-37 6")
private val fruitSparkle = path("M25 9c1 10 6 15 15 16-9 1-14 6-15 16-1-10-6-15-15-16 9-1 14-6 15-16z")
private val fruitTinySparkle = path("M111 19c1 5 4 8 9 9-5 1-8 4-9 9-1-5-4-8-9-9 5-1 8-4 9-9z")

@Composable
internal fun CitrusSticker(modifier: Modifier = Modifier) {
    val colors = nourishColors
    Canvas(modifier) {
        scale(size.width / 128f, size.height / 126f, pivot = Offset.Zero) {
            val inkStroke = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawOval(colors.ink.copy(alpha = 0.14f), Offset(19f, 108f), Size(97f, 11f))
            drawPath(fruitShadow, colors.ink)
            drawPath(fruitRind, colors.peach)
            drawPath(fruitRind, colors.ink, style = inkStroke)
            drawPath(fruitFlesh, colors.pink)
            drawPath(fruitFlesh, colors.ink, style = Stroke(1.4f))
            drawPath(fruitSegments, colors.ink.copy(alpha = 0.65f), style = Stroke(1.25f, cap = StrokeCap.Round))
            drawOval(colors.cream, Offset(61f, 69f), Size(8f, 6f))
            drawPath(fruitLeaf, colors.mint)
            drawPath(fruitLeaf, colors.ink, style = inkStroke)
            drawPath(fruitStem, colors.ink, style = inkStroke)
            drawPath(fruitSparkle, colors.cream)
            drawPath(fruitSparkle, colors.ink, style = Stroke(1.5f))
            drawPath(fruitTinySparkle, colors.ink)
        }
    }
}
