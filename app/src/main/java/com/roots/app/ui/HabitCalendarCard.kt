package com.roots.app.ui

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.LinearLayout
import com.roots.app.data.Habit
import java.time.LocalDate

/**
 * Card de um hábito: identificação (emoji, nome, dias no período), o calendário próprio
 * e a linha que descreve o dia tocado. Cada hábito tem o seu card e o seu calendário.
 * Tocar no cabeçalho abre a edição do hábito.
 */
class HabitCalendarCard(
    private val context: Context,
    private val habit: Habit,
    private val window: MonthWindow,
    initialDone: Set<String>,
    onEdit: () -> Unit
) {
    private val graph = ContributionGraph(context)
    private val countView = context.text("", 14f, Palette.MUTED, bold = true)
    private val selectedView = context.text("", 13f, Palette.MUTED)
    private var done: Set<String> = initialDone
    private var selected: LocalDate? = null

    val view: LinearLayout = context.card()

    init {
        val title = context.text(habit.name, 17f, Palette.TEXT, bold = true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val header = context.horizontal().apply {
            isClickable = true
            background = ripple(rounded(0x00000000, context.dpf(12f)))
            setOnClickListener { onEdit() }
            addView(context.emojiBadge(habit.icon, habit.color, 36))
            addView(title, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = context.dp(12)))
            addView(countView, lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(l = context.dp(8)))
            addView(
                context.text("✎", 15f, Palette.MUTED),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(l = context.dp(10), r = context.dp(2))
            )
        }
        view.addView(header)

        graph.setData(window, done, habit.color)
        graph.onDayClick = { date ->
            selected = date
            graph.select(date)
            updateSelectedLine()
        }
        view.addView(graph, lp().margins(t = context.dp(12)))
        view.addView(selectedView, lp().margins(t = context.dp(8), l = context.dp(2)))

        updateCount()
        updateSelectedLine()
    }

    /** Recarrega os dias concluídos (vindos do banco) e, se pedido, anima o quadrado de [popDate]. */
    fun refresh(newDone: Set<String>, popDate: LocalDate?) {
        done = newDone
        graph.setDone(newDone)
        if (popDate != null && window.contains(popDate)) graph.pop(popDate)
        updateCount()
        updateSelectedLine()
    }

    private fun updateCount() {
        countView.text = Fmt.dayCount(done.size)
    }

    private fun updateSelectedLine() {
        val date = selected
        if (date == null) {
            selectedView.text = "Toque em um dia para ver os detalhes"
            selectedView.setTextColor(withAlpha(Palette.MUTED, 0xB0))
        } else {
            val isDone = done.contains(date.toString())
            selectedView.text = Fmt.capitalize(Fmt.dayLabel(date)) + " · " + (if (isDone) "realizado ✓" else "não realizado")
            selectedView.setTextColor(if (isDone) Palette.TEXT else Palette.MUTED)
        }
    }
}
