package com.roots.app.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.roots.app.R
import com.roots.app.data.HabitOptions

/*
 * Peças compartilhadas pelos formulários que sobem da base da tela (hábito, tarefa e a escolha entre os dois):
 * a casca da folha, os blocos de formulário e o seletor de ícone e cor.
 */

/** Dialog da folha, já no tema claro/escuro. Crie campos de texto com `dialog.context` para herdarem o tema dele. */
fun Activity.newSheet(): Dialog = Dialog(this, if (Palette.dark) R.style.AppSheetDark else R.style.AppSheetLight)

/** Área rolável do formulário, com o recuo padrão da folha. */
fun Context.sheetForm(): LinearLayout = vertical().apply { setPadding(dp(22), dp(14), dp(22), dp(8)) }

/** Rodapé fixo da folha (o botão principal fica sempre à vista). */
fun Context.sheetFooter(): LinearLayout = vertical().apply { setPadding(dp(22), dp(10), dp(22), dp(16)) }

/** Rótulo pequeno em caixa alta que abre cada bloco do formulário. */
fun Context.sheetLabel(value: String): TextView =
    text(value, 12f, Palette.MUTED, bold = true).apply { letterSpacing = 0.1f }

/** Pequeno "pulo" ao escolher algo, para a seleção responder ao toque. */
fun popView(view: View) {
    view.scaleX = 0.82f
    view.scaleY = 0.82f
    view.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(OvershootInterpolator(2.5f)).start()
}

/**
 * Monta e mostra a folha em [dialog]: alça, título com ×, subtítulo, [form] rolável (para telas baixas e quando o
 * teclado abre) e [footer] fixo. A folha cola na base, ocupa a largura toda, fica acima do teclado e sobe com uma
 * animação assim que é medida (antes do primeiro desenho, sem piscar).
 * [openKeyboard] abre o teclado já; [focus] é o campo que recebe o foco ao abrir.
 */
fun Activity.showSheet(
    dialog: Dialog,
    title: String,
    subtitle: String,
    form: View,
    footer: View,
    openKeyboard: Boolean = false,
    focus: View? = null
) {
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

    val titleView = text(title, 24f, Palette.TEXT).apply { typeface = font(Weight.BLACK) }
    val close = text("×", 28f, Palette.MUTED).apply {
        gravity = Gravity.CENTER
        contentDescription = "Fechar"
        isClickable = true
        background = ripple(oval(0x00000000))
        setOnClickListener { dialog.dismiss() }
    }
    val titleRow = horizontal().apply { setPadding(dp(22), dp(8), dp(12), 0) }
    titleRow.addView(titleView, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    titleRow.addView(close, lp(dp(40), dp(40)))
    sheet.addView(titleRow, lp())

    val subtitleView = text(subtitle, 14f, Palette.MUTED).apply { typeface = font(Weight.SEMIBOLD) }
    sheet.addView(subtitleView, lp().margins(l = dp(22), r = dp(22), t = dp(2)))

    val scroll = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(form)
    }
    sheet.addView(scroll, lp(height = 0, weight = 1f))
    sheet.addView(footer, lp())

    // Janela: colada na base, largura total, acima do teclado.
    dialog.setContentView(sheet)
    dialog.window?.let { window ->
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val keyboard = if (openKeyboard) {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        }
        window.setSoftInputMode(keyboard or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    // Entrada: a folha sobe da base da tela assim que é medida.
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

    focus?.requestFocus()
    dialog.show()
}

/**
 * Grades de ícones e de cores dos formulários, com a seleção destacada. Os callbacks avisam qual ícone ou cor
 * foi tocado (o formulário atualiza a sua prévia); o destaque das grades o próprio seletor refaz.
 */
class IconColorPicker(
    private val activity: Activity,
    private var icon: String,
    private var color: Int,
    private val onIconPicked: (String) -> Unit,
    private val onColorPicked: (Int) -> Unit
) {
    private val iconCells = ArrayList<TextView>()
    private val colorRings = ArrayList<LinearLayout>()
    private val colorMarks = ArrayList<TextView>()

    val iconGrid: View = grid(HabitOptions.ICONS.size, 6) { index ->
        val cell = activity.text(HabitOptions.ICONS[index], 22f).apply {
            gravity = Gravity.CENTER
            minHeight = activity.dp(48)
            isClickable = true
        }
        cell.setOnClickListener {
            icon = HabitOptions.ICONS[index]
            styleIcons()
            onIconPicked(icon)
        }
        iconCells.add(cell)
        cell
    }

    val colorGrid: View = grid(HabitOptions.COLORS.size, 5) { index ->
        val circle = TextView(activity).apply {
            textSize = 16f
            typeface = activity.font(Weight.BLACK)
            setTextColor(Palette.WHITE)
            gravity = Gravity.CENTER
            background = oval(HabitOptions.COLORS[index])
        }
        // Caixa quadrada fixa: o anel de seleção é desenhado nela, então fica sempre redondo
        // (a célula da grade é mais larga que alta) e a seleção não muda o tamanho da grade.
        val ring = activity.vertical().apply {
            gravity = Gravity.CENTER
            addView(circle, lp(activity.dp(34), activity.dp(34)))
        }
        val holder = activity.vertical().apply {
            gravity = Gravity.CENTER
            isClickable = true
            addView(ring, lp(activity.dp(46), activity.dp(46)))
            setOnClickListener {
                color = HabitOptions.COLORS[index]
                styleColors()
                styleIcons()
                popView(circle)
                onColorPicked(color)
            }
        }
        colorRings.add(ring)
        colorMarks.add(circle)
        holder
    }

    init {
        styleIcons()
        styleColors()
    }

    /** Monta uma grade de [count] células em linhas de [columns] colunas iguais. */
    private fun grid(count: Int, columns: Int, cellBuilder: (Int) -> View): LinearLayout {
        val box = activity.vertical()
        var start = 0
        while (start < count) {
            val row = activity.horizontal()
            for (c in 0 until columns) {
                val index = start + c
                if (index < count) {
                    row.addView(
                        cellBuilder(index),
                        lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            .margins(activity.dp(3), activity.dp(3), activity.dp(3), activity.dp(3))
                    )
                } else {
                    row.addView(View(activity), lp(0, 1, 1f))
                }
            }
            box.addView(row, lp())
            start += columns
        }
        return box
    }

    private fun styleIcons() {
        iconCells.forEachIndexed { i, cell ->
            val chosen = HabitOptions.ICONS[i] == icon
            cell.background = if (chosen) {
                rounded(withAlpha(color, Palette.TINT_ALPHA), activity.dpf(14f), color, activity.dp(2))
            } else {
                rounded(Palette.INPUT_BG, activity.dpf(14f))
            }
        }
    }

    private fun styleColors() {
        for (i in colorRings.indices) {
            val chosen = HabitOptions.COLORS[i] == color
            // Escolhida: anel na própria cor em volta do círculo e um ✓ dentro.
            colorRings[i].background = if (chosen) oval(0x00000000, HabitOptions.COLORS[i], activity.dp(3)) else null
            colorMarks[i].text = if (chosen) "✓" else ""
        }
    }
}
