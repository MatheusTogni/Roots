package com.roots.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.roots.app.ui.Fmt
import com.roots.app.ui.MonthWindow

/**
 * Desenha o calendário de contribuições de um hábito num bitmap, para o widget "Meus gráficos"
 * (RemoteViews não aceita Views próprias, só imagens prontas). Usa a mesma [MonthWindow] do app,
 * então semanas, meses e anos bissextos saem idênticos ao calendário da tela.
 *
 * As cores de apoio são neutras e funcionam sobre fundo claro e escuro: assim a imagem não precisa ser
 * refeita quando o tema do aparelho muda. O que depende do tema (fundo, nome, contagem) fica em views
 * normais do layout do widget.
 */
object GraphDraw {

    // Medidas em pixels do bitmap; o widget escala a imagem para a largura disponível.
    private const val CELL = 40f
    private const val GAP = 7f
    private const val LABEL_WIDTH = 36f
    private const val TOP_HEIGHT = 42f
    private const val MARGIN = 3f

    /** Quadrado sem registro: cinza translúcido (aparece suave no claro e no escuro). */
    private const val EMPTY_CELL = 0x2E8A8A8A

    /** Letras e meses: cinza-esverdeado médio, legível nos dois temas. */
    private val LABEL_COLOR = 0xFF7B867F.toInt()

    fun render(window: MonthWindow, done: Set<String>, habitColor: Int): Bitmap {
        val step = CELL + GAP
        val width = (MARGIN + LABEL_WIDTH + window.columns * step - GAP + MARGIN).toInt()
        val height = (TOP_HEIGHT + 7 * step - GAP + MARGIN).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_COLOR
            textSize = 24f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = LABEL_COLOR
        }
        val rect = RectF()
        val radius = CELL * 0.22f
        val gridLeft = MARGIN + LABEL_WIDTH

        // Letras dos dias da semana, uma por linha.
        val fm = labelPaint.fontMetrics
        for (row in 0..6) {
            val centerY = TOP_HEIGHT + row * step + CELL / 2f
            canvas.drawText(Fmt.WEEKDAY_LETTERS[row], MARGIN, centerY - (fm.ascent + fm.descent) / 2f, labelPaint)
        }

        // Nome de cada mês sobre a coluna do dia 1.
        for (month in window.months()) {
            val label = Fmt.monthShort(month)
            val labelWidth = labelPaint.measureText(label)
            var x = gridLeft + window.columnOf(month.atDay(1)) * step
            if (x > width - MARGIN - labelWidth) x = width - MARGIN - labelWidth
            canvas.drawText(label, x, TOP_HEIGHT - 12f, labelPaint)
        }

        // Quadrados: cor do hábito quando feito; vazio quando não; dias futuros não são desenhados.
        for (col in 0 until window.columns) {
            for (row in 0..6) {
                val date = window.dateAt(col, row)
                if (!window.contains(date)) continue
                val left = gridLeft + col * step
                val top = TOP_HEIGHT + row * step
                rect.set(left, top, left + CELL, top + CELL)
                cellPaint.color = if (done.contains(date.toString())) habitColor else EMPTY_CELL
                canvas.drawRoundRect(rect, radius, radius, cellPaint)

                if (date == window.today) {
                    val grow = 2.5f
                    rect.set(left - grow, top - grow, left + CELL + grow, top + CELL + grow)
                    canvas.drawRoundRect(rect, radius + grow, radius + grow, ringPaint)
                }
            }
        }
        return bitmap
    }
}
