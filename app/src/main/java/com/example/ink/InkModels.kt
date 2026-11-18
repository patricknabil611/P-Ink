package com.example.ink

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
)

data class InkStroke(
    val points: List<InkPoint>,
)

fun normalizeInkPressure(pressure: Float): Float {
    if (pressure.isNaN() || pressure <= 0f) return 0.45f
    return pressure.coerceIn(0.04f, 1.0f)
}

fun pressureToStrokeWidth(
    pressure: Float,
    minWidth: Float,
    maxWidth: Float,
): Float {
    val p = normalizeInkPressure(pressure)
    val t = ((p - 0.04f) / 0.96f).coerceIn(0f, 1f)
    return minWidth + (maxWidth - minWidth) * t.pow(0.8f)
}

fun findIntersectedStrokeIndices(
    strokes: List<InkStroke>,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    radius: Float,
): Set<Int> {
    if (strokes.isEmpty() || radius <= 0f) return emptySet()

    val radiusSq = radius * radius
    val dx = x1 - x0
    val dy = y1 - y0
    val dist = kotlin.math.hypot(dx, dy)
    val steps = max(1, ceil(dist / max(4f, radius * 0.45f)).toInt())

    val hitIndices = HashSet<Int>()
    for (step in 0..steps) {
        val t = step.toFloat() / steps.toFloat()
        val sampleX = x0 + dx * t
        val sampleY = y0 + dy * t

        for (index in strokes.indices) {
            if (index in hitIndices) continue
            if (strokeIntersectsCircle(strokes[index], sampleX, sampleY, radiusSq)) {
                hitIndices.add(index)
            }
        }
        if (hitIndices.size == strokes.size) break
    }
    return hitIndices
}

private fun strokeIntersectsCircle(
    stroke: InkStroke,
    cx: Float,
    cy: Float,
    radiusSq: Float,
): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false

    if (pts.size == 1) {
        val dx = pts[0].x - cx
        val dy = pts[0].y - cy
        return (dx * dx + dy * dy) <= radiusSq
    }

    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        if (pointToSegmentDistanceSq(cx, cy, a.x, a.y, b.x, b.y) <= radiusSq) {
            return true
        }
    }
    return false
}

private fun pointToSegmentDistanceSq(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float {
    val abX = bx - ax
    val abY = by - ay
    val lenSq = abX * abX + abY * abY
    if (lenSq <= 0.0001f) {
        val dx = px - ax
        val dy = py - ay
        return dx * dx + dy * dy
    }
    val proj = (((px - ax) * abX + (py - ay) * abY) / lenSq).coerceIn(0f, 1f)
    val closestX = ax + proj * abX
    val closestY = ay + proj * abY
    val dx = px - closestX
    val dy = py - closestY
    return dx * dx + dy * dy
}
