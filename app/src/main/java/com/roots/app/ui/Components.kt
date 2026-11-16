package com.roots.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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

/**
 * Faixa de quadradinhos que imita o calendário do hábito, para o formulário mostrar ao vivo
 * como a cor escolhida vai ficar. O desenho é só ilustrativo (um padrão fixo de dias feitos).
 */
class HabitPreviewStrip(context: Context, private var tint: Int) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rect = RectF()
    private val gap = context.dpf(3f)

    fun recolor(newTint: Int) {
        tint = newTint
        invalidate()
    }

    private fun cellSize(width: Int): Float = (width - gap * (COLUMNS - 1)) / COLUMNS

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = ROWS * cellSize(width) + (ROWS - 1) * gap
        setMeasuredDimension(width, height.toInt() + 1)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cell = cellSize(width)
        val radius = cell * 0.22f
        for (col in 0 until COLUMNS) {
            for (row in 0 until ROWS) {
                val index = col * ROWS + row
                val left = col * (cell + gap)
                val top = row * (cell + gap)
                rect.set(left, top, left + cell, top + cell)
                // Padrão fixo, espalhado, só para parecer um calendário de verdade.
                paint.color = if ((index * 37 + 11) % 10 < 6) tint else Palette.EMPTY_CELL
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }
    }

    private companion object {
        const val COLUMNS = 22
        const val ROWS = 3
    }
}

/** Emoji do hábito sobre um quadrado arredondado tingido com a cor dele. */
fun Context.emojiBadge(icon: String, color: Int, sizeDp: Int = 40): TextView =
    TextView(this).apply {
        text = icon
        textSize = (sizeDp * 0.5f)
        gravity = Gravity.CENTER
        background = rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(12f))
        layoutParams = lp(dp(sizeDp), dp(sizeDp))
    }
