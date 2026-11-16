package com.roots.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** Desenhos do widget feitos em Canvas (RemoteViews só aceita imagens prontas). */
object WidgetDraw {

    /** Checkbox redondo de 24dp: anel na cor do hábito (pendente) ou círculo preenchido com ✓ (concluído). */
    fun checkBitmap(context: Context, color: Int, done: Boolean): Bitmap {
        val size = (24f * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = size.toFloat()
        val stroke = s * 0.09f
        val cx = s / 2f
        val radius = s / 2f - stroke

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (done) {
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawCircle(cx, cx, radius + stroke / 2f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = 0xFFFFFFFF.toInt()
            paint.strokeWidth = stroke * 1.1f
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            val path = Path()
            path.moveTo(s * 0.29f, s * 0.52f)
            path.lineTo(s * 0.44f, s * 0.67f)
            path.lineTo(s * 0.72f, s * 0.36f)
            canvas.drawPath(path, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.color = color
            paint.strokeWidth = stroke
            canvas.drawCircle(cx, cx, radius, paint)
        }
        return bitmap
    }
}
