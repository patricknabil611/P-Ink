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
     * Renders S Pen strokes onto a high-contrast white Bitmap with crisp black ink
     * cropped comfortably around the ink bounding box for optimal Gemini Arabic/English OCR.
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
        val fallbackHeight = max(canvasHeight, 360).toFloat()

        val rawSpanX = max(maxX - minX, 40f)
        val rawSpanY = max(maxY - minY, 40f)
        val margin = max(48f, min(fallbackWidth, fallbackHeight) * 0.12f)

        val contentWidth = rawSpanX + margin * 2f
        val contentHeight = rawSpanY + margin * 2f

        val maxTargetWidth = 1280f
        val minTargetWidth = 640f
        val scale = when {
            contentWidth > maxTargetWidth -> maxTargetWidth / contentWidth
            contentWidth < minTargetWidth -> min(2.2f, minTargetWidth / contentWidth)
            else -> 1.0f
        }

        val outWidth = max(320, (contentWidth * scale).toInt())
        val outHeight = max(180, (contentHeight * scale).toInt())

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = max(5.5f, 7.5f * scale)
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
                canvas.drawCircle(cx, cy, paint.strokeWidth * 0.55f, dotPaint)
            } else {
                val path = Path()
                val firstX = (pts[0].x + offsetX) * scale
                val firstY = (pts[0].y + offsetY) * scale
                path.moveTo(firstX, firstY)

                for (i in 1 until pts.size) {
                    val prevX = (pts[i - 1].x + offsetX) * scale
                    val prevY = (pts[i - 1].y + offsetY) * scale
                    val currX = (pts[i].x + offsetX) * scale
                    val currY = (pts[i].y + offsetY) * scale
                    val midX = (prevX + currX) * 0.5f
                    val midY = (prevY + currY) * 0.5f
                    path.quadTo(prevX, prevY, midX, midY)
                }

                val lastX = (pts.last().x + offsetX) * scale
                val lastY = (pts.last().y + offsetY) * scale
                path.lineTo(lastX, lastY)
                canvas.drawPath(path, paint)
            }
        }

        return bitmap
    }
}
