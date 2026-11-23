package com.roots.app

import android.animation.ObjectAnimator
import android.app.Activity
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.roots.app.data.Habit
import com.roots.app.data.HabitOptions
import com.roots.app.data.Repository
import com.roots.app.ui.BtnStyle
import com.roots.app.ui.Fmt
import com.roots.app.ui.HabitPreviewStrip
import com.roots.app.ui.IconColorPicker
import com.roots.app.ui.Palette
import com.roots.app.ui.button
import com.roots.app.ui.confirm
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
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
import com.roots.app.ui.withAlpha

/**
 * Formulário de hábito numa folha que sobe da base da tela: criar, editar (nome, ícone e cor) e, na edição, excluir.
 * Um cartão no topo mostra ao vivo o ícone e como o calendário vai ficar com a cor escolhida.
 * Editar a cor só muda o hábito; os registros guardam apenas datas e ficam intactos.
 * [onChanged] é chamado depois de salvar ou excluir.
 */
fun Activity.showHabitDialog(existing: Habit?, onChanged: () -> Unit) {
    val repo = Repository.get(this)
    var icon: String = existing?.icon ?: "🌱"
    var color: Int = existing?.color ?: HabitOptions.COLORS[0]

    val dialog = newSheet()

    // ---------- Nome, ícone em destaque e prévia do calendário ----------

    // O campo é criado com o contexto do diálogo para herdar o tema dele (cursor e seleção).
    val name = dialog.context.input("Nome do hábito", existing?.name ?: "").apply {
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
    val strip = HabitPreviewStrip(this, color)

    fun styleBadge() {
        badge.text = icon
        badge.background = rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(18f))
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
            strip.recolor(color)
        }
    )

    // ---------- Formulário ----------

    val nameRow = horizontal()
    nameRow.addView(badge, lp(dp(60), dp(60)))
    nameRow.addView(name, lp(0, dp(60), 1f).margins(l = dp(12)))

    val caption = text("Assim ficam os quadradinhos do calendário", 12f, Palette.MUTED)

    val form = sheetForm()
    form.addView(nameRow, lp())
    form.addView(strip, lp().margins(t = dp(16)))
    form.addView(caption, lp().margins(t = dp(6), l = dp(2)))
    form.addView(sheetLabel("ÍCONE"), lp().margins(t = dp(22), b = dp(4), l = dp(2)))
    form.addView(picker.iconGrid, lp())
    form.addView(sheetLabel("COR"), lp().margins(t = dp(20), b = dp(4), l = dp(2)))
    form.addView(picker.colorGrid, lp())

    styleBadge()

    // ---------- Ações ----------

    fun showNameError() {
        name.background = errorBg
        ObjectAnimator.ofFloat(name, View.TRANSLATION_X, 0f, -dpf(8f), dpf(8f), -dpf(6f), dpf(6f), 0f)
            .setDuration(340)
            .start()
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
        val base = existing ?: Habit(name = n, icon = icon, color = color)
        repo.saveHabit(base.copy(name = n, icon = icon, color = color))
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

    // Rodapé fixo: o botão principal fica sempre à vista, mesmo com a lista rolando.
    val footer = sheetFooter()
    footer.addView(button(if (existing == null) "Criar hábito" else "Salvar alterações", BtnStyle.PRIMARY) { save() }, lp(height = dp(52)))

    if (existing != null) {
        val delete = text("Excluir hábito", 15f, Palette.DANGER, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            background = ripple(rounded(0x00000000, dpf(14f)))
            setOnClickListener {
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
        footer.addView(delete, lp().margins(t = dp(6)))
    }

    // Novo hábito já abre o teclado no nome; na edição o teclado só aparece se a pessoa tocar no campo.
    showSheet(
        dialog,
        title = if (existing == null) "Novo hábito" else "Editar hábito",
        subtitle = if (existing == null) "Dê um nome, escolha um ícone e uma cor." else "Mudar a cor não altera o histórico.",
        form = form,
        footer = footer,
        openKeyboard = existing == null,
        focus = if (existing == null) name else null
    )
}
