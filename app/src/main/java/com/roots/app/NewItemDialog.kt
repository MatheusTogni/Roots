package com.roots.app

import android.app.Activity
import android.view.ViewGroup
import com.roots.app.data.HabitOptions
import com.roots.app.ui.Palette
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
import com.roots.app.ui.emojiBadge
import com.roots.app.ui.horizontal
import com.roots.app.ui.lp
import com.roots.app.ui.margins
import com.roots.app.ui.newSheet
import com.roots.app.ui.ripple
import com.roots.app.ui.rounded
import com.roots.app.ui.sheetFooter
import com.roots.app.ui.sheetForm
import com.roots.app.ui.showSheet
import com.roots.app.ui.text
import com.roots.app.ui.vertical

/**
 * Primeira pergunta ao criar algo novo: é um hábito (acompanhado num calendário) ou uma tarefa (algo a fazer, sem
 * calendário)? Escolher fecha a folha e chama [onHabit] ou [onTask], que abrem o formulário certo.
 */
fun Activity.showNewItemDialog(onHabit: () -> Unit, onTask: () -> Unit) {
    val dialog = newSheet()

    fun option(icon: String, color: Int, title: String, detail: String, onChoose: () -> Unit) =
        horizontal().apply {
            setPadding(dp(14), dp(14), dp(16), dp(14))
            background = ripple(rounded(Palette.INPUT_BG, dpf(18f), Palette.BORDER, dp(1)))
            isClickable = true
            addView(emojiBadge(icon, color, 48))
            val texts = vertical()
            texts.addView(text(title, 18f, Palette.TEXT, bold = true))
            texts.addView(text(detail, 13f, Palette.MUTED))
            addView(texts, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(14), r = dp(8)))
            addView(text("›", 24f, Palette.MUTED, bold = true))
            setOnClickListener {
                dialog.dismiss()
                onChoose()
            }
        }

    val form = sheetForm()
    form.addView(
        option("🌱", HabitOptions.COLORS[0], "Hábito", "Acompanhe a constância, dia a dia, num calendário.", onHabit),
        lp()
    )
    form.addView(
        option("✅", HabitOptions.COLORS[2], "Tarefa", "Algo para fazer: uma vez só ou toda semana, sem calendário.", onTask),
        lp().margins(t = dp(10))
    )

    showSheet(
        dialog,
        title = "O que você quer criar?",
        subtitle = "Escolha o tipo para continuar.",
        form = form,
        footer = sheetFooter()
    )
}
