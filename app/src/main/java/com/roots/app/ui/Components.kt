package com.roots.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import kotlin.math.min

/**
 * Controle de conclusão: círculo vazio (borda na cor do hábito) ou preenchido com um ✓.
 * [setChecked] anima o preenchimento quando `animate` é true.
 */
class CheckCircle(context: Context, private var tint: Int) : View(context) {

    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpf(2f)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpf(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Palette.WHITE
    }
    private val path = Path()

    fun recolor(newTint: Int) {
        tint = newTint
        invalidate()
    }

    fun setChecked(checked: Boolean, animate: Boolean) {
        animator?.cancel()
        val target = if (checked) 1f else 0f
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        val a = ValueAnimator.ofFloat(progress, target)
        a.duration = 240
        if (checked) a.interpolator = OvershootInterpolator(2.2f) else a.interpolator = DecelerateInterpolator()
        a.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator = a
        a.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f - ring.strokeWidth
        ring.color = tint
        canvas.drawCircle(cx, cy, radius, ring)

        val p = if (progress < 0f) 0f else progress
        if (p > 0f) {
            fill.color = tint
            canvas.drawCircle(cx, cy, radius * p, fill)
            if (p > 0.45f) {
                val a = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                mark.alpha = (255 * a).toInt()
                val box = radius * 2f
                val left = cx - radius
                val top = cy - radius
                path.reset()
                path.moveTo(left + box * 0.28f, top + box * 0.52f)
                path.lineTo(left + box * 0.44f, top + box * 0.68f)
                path.lineTo(left + box * 0.73f, top + box * 0.36f)
                canvas.drawPath(path, mark)
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}

/** Emoji do hábito sobre um quadrado arredondado tingido com a cor dele. */
fun Context.emojiBadge(icon: String, color: Int, sizeDp: Int = 40): TextView =
    TextView(this).apply {
        text = icon
        textSize = (sizeDp * 0.5f)
        gravity = Gravity.CENTER
        background = rounded(withAlpha(color, 0x2E), dpf(12f))
        layoutParams = lp(dp(sizeDp), dp(sizeDp))
    }
