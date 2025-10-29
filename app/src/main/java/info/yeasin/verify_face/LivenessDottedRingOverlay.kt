package info.yeasin.verify_face


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * স্ক্রিনশটের মতো ডটেড সার্কুলার প্রগ্রেস + ৪টি কার্ভড ব্র্যাকেট + গোল হোল মাস্ক
 * @param progress 0f..1f (যতটা কমপ্লিট হয়েছে)
 * @param ringSize বৃত্তের ডায়ামিটার
 */
@Composable
fun LivenessDottedRingOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 300.dp,
    tickWidth: Dp = 6.dp,
    tickLength: Dp = 16.dp,
    tickGapDegrees: Float = 6f,
    startAngle: Float = -90f, // top
    holePadding: Dp = 10.dp,  // হোলের ভেতরের মার্জিন
    ringGradient: List<Color> = listOf(Color(0xFF1AA7A1), Color(0xFF0E7A77))
) {
    val density = LocalDensity.current
    val diameterPx = with(density) { ringSize.toPx() }
    val tickW = with(density) { tickWidth.toPx() }
    val tickL = with(density) { tickLength.toPx() }
    val holePad = with(density) { holePadding.toPx() }

    // smooth animation to target progress
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "ringProgress"
    )

    Canvas(
        modifier = modifier
            .size(ringSize)
            .drawWithCache {
                onDrawWithContent {
                    // camera preview নিচে থাকবে, এখানে শুধু overlay আঁকি
                    drawContent()
                    // (কিছুই কাটা লাগছে না – overlay আলাদা)
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)

        // রিং কোথায় ডট বসবে তার রেক্ট
        val ringR = radius - tickW // একটু ভেতরে
        val innerR = ringR - tickL
        val outerR = ringR

        // গ্রেডিয়েন্ট ব্রাশ
        val brush = Brush.sweepGradient(
            colors = ringGradient + ringGradient.first(), // loop
            center = Offset(cx, cy)
        )

        // ডট/টিক সেটিংস
        val sweepPerTick = 6f     // প্রতিটি ডটের কৌনিক দৈর্ঘ্য (ডিগ্রি)
        val gap = tickGapDegrees  // ডটের মাঝে ফাঁক
        val totalTicks = (360f / (sweepPerTick + gap)).toInt()

        // কতটা পূর্ণ হবে
        val ticksToFill = (totalTicks * animProgress).toInt()

        // ইনঅ্যাকটিভ কালার (হালকা ধূসর)
        val inactiveColor = Color(0x33000000)

        // সব ডট আঁকা (inactive)
        repeat(totalTicks) { i ->
            val start = startAngle + i * (sweepPerTick + gap)
            drawTickArc(
                cx, cy, innerR, outerR,
                startAngle = start,
                sweep = sweepPerTick,
                color = inactiveColor,
                strokeWidth = tickW
            )
        }
        // একটিভ ডটগুলো গ্রেডিয়েন্টে
        repeat(ticksToFill.coerceAtMost(totalTicks)) { i ->
            val start = startAngle + i * (sweepPerTick + gap)
            drawTickArc(
                cx, cy, innerR, outerR,
                startAngle = start,
                sweep = sweepPerTick,
                brush = brush,
                strokeWidth = tickW
            )
        }

        // ৪টা কার্ভড ব্র্যাকেট (হালকা সাদা)
        val bracketColor = Color(0xCCFFFFFF)
        val bracketSweep = 38f
        val bracketThickness = tickW
        val bracketInset = tickL + holePad + 8f
        val bracketRectSize = Size(
            (radius*2 - bracketInset*2),
            (radius*2 - bracketInset*2)
        )
        val topLeft = Offset(cx - bracketRectSize.width/2, cy - bracketRectSize.height/2)
        val stroke = Stroke(width = bracketThickness, cap = StrokeCap.Round)

        // 12, 3, 6, 9 o'clock-এর দিকে ছোট কার্ভ
        val anchors = listOf(-90f, 0f, 90f, 180f)
        anchors.forEach { base ->
            drawArc(
                color = bracketColor,
                startAngle = base - bracketSweep/2,
                sweepAngle = bracketSweep,
                useCenter = false,
                topLeft = topLeft,
                size = bracketRectSize,
                style = stroke
            )
        }
    }
}

/** একটি ছোট আর্ক-টিক আঁকার হেল্পার */
private fun DrawScope.drawTickArc(
    cx: Float, cy: Float,
    innerR: Float, outerR: Float,
    startAngle: Float,
    sweep: Float,
    color: Color? = null,
    brush: Brush? = null,
    strokeWidth: Float
) {
    // টিকটাকে আর্ক হিসেবে না এঁকে, আর্ক-সেক্টরের বাইরের বর্ডার আঁকি
    // rect টাকে outerR দিয়ে আর ভেতরে inset করে innerR লেয়ার করি
    val size = Size(outerR*2, outerR*2)
    val topLeft = Offset(cx - outerR, cy - outerR)
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    if (brush != null) {
        drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweep, useCenter = false,
            topLeft = topLeft, size = size, style = stroke)
    } else {
        drawArc(color = color ?: Color.Black, startAngle = startAngle, sweepAngle = sweep, useCenter = false,
            topLeft = topLeft, size = size, style = stroke)
    }
}
