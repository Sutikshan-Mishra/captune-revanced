package dev.hd450btnc.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.hd450btnc.companion.dsp.EqBands
import dev.hd450btnc.companion.dsp.EqResponse
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

private const val RANGE_DB = 15f
private const val POINTS = 160

/**
 * Frequency response computed from the exact filter cascade the audio engine runs.
 * [gains] should already be animated by the caller so preset switches glide.
 */
@Composable
fun EqCurveView(gains: List<Float>, enabled: Boolean, preampDb: Float, modifier: Modifier = Modifier) {
    val c = Studio.colors
    val measurer = rememberTextMeasurer()
    val curve = remember(gains) { EqResponse.curve(gains, POINTS) }
    val bandPoints = remember(gains) { EqBands.FREQUENCIES.map { EqResponse.responseDb(gains, it).toFloat() } }
    val lineColor by animateColorAsState(if (enabled) c.accent else c.textFaint, label = "curveColor")
    val fillAlpha by animateFloatAsState(if (enabled) 0.22f else 0f, label = "curveFill")
    val labelStyle = StudioType.monoSmall.copy(color = c.textFaint)
    val summary = if (enabled) "Equalizer curve. Peak ${"%.1f".format(curve.maxOrNull() ?: 0f)} dB" else "Equalizer bypassed"

    Canvas(
        modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = summary },
    ) {
        val w = size.width
        val h = size.height
        val labelPad = 18.dp.toPx()
        val plotTop = 6.dp.toPx()
        val plotBottom = h - labelPad
        val plotH = plotBottom - plotTop
        fun y(db: Float) = plotTop + plotH * (0.5f - db / (2 * RANGE_DB))
        fun x(freq: Double) = (EqResponse.logPosition(freq) * w).toFloat()

        // Grid: decades and +/-6/12 dB, with 0 dB emphasised.
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
        for (db in listOf(-12f, -6f, 6f, 12f)) {
            drawLine(c.border, Offset(0f, y(db)), Offset(w, y(db)), 1f, pathEffect = dash)
        }
        drawLine(c.borderStrong, Offset(0f, y(0f)), Offset(w, y(0f)), 1.dp.toPx())
        for ((f, label) in listOf(100.0 to "100", 1000.0 to "1k", 10_000.0 to "10k")) {
            drawLine(c.border, Offset(x(f), plotTop), Offset(x(f), plotBottom), 1f)
            val m = measurer.measure(label, labelStyle)
            drawText(m, topLeft = Offset(x(f) - m.size.width / 2f, h - m.size.height))
        }
        for ((db, label) in listOf(12f to "+12", -12f to "−12")) {
            val m = measurer.measure(label, labelStyle)
            drawText(m, topLeft = Offset(4.dp.toPx(), y(db) - m.size.height - 2.dp.toPx()))
        }

        // Curve and soft fill towards the 0 dB line.
        val path = Path()
        val fill = Path()
        for (i in curve.indices) {
            val px = w * i / (POINTS - 1)
            val py = y(curve[i].coerceIn(-RANGE_DB, RANGE_DB))
            if (i == 0) {
                path.moveTo(px, py); fill.moveTo(px, y(0f)); fill.lineTo(px, py)
            } else {
                path.lineTo(px, py); fill.lineTo(px, py)
            }
        }
        fill.lineTo(w, y(0f))
        fill.close()
        if (fillAlpha > 0f) {
            drawPath(
                fill,
                Brush.verticalGradient(
                    0f to lineColor.copy(alpha = fillAlpha),
                    0.5f to lineColor.copy(alpha = fillAlpha * 0.25f),
                    1f to lineColor.copy(alpha = fillAlpha),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )
        }
        drawPath(
            path,
            lineColor,
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = if (enabled) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            ),
        )

        // Band nodes sit on the actual curve.
        for (i in 0 until EqBands.COUNT) {
            val center = Offset(x(EqBands.FREQUENCIES[i]), y(bandPoints[i].coerceIn(-RANGE_DB, RANGE_DB)))
            drawCircle(c.surface, 4.5.dp.toPx(), center)
            drawCircle(lineColor, 4.5.dp.toPx(), center, style = Stroke(1.6.dp.toPx()))
        }

        // Preamp marker on the right edge when headroom is applied.
        if (enabled && preampDb < 0f) {
            val py = y(preampDb.coerceAtLeast(-RANGE_DB))
            drawLine(c.textMuted, Offset(w - 10.dp.toPx(), py), Offset(w, py), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
