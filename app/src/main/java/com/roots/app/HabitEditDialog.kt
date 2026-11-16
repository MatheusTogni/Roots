package com.roots.app

import android.app.Activity
import android.app.AlertDialog
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import com.roots.app.data.Habit
import com.roots.app.data.HabitOptions
import com.roots.app.data.Repository
import com.roots.app.ui.Fmt
import com.roots.app.ui.Palette
import com.roots.app.ui.Weight
import com.roots.app.ui.confirm
import com.roots.app.ui.dialogBuilder
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
import com.roots.app.ui.field
import com.roots.app.ui.font
import com.roots.app.ui.horizontal
import com.roots.app.ui.input
import com.roots.app.ui.lp
import com.roots.app.ui.margins
import com.roots.app.ui.oval
import com.roots.app.ui.rounded
import com.roots.app.ui.text
import com.roots.app.ui.vertical
import com.roots.app.ui.withAlpha

/**
 * Formulário de hábito num diálogo: criar, editar (nome, ícone e cor) e, na edição, excluir.
 * Editar a cor só muda o hábito; os registros guardam apenas datas e ficam intactos.
 * [onChanged] é chamado depois de salvar ou excluir.
 */
fun Activity.showHabitDialog(existing: Habit?, onChanged: () -> Unit) {
    val repo = Repository.get(this)
    var icon: String = existing?.icon ?: "🌱"
    var color: Int = existing?.color ?: HabitOptions.COLORS[0]

    val name = input("Ex.: Exercício, Leitura, Meditação", existing?.name ?: "").apply {
        filters = arrayOf(InputFilter.LengthFilter(HabitOptions.NAME_MAX_LENGTH))
    }

    val iconCells = ArrayList<TextView>()
    val colorCells = ArrayList<TextView>()

    fun styleIcons() {
        iconCells.forEachIndexed { i, cell ->
            val chosen = HabitOptions.ICONS[i] == icon
            cell.background = if (chosen) {
                rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(12f), color, dp(2))
            } else {
                rounded(0x00000000, dpf(12f))
            }
        }
    }

    fun styleColors() {
        colorCells.forEachIndexed { i, cell ->
            cell.text = if (HabitOptions.COLORS[i] == color) "✓" else ""
        }
    }

    /** Monta uma grade de [count] células em linhas de [columns] colunas iguais. */
    fun grid(count: Int, columns: Int, cellBuilder: (Int) -> View): android.widget.LinearLayout {
        val box = vertical()
        var start = 0
        while (start < count) {
            val row = horizontal()
            for (c in 0 until columns) {
                val index = start + c
                if (index < count) {
                    row.addView(cellBuilder(index), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                } else {
                    row.addView(View(this), lp(0, 1, 1f))
                }
            }
            box.addView(row, lp().margins(t = dp(4)))
            start += columns
        }
        return box
    }

    val iconGrid = grid(HabitOptions.ICONS.size, 6) { index ->
        val cell = text(HabitOptions.ICONS[index], 22f)
        cell.gravity = Gravity.CENTER
        cell.setPadding(0, dp(8), 0, dp(8))
        cell.setOnClickListener {
            icon = HabitOptions.ICONS[index]
            styleIcons()
        }
        iconCells.add(cell)
        cell
    }

    val colorGrid = grid(HabitOptions.COLORS.size, 5) { index ->
        val circle = TextView(this)
        circle.textSize = 16f
        circle.typeface = font(Weight.BLACK)
        circle.setTextColor(Palette.WHITE)
        circle.gravity = Gravity.CENTER
        circle.background = oval(HabitOptions.COLORS[index])
        circle.setOnClickListener {
            color = HabitOptions.COLORS[index]
            styleColors()
            styleIcons()
        }
        colorCells.add(circle)
        val holder = vertical()
        holder.gravity = Gravity.CENTER
        holder.addView(circle, lp(dp(38), dp(38)))
        holder
    }

    val form = vertical(dp(20)).apply {
        addView(field("Nome", name))
        addView(field("Ícone", iconGrid))
        addView(field("Cor", colorGrid))
    }
    styleIcons()
    styleColors()

    val builder = dialogBuilder()
        .setTitle(if (existing == null) "Novo hábito" else "Editar hábito")
        .setView(ScrollView(this).apply { addView(form) })
        .setPositiveButton("Salvar", null)
        .setNegativeButton("Cancelar", null)
    if (existing != null) builder.setNeutralButton("Excluir", null)
    val dialog = builder.create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = name.text.toString().trim()
            if (n.isEmpty()) {
                name.error = "Informe um nome"
                return@setOnClickListener
            }
            val base = existing ?: Habit(name = n, icon = icon, color = color)
            repo.saveHabit(base.copy(name = n, icon = icon, color = color))
            dialog.dismiss()
            onChanged()
        }
        if (existing != null) {
            val delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            delete.setTextColor(Palette.DANGER)
            delete.setOnClickListener {
                val count = repo.recordCount(existing.id)
                val detail = if (count > 0) " (${Fmt.dayCount(count)} realizados)" else ""
                confirm(
                    "Excluir hábito",
                    "Excluir \"${existing.name}\"? Todos os registros associados$detail também serão excluídos. Esta ação não pode ser desfeita.",
                    "Excluir"
                ) {
                    repo.deleteHabit(existing.id)
                    dialog.dismiss()
                    onChanged()
                }
            }
        }
    }
    dialog.show()
}
