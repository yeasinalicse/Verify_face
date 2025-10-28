package info.yeasin.verify_face


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular step progress ring — ভিডিওর মতো চারপাশে segment-ভিত্তিক প্রগ্রেস দেখাবে
 */
@Composable
fun CircularStepProgress(
    modifier: Modifier = Modifier,
    totalSteps: Int,
    currentStepIndex: Int, // 0-based
    ringThickness: Dp = 10.dp,
    gapDegrees: Float = 8f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = ringThickness.toPx(), cap = StrokeCap.Round)
        val sizeMin = size.minDimension
        val inset = ringThickness.toPx() / 2f
        val rect = Rect(Offset(inset, inset), Size(sizeMin - ringThickness.toPx(), sizeMin - ringThickness.toPx()))

        val sweepPerStep = (360f - gapDegrees * totalSteps) / totalSteps

        // draw background segments
        repeat(totalSteps) { i ->
            val start = i * (sweepPerStep + gapDegrees) - 90f
            drawArc(
                color = inactiveColor,
                startAngle = start,
                sweepAngle = sweepPerStep,
                useCenter = false,
                style = stroke,
                topLeft = rect.topLeft,
                size = rect.size
            )
        }
        // draw active segments
        for (i in 0..currentStepIndex.coerceAtMost(totalSteps - 1)) {
            val start = i * (sweepPerStep + gapDegrees) - 90f
            drawArc(
                brush = Brush.sweepGradient(
                    0f to activeColor,
                    1f to activeColor.copy(alpha = 0.65f),
                    center = rect.center
                ),
                startAngle = start,
                sweepAngle = sweepPerStep,
                useCenter = false,
                style = stroke,
                topLeft = rect.topLeft,
                size = rect.size
            )
        }
    }
}

/**
 * বামদিকে চলমান বাউন্সি অ্যারো — TURN_LEFT ধাপে ইউজারকে গাইড করবে
 */
@Composable
fun BouncingArrowLeft(
    modifier: Modifier = Modifier,
    amplitude: Dp = 32.dp,
    thickness: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    arrowHeadSize: Dp = 14.dp,
) {
    val pxAmplitude = with(androidx.compose.ui.platform.LocalDensity.current) { amplitude.toPx() }
    val pxThickness = with(androidx.compose.ui.platform.LocalDensity.current) { thickness.toPx() }
    val pxHead = with(androidx.compose.ui.platform.LocalDensity.current) { arrowHeadSize.toPx() }

    val anim = rememberInfiniteTransition(label = "arrow")
    val x by anim.animateFloat(
        initialValue = 0f,
        targetValue = -pxAmplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dx"
    )
    val alpha by anim.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "alpha"
    )

    Canvas(
        modifier = modifier
    ) {
        val centerY = size.height / 2f
        val startX = size.width * 0.75f + x
        val endX = startX - size.width * 0.25f

        // main line
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = pxThickness,
            cap = StrokeCap.Round
        )
        // arrow head
        val headBase = Offset(endX, centerY)
        val leftWing = Offset(endX + pxHead, centerY - pxHead)
        val rightWing = Offset(endX + pxHead, centerY + pxHead)
        drawLine(color = color.copy(alpha = alpha), start = headBase, end = leftWing, strokeWidth = pxThickness, cap = StrokeCap.Round)
        drawLine(color = color.copy(alpha = alpha), start = headBase, end = rightWing, strokeWidth = pxThickness, cap = StrokeCap.Round)

        // subtle trailing dots
        val dotCount = 3
        for (i in 1..dotCount) {
            val t = i / (dotCount + 1f)
            val dx = lerp(startX, endX, t)
            drawCircle(
                color = color.copy(alpha = alpha * (0.25f + 0.75f * (1f - t))),
                radius = pxThickness,
                center = Offset(dx, centerY)
            )
        }
    }
}

/** small helper for linear interpolation */
private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * নিচে একটি ছোট status pill — টেক্সট দেখাতে সুন্দর লাগে
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CircleShape
    ) {
        Box(
            Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}