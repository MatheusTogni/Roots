package com.roots.app

import android.animation.ObjectAnimator
import android.app.Activity
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import android.widget.TextView
import com.roots.app.data.HabitOptions
import com.roots.app.data.Repository
import com.roots.app.data.Task
import com.roots.app.data.TaskDays
import com.roots.app.ui.BtnStyle
import com.roots.app.ui.IconColorPicker
import com.roots.app.ui.Palette
import com.roots.app.ui.SegmentedControl
import com.roots.app.ui.Weight
import com.roots.app.ui.button
import com.roots.app.ui.confirm
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
import com.roots.app.ui.font
import com.roots.app.ui.horizontal
import com.roots.app.ui.input
import com.roots.app.ui.lp
import com.roots.app.ui.margins
import com.roots.app.ui.newSheet
import com.roots.app.ui.popView
import com.roots.app.ui.ripple
import com.roots.app.ui.rounded
import com.roots.app.ui.sheetFooter
import com.roots.app.ui.sheetForm
import com.roots.app.ui.sheetLabel
import com.roots.app.ui.showSheet
import com.roots.app.ui.text
import com.roots.app.ui.vertical
import com.roots.app.ui.withAlpha
import java.time.LocalDate

/**
 * Formulário de tarefa, na mesma folha do de hábito: nome, tipo, ícone e cor (sem calendário).
 * Tipos: **normal** (fica na lista até ser concluída) ou **agendada** (aparece só nos dias da semana escolhidos).
 * O tipo se escolhe ao criar e não muda depois; numa agendada, os dias podem ser editados.
 * [onChanged] é chamado depois de salvar ou excluir.
 */
