package com.roots.app

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.roots.app.data.Habit
import com.roots.app.data.HabitOptions
import com.roots.app.data.Repository
import com.roots.app.ui.BtnStyle
import com.roots.app.ui.Fmt
import com.roots.app.ui.HabitPreviewStrip
import com.roots.app.ui.Palette
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
import com.roots.app.ui.oval
import com.roots.app.ui.ripple
import com.roots.app.ui.rounded
import com.roots.app.ui.text
import com.roots.app.ui.vertical
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

    val dialog = Dialog(this, if (Palette.dark) R.style.AppSheetDark else R.style.AppSheetLight)

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

    /** Pequeno "pulo" ao escolher algo, para a seleção responder ao toque. */
    fun pop(view: View) {
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(OvershootInterpolator(2.5f)).start()
    }

    // ---------- Grades de ícones e de cores ----------

    val iconCells = ArrayList<TextView>()
    val colorHolders = ArrayList<LinearLayout>()
    val colorMarks = ArrayList<TextView>()

    fun styleIcons() {
        iconCells.forEachIndexed { i, cell ->
            val chosen = HabitOptions.ICONS[i] == icon
            cell.background = if (chosen) {
                rounded(withAlpha(color, Palette.TINT_ALPHA), dpf(14f), color, dp(2))
            } else {
                rounded(Palette.INPUT_BG, dpf(14f))
            }
        }
    }

    fun styleColors() {
        for (i in colorHolders.indices) {
            val chosen = HabitOptions.COLORS[i] == color
            // Escolhida: anel na própria cor em volta do círculo e um ✓ dentro.
            colorHolders[i].background = if (chosen) oval(0x00000000, HabitOptions.COLORS[i], dp(3)) else null
            colorMarks[i].text = if (chosen) "✓" else ""
        }
    }

    /** Monta uma grade de [count] células em linhas de [columns] colunas iguais. */
    fun grid(count: Int, columns: Int, cellBuilder: (Int) -> View): LinearLayout {
        val box = vertical()
        var start = 0
        while (start < count) {
            val row = horizontal()
            for (c in 0 until columns) {
                val index = start + c
                if (index < count) {
                    row.addView(
                        cellBuilder(index),
                        lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(dp(3), dp(3), dp(3), dp(3))
                    )
                } else {
                    row.addView(View(this), lp(0, 1, 1f))
                }
            }
            box.addView(row, lp())
            start += columns
        }
        return box
    }

    val iconGrid = grid(HabitOptions.ICONS.size, 6) { index ->
        val cell = text(HabitOptions.ICONS[index], 22f).apply {
            gravity = Gravity.CENTER
            minHeight = dp(48)
            isClickable = true
        }
        cell.setOnClickListener {
            icon = HabitOptions.ICONS[index]
            styleIcons()
            styleBadge()
            pop(badge)
        }
        iconCells.add(cell)
        cell
    }

    val colorGrid = grid(HabitOptions.COLORS.size, 5) { index ->
        val circle = TextView(this).apply {
            textSize = 16f
            typeface = font(Weight.BLACK)
            setTextColor(Palette.WHITE)
            gravity = Gravity.CENTER
            background = oval(HabitOptions.COLORS[index])
        }
        // Caixa quadrada fixa: o anel de seleção é desenhado nela, então fica sempre redondo
        // (a célula da grade é mais larga que alta) e a seleção não muda o tamanho da grade.
        val ring = vertical().apply {
            gravity = Gravity.CENTER
            addView(circle, lp(dp(34), dp(34)))
        }
        val holder = vertical().apply {
            gravity = Gravity.CENTER
            isClickable = true
            addView(ring, lp(dp(46), dp(46)))
            setOnClickListener {
                color = HabitOptions.COLORS[index]
                styleColors()
                styleIcons()
                styleBadge()
                strip.recolor(color)
                pop(circle)
            }
        }
        colorHolders.add(ring)
        colorMarks.add(circle)
        holder
    }

    fun label(value: String) = text(value, 12f, Palette.MUTED, bold = true).apply { letterSpacing = 0.1f }

    // ---------- Monta a folha ----------

    val radius = dpf(28f)
    val sheet = vertical().apply {
        background = GradientDrawable().apply {
            setColor(Palette.SHEET)
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
    }

    // Alça + título (fixos no topo).
    val handle = View(this).apply { background = rounded(Palette.EMPTY_CELL, dpf(2f)) }
    sheet.addView(handle, lp(dp(40), dp(4)).margins(t = dp(10)).apply { gravity = Gravity.CENTER_HORIZONTAL })

    val title = text(if (existing == null) "Novo hábito" else "Editar hábito", 24f, Palette.TEXT).apply {
        typeface = font(Weight.BLACK)
    }
    val close = text("×", 28f, Palette.MUTED).apply {
        gravity = Gravity.CENTER
        contentDescription = "Fechar"
        isClickable = true
        background = ripple(oval(0x00000000))
        setOnClickListener { dialog.dismiss() }
    }
    val titleRow = horizontal().apply { setPadding(dp(22), dp(8), dp(12), 0) }
    titleRow.addView(title, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    titleRow.addView(close, lp(dp(40), dp(40)))
    sheet.addView(titleRow, lp())

    val subtitle = text(
        if (existing == null) "Dê um nome, escolha um ícone e uma cor." else "Mudar a cor não altera o histórico.",
        14f,
        Palette.MUTED
    ).apply { typeface = font(Weight.SEMIBOLD) }
    sheet.addView(subtitle, lp().margins(l = dp(22), r = dp(22), t = dp(2)))

    // Formulário rolável (para telas baixas e quando o teclado abre).
    val nameRow = horizontal()
    nameRow.addView(badge, lp(dp(60), dp(60)))
    nameRow.addView(name, lp(0, dp(60), 1f).margins(l = dp(12)))

    val caption = text("Assim ficam os quadradinhos do calendário", 12f, Palette.MUTED)

    val form = vertical().apply { setPadding(dp(22), dp(14), dp(22), dp(8)) }
    form.addView(nameRow, lp())
    form.addView(strip, lp().margins(t = dp(16)))
    form.addView(caption, lp().margins(t = dp(6), l = dp(2)))
    form.addView(label("ÍCONE"), lp().margins(t = dp(22), b = dp(4), l = dp(2)))
    form.addView(iconGrid, lp())
    form.addView(label("COR"), lp().margins(t = dp(20), b = dp(4), l = dp(2)))
    form.addView(colorGrid, lp())

    val scroll = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(form)
    }
    sheet.addView(scroll, lp(height = 0, weight = 1f))

    styleBadge()
    styleIcons()
    styleColors()

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
    val footer = vertical().apply { setPadding(dp(22), dp(10), dp(22), dp(16)) }
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
    sheet.addView(footer, lp())

    // ---------- Janela: colada na base, largura total, acima do teclado ----------

    dialog.setContentView(sheet)
    dialog.window?.let { window ->
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Novo hábito já abre o teclado no nome; na edição o teclado só aparece se a pessoa tocar no campo.
        val keyboard = if (existing == null) {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        }
        window.setSoftInputMode(keyboard or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    // Entrada: a folha sobe da base da tela assim que é medida (antes do primeiro desenho, sem piscar).
    sheet.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            sheet.removeOnLayoutChangeListener(this)
            sheet.translationY = sheet.height.toFloat()
            sheet.animate().translationY(0f).setDuration(340).setInterpolator(DecelerateInterpolator(1.8f)).start()
        }
    })

    if (existing == null) name.requestFocus()
    dialog.show()
}
