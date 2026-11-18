package com.example.ink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.max
import kotlin.math.min

object InkBitmapRenderer {

    /**
     * Renders S Pen strokes onto a high-contrast white Bitmap with pressure-weighted
     * crisp black ink cropped comfortably around the ink bounding box for optimal
     * Gemini Arabic/English OCR.
     */
    fun renderToBitmap(
        strokes: List<InkStroke>,
        canvasWidth: Int,
        canvasHeight: Int,
    ): Bitmap? {
        val nonEmptyStrokes = strokes.filter { it.points.isNotEmpty() }
        if (nonEmptyStrokes.isEmpty()) return null

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (stroke in nonEmptyStrokes) {
            for (pt in stroke.points) {
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
        }

        val fallbackWidth = max(canvasWidth, 800).toFloat()
        val fallbackHeight = max(canvasHeight, 400).toFloat()

        val rawSpanX = max(maxX - minX, 40f)
        val rawSpanY = max(maxY - minY, 40f)
        val margin = max(56f, min(fallbackWidth, fallbackHeight) * 0.12f)

        val contentWidth = rawSpanX + margin * 2f
        val contentHeight = rawSpanY + margin * 2f

        val maxTargetWidth = 1600f
        val minTargetWidth = 800f
        val scale = when {
            contentWidth > maxTargetWidth -> maxTargetWidth / contentWidth
            contentWidth < minTargetWidth -> min(2.6f, minTargetWidth / contentWidth)
            else -> 1.0f
        }

        val outWidth = max(400, (contentWidth * scale).toInt())
        val outHeight = max(200, (contentHeight * scale).toInt())

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val minInkWidth = max(4.2f, 4.8f * scale)
        val maxInkWidth = max(10.0f, 12.5f * scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val offsetX = -minX + margin
        val offsetY = -minY + margin

        for (stroke in nonEmptyStrokes) {
            val pts = stroke.points
            if (pts.size == 1) {
                val cx = (pts[0].x + offsetX) * scale
                val cy = (pts[0].y + offsetY) * scale
                val radius = pressureToStrokeWidth(pts[0].pressure, minInkWidth, maxInkWidth) * 0.6f
                canvas.drawCircle(cx, cy, radius, dotPaint)
            } else {
                var prevMidX = (pts[0].x + offsetX) * scale
                var prevMidY = (pts[0].y + offsetY) * scale
                val segmentPath = Path()

                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val curr = pts[i]
                    val prevX = (prev.x + offsetX) * scale
                    val prevY = (prev.y + offsetY) * scale
                    val currX = (curr.x + offsetX) * scale
                    val currY = (curr.y + offsetY) * scale

                    val midX = if (i == pts.lastIndex) currX else (prevX + currX) * 0.5f
                    val midY = if (i == pts.lastIndex) currY else (prevY + currY) * 0.5f

                    val avgPressure = (normalizeInkPressure(prev.pressure) + normalizeInkPressure(curr.pressure)) * 0.5f
                    strokePaint.strokeWidth = pressureToStrokeWidth(avgPressure, minInkWidth, maxInkWidth)

                    segmentPath.reset()
                    segmentPath.moveTo(prevMidX, prevMidY)
                    if (i == pts.lastIndex && pts.size == 2) {
                        segmentPath.lineTo(currX, currY)
                    } else {
                        segmentPath.quadTo(prevX, prevY, midX, midY)
                    }
                    canvas.drawPath(segmentPath, strokePaint)

                    prevMidX = midX
                    prevMidY = midY
                }
            }
        }

        return bitmap
    }
}
