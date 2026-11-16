package com.roots.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import java.time.LocalDate

/**
 * Calendário de contribuições de um hábito, desenhado em Canvas no estilo do GitHub:
 * semanas em colunas, os 7 dias da semana em linhas e o nome dos meses no topo.
 * Dia concluído = quadrado na cor do hábito; sem registro = quadrado neutro.
 * Datas futuras não são desenhadas.
 */
class ContributionGraph(context: Context) : View(context) {

    private var window: MonthWindow? = null
    private var done: Set<String> = emptySet()
    private var habitColor: Int = Palette.ACCENT
    private var selected: LocalDate? = null

    private var popDate: LocalDate? = null
    private var popProgress = 1f
    private var popAnimator: ValueAnimator? = null

    /** Chamado quando o usuário toca num dia desenhado. */
    var onDayClick: ((LocalDate) -> Unit)? = null

    private val gap = context.dpf(3f)
    private val labelWidth = context.dpf(16f)
    private val topHeight = context.dpf(16f)
    private val minCell = context.dpf(10f)
    private val maxCell = context.dpf(26f)

    // Medidas derivadas da largura atual (recalculadas em computeLayout).
    private var cell = 0f
    private var gridLeft = 0f

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = context.dpf(10f)
        color = Palette.MUTED
        typeface = context.font(Weight.SEMIBOLD)
    }
    private val rect = RectF()

    fun setData(win: MonthWindow, doneDates: Set<String>, color: Int) {
        window = win
        done = doneDates
        habitColor = color
        val s = selected
        if (s != null && !win.contains(s)) selected = null
        requestLayout()
        invalidate()
    }

    /** Atualiza só os dias concluídos (mesma janela). */
    fun setDone(doneDates: Set<String>) {
        done = doneDates
        invalidate()
    }

    fun select(date: LocalDate?) {
        selected = date
        invalidate()
    }

    /** Pequena animação de "brotar" no quadrado do dia concluído. */
    fun pop(date: LocalDate) {
        popAnimator?.cancel()
        popDate = date
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 320
        a.interpolator = OvershootInterpolator(2.5f)
        a.addUpdateListener {
            popProgress = it.animatedValue as Float
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                popDate = null
                popProgress = 1f
                invalidate()
            }
        })
        popAnimator = a
        a.start()
    }

    /** Quadrado e margem lateral para caber todas as colunas na largura disponível. */
    private fun computeLayout(viewWidth: Int) {
        val cols = window?.columns ?: 1
        val avail = viewWidth - labelWidth
        cell = ((avail - gap * (cols - 1)) / cols).coerceIn(minCell, maxCell)
        val gridWidth = cols * cell + gap * (cols - 1)
        gridLeft = labelWidth + (avail - gridWidth).coerceAtLeast(0f) / 2f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        computeLayout(w)
        val h = topHeight + 7 * cell + 6 * gap
        setMeasuredDimension(w, h.toInt() + 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val win = window ?: return
        computeLayout(width)
        val step = cell + gap
        val radius = cell * 0.22f

        // Letras dos dias da semana (todas, ou só seg/qua/sex quando os quadrados são pequenos).
        val showAll = cell >= context.dpf(14f)
        val fm = labelPaint.fontMetrics
        for (row in 0..6) {
            if (!showAll && row % 2 == 0) continue
            val cy = topHeight + row * step + cell / 2f
            canvas.drawText(Fmt.WEEKDAY_LETTERS[row], 0f, cy - (fm.ascent + fm.descent) / 2f, labelPaint)
        }

        // Nome de cada mês sobre a coluna do dia 1.
        for (m in win.months()) {
            val label = Fmt.monthShort(m)
            val textWidth = labelPaint.measureText(label)
            var x = gridLeft + win.columnOf(m.atDay(1)) * step
            if (x > width - textWidth) x = width - textWidth
            canvas.drawText(label, x, topHeight - context.dpf(5f), labelPaint)
        }

        val todayDate = win.today
        for (col in 0 until win.columns) {
            for (row in 0..6) {
                val date = win.dateAt(col, row)
                if (!win.contains(date)) continue
                val left = gridLeft + col * step
                val top = topHeight + row * step
                rect.set(left, top, left + cell, top + cell)

                if (date == popDate) {
                    val half = cell / 2f * (0.4f + 0.6f * popProgress)
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    rect.set(cx - half, cy - half, cx + half, cy + half)
                }

                cellPaint.color = if (done.contains(date.toString())) habitColor else Palette.EMPTY_CELL
                canvas.drawRoundRect(rect, radius, radius, cellPaint)

                if (date == selected) {
                    ringPaint.color = Palette.TEXT
                    ringPaint.strokeWidth = context.dpf(2f)
                    val grow = context.dpf(1.5f)
                    rect.set(left - grow, top - grow, left + cell + grow, top + cell + grow)
                    canvas.drawRoundRect(rect, radius + grow, radius + grow, ringPaint)
                } else if (date == todayDate) {
                    ringPaint.color = Palette.MUTED
                    ringPaint.strokeWidth = context.dpf(1.5f)
                    val grow = context.dpf(1.25f)
                    rect.set(left - grow, top - grow, left + cell + grow, top + cell + grow)
                    canvas.drawRoundRect(rect, radius + grow, radius + grow, ringPaint)
                }
            }
        }
    }

    private fun dateAt(x: Float, y: Float): LocalDate? {
        val win = window ?: return null
        computeLayout(width)
        if (x < gridLeft || y < topHeight) return null
        val step = cell + gap
        val col = ((x - gridLeft) / step).toInt()
        val row = ((y - topHeight) / step).toInt()
        if (col >= win.columns || row > 6) return null
        val date = win.dateAt(col, row)
        return if (win.contains(date)) date else null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (window == null) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val date = dateAt(event.x, event.y)
            if (date != null) {
                performClick()
                onDayClick?.invoke(date)
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        popAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