fun Activity.showTaskDialog(existing: Task?, onChanged: () -> Unit) {
    val repo = Repository.get(this)
    var icon: String = existing?.icon ?: "🎯"
    var color: Int = existing?.color ?: HabitOptions.COLORS[2]
    var scheduled: Boolean = existing?.isScheduled ?: false
    var days: Int = existing?.weekdays ?: 0

    val dialog = newSheet()

    // ---------- Nome e ícone em destaque ----------

    val name = dialog.context.input("Nome da tarefa", existing?.name ?: "").apply {
        filters = arrayOf(InputFilter.LengthFilter(HabitOptions.NAME_MAX_LENGTH))
        textSize = 18f
        imeOptions = EditorInfo.IME_ACTION_DONE
    }
    val normalBg = name.background
    val errorBg = rounded(Palette.INPUT_BG, dpf(14f), Palette.DANGER, dp(2))

    val badge = TextView(this).apply {
        gravity = Gravity.CENTER
        textSize = 30f
    }

    fun styleBadge() {
        badge.text = icon
        badge.background = rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(18f))
    }

    // ---------- Dias da semana (só para a agendada) ----------

    val chips = ArrayList<TextView>()

    fun styleChips() {
        chips.forEachIndexed { i, chip ->
            val chosen = (days and TaskDays.bit(TaskDays.DISPLAY_ORDER[i])) != 0
            chip.setTextColor(if (chosen) Palette.TEXT else Palette.MUTED)
            chip.background = if (chosen) {
                rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(14f), color, dp(2))
            } else {
                rounded(Palette.INPUT_BG, dpf(14f))
            }
        }
    }

    val chipsRow = horizontal()
    TaskDays.DISPLAY_ORDER.forEachIndexed { i, day ->
        val chip = TextView(this).apply {
            text = TaskDays.SHORT_LABELS[i]
            textSize = 13f
            typeface = font(Weight.EXTRABOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(13), 0, dp(13))
            isClickable = true
            setOnClickListener {
                days = days xor TaskDays.bit(day)
                styleChips()
                popView(this)
            }
        }
        chips.add(chip)
        chipsRow.addView(chip, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(dp(2), 0, dp(2), 0))
    }

    val daysBox = vertical()
    daysBox.addView(sheetLabel("DIAS DA SEMANA"), lp().margins(t = dp(20), b = dp(6), l = dp(2)))
    daysBox.addView(chipsRow, lp())

    // ---------- Tipo ----------

    val kindHint = text("", 13f, Palette.MUTED)

    fun styleKind() {
        daysBox.visibility = if (scheduled) View.VISIBLE else View.GONE
        kindHint.text = when {
            existing != null -> {
                val what = if (scheduled) "Tarefa agendada: só aparece nos dias escolhidos, concluída ou não." else
                    "Tarefa normal: fica na lista até ser concluída."
                "$what Para mudar o tipo, exclua e crie de novo."
            }
            scheduled -> "Aparece só nos dias escolhidos. Se não for concluída, sai da lista no dia seguinte."
            else -> "Fica na lista até você concluir. Concluída, ela some no dia seguinte."
        }
    }

    val kindControl: View = if (existing == null) {
        SegmentedControl(this, listOf("Normal", "Agendada"), if (scheduled) 1 else 0) { index ->
            scheduled = index == 1
            // Primeira vez em "Agendada": já vem marcado o dia de hoje.
            if (scheduled && days == 0) days = TaskDays.bit(LocalDate.now().dayOfWeek)
            styleChips()
            styleKind()
        }
    } else {
        text(if (scheduled) "Agendada" else "Normal", 16f, Palette.TEXT, bold = true)
    }

    // ---------- Grades de ícones e de cores ----------

    val picker = IconColorPicker(
        this, icon, color,
        onIconPicked = {
            icon = it
            styleBadge()
            popView(badge)
        },
        onColorPicked = {
            color = it
            styleBadge()
            styleChips()
        }
    )

    // ---------- Formulário ----------

    val nameRow = horizontal()
    nameRow.addView(badge, lp(dp(60), dp(60)))
    nameRow.addView(name, lp(0, dp(60), 1f).margins(l = dp(12)))

    val form = sheetForm()
    form.addView(nameRow, lp())
    form.addView(sheetLabel("TIPO"), lp().margins(t = dp(22), b = dp(6), l = dp(2)))
    form.addView(kindControl, lp())
    form.addView(kindHint, lp().margins(t = dp(8), l = dp(2)))
    form.addView(daysBox, lp())
    form.addView(sheetLabel("ÍCONE"), lp().margins(t = dp(22), b = dp(4), l = dp(2)))
    form.addView(picker.iconGrid, lp())
    form.addView(sheetLabel("COR"), lp().margins(t = dp(20), b = dp(4), l = dp(2)))
    form.addView(picker.colorGrid, lp())

    styleBadge()
    styleChips()
    styleKind()

    // ---------- Ações ----------

    fun shake(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -dpf(8f), dpf(8f), -dpf(6f), dpf(6f), 0f)
            .setDuration(340)
            .start()
    }

    fun showNameError() {
        name.background = errorBg
        shake(name)
        name.requestFocus()
    }

    // Ao voltar a digitar, o campo deixa de parecer em erro.
    name.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (name.background !== normalBg) name.background = normalBg
        }
    })

    fun save() {
        val n = name.text.toString().trim()
        if (n.isEmpty()) {
            showNameError()
            return
        }
        if (scheduled && days == 0) {
            shake(chipsRow)
            Toast.makeText(this, "Escolha pelo menos um dia da semana", Toast.LENGTH_SHORT).show()
            return
        }
        val weekdays = if (scheduled) days else 0
        val base = existing ?: Task(name = n, icon = icon, color = color, weekdays = weekdays)
        repo.saveTask(base.copy(name = n, icon = icon, color = color, weekdays = weekdays))
        dialog.dismiss()
        onChanged()
    }

    name.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            save()
            true
        } else {
            false
        }
    }

    val footer = sheetFooter()
    footer.addView(button(if (existing == null) "Criar tarefa" else "Salvar alterações", BtnStyle.PRIMARY) { save() }, lp(height = dp(52)))

    if (existing != null) {
        val delete = text("Excluir tarefa", 15f, Palette.DANGER, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            background = ripple(rounded(0x00000000, dpf(14f)))
            setOnClickListener {
                val detail = if (existing.isScheduled) " Ela deixa de aparecer em todos os dias agendados." else ""
                confirm(
                    "Excluir tarefa",
                    "Excluir \"${existing.name}\"?$detail Esta ação não pode ser desfeita.",
                    "Excluir"
                ) {
                    repo.deleteTask(existing.id)
                    dialog.dismiss()
                    onChanged()
                }
            }
        }
        footer.addView(delete, lp().margins(t = dp(6)))
    }

    // Nova tarefa já abre o teclado no nome; na edição o teclado só aparece se a pessoa tocar no campo.
    showSheet(
        dialog,
        title = if (existing == null) "Nova tarefa" else "Editar tarefa",
        subtitle = if (existing == null) "Dê um nome, escolha o tipo, um ícone e uma cor." else "Ajuste o que precisar e salve.",
        form = form,
        footer = footer,
        openKeyboard = existing == null,
        focus = if (existing == null) name else null
    )
}
