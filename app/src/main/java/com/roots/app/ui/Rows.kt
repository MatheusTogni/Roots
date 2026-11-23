package com.roots.app.ui

import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.view.ViewGroup
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Linha da lista "Hoje" (hábitos e tarefas): emoji sobre a cor, nome e o círculo de conclusão.
 * O toque e o toque longo ficam por conta de quem usa; [setDone] só atualiza o desenho.
 * Com [strikeWhenDone], o nome concluído fica apagado e riscado (as tarefas fazem isso; os hábitos não).
 */
class TodayRow(
    context: Context,
    icon: String,
    color: Int,
    name: String,
    done: Boolean,
    private val strikeWhenDone: Boolean = false
) {
    val view: LinearLayout = context.horizontal()
    private val nameView: TextView = context.text(name, 17f, Palette.TEXT, bold = true)
    private val check = CheckCircle(context, color)

    init {
        view.setPadding(context.dp(10), context.dp(8), context.dp(12), context.dp(8))
        view.background = ripple(rounded(0x00000000, context.dpf(14f)))
        view.isClickable = true

        view.addView(context.emojiBadge(icon, color, 40))
        nameView.maxLines = 1
        nameView.ellipsize = TextUtils.TruncateAt.END
        view.addView(nameView, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = context.dp(12), r = context.dp(8)))
        view.addView(check, lp(context.dp(30), context.dp(30)))
        setDone(done, false)
    }

    fun setDone(done: Boolean, animate: Boolean) {
        check.setChecked(done, animate)
        if (strikeWhenDone) {
            nameView.setTextColor(if (done) Palette.MUTED else Palette.TEXT)
            nameView.paintFlags = if (done) {
                nameView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
}

/** Linha sem círculo de conclusão: emoji, nome e uma linha de apoio (usada na lista de tarefas agendadas). */
fun Context.infoRow(icon: String, color: Int, name: String, detail: String): LinearLayout {
    val row = horizontal()
    row.setPadding(dp(10), dp(8), dp(12), dp(8))
    row.background = ripple(rounded(0x00000000, dpf(14f)))
    row.isClickable = true

    row.addView(emojiBadge(icon, color, 40))
    val texts = vertical()
    val title = text(name, 17f, Palette.TEXT, bold = true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    texts.addView(title)
    texts.addView(text(detail, 13f, Palette.MUTED).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    })
    row.addView(texts, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(12), r = dp(8)))
    row.addView(text("›", 22f, Palette.MUTED, bold = true))
    return row
}

/** Card com as [rows] uma sobre a outra, separadas por um filete que começa depois do emoji. */
fun Context.dividedCard(rows: List<View>): LinearLayout {
    val box = card()
    box.setPadding(dp(6), dp(6), dp(6), dp(6))
    rows.forEachIndexed { index, row ->
        box.addView(row)
        if (index < rows.lastIndex) {
            val divider = View(this)
            divider.setBackgroundColor(Palette.BORDER)
            box.addView(divider, lp(height = dp(1)).margins(l = dp(62), r = dp(10)))
        }
    }
    return box
}
