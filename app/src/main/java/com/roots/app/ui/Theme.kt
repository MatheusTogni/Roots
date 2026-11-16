package com.roots.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.roots.app.R

/** Cores do Roots: papel quente, texto verde-escuro e o verde "raiz" como acento. */
object Palette {
    val BG = 0xFFF6F5F0.toInt()
    val SURFACE = 0xFFFFFFFF.toInt()
    val BORDER = 0xFFECEAE3.toInt()
    val EMPTY_CELL = 0xFFE7E5DE.toInt()
    val TEXT = 0xFF1F2A24.toInt()
    val MUTED = 0xFF7B867F.toInt()
    val ACCENT = 0xFF2F7D4F.toInt()
    val ACCENT_SOFT = 0xFFE3F0E8.toInt()
    val DANGER = 0xFFD1463F.toInt()
    val INPUT_BG = 0xFFF3F2EC.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
}

/** Pesos da Nunito, a fonte do app (arquivos em res/font). */
enum class Weight(val res: Int) {
    MEDIUM(R.font.nunito_medium),
    SEMIBOLD(R.font.nunito_semibold),
    BOLD(R.font.nunito_bold),
    EXTRABOLD(R.font.nunito_extrabold),
    BLACK(R.font.nunito_black)
}

private val fonts = HashMap<Weight, Typeface>()

fun Context.font(weight: Weight): Typeface = fonts.getOrPut(weight) { resources.getFont(weight.res) }

/** Raio dos cards. */
const val CARD_RADIUS = 20f

const val INPUT_TEXT = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

/** Mesma cor com outra opacidade (0..255). */
fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0x00FFFFFF)

fun rounded(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) setStroke(strokeWidth, strokeColor)
    }

fun oval(color: Int, strokeColor: Int? = null, strokeWidth: Int = 0) =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (strokeColor != null) setStroke(strokeWidth, strokeColor)
    }

fun ripple(base: Drawable, rippleColor: Int = 0x1A000000) =
    RippleDrawable(ColorStateList.valueOf(rippleColor), base, null)

/** Encolhe um pouco a view enquanto ela está pressionada. */
fun pressScale(): StateListAnimator {
    fun scaleTo(v: Float) = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat<View>(null, View.SCALE_X, v),
            ObjectAnimator.ofFloat<View>(null, View.SCALE_Y, v)
        )
        duration = 90
    }
    return StateListAnimator().apply {
        addState(intArrayOf(android.R.attr.state_pressed), scaleTo(0.97f))
        addState(intArrayOf(), scaleTo(1f))
    }
}

fun lp(
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    weight: Float = 0f
) = LinearLayout.LayoutParams(width, height, weight)

fun LinearLayout.LayoutParams.margins(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    apply { setMargins(l, t, r, b) }

fun Context.text(
    value: String,
    size: Float = 15f,
    color: Int = Palette.TEXT,
    bold: Boolean = false
) = TextView(this).apply {
    text = value
    textSize = size
    setTextColor(color)
    typeface = font(if (bold) Weight.EXTRABOLD else Weight.MEDIUM)
}

fun Context.vertical(padding: Int = 0) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(padding, padding, padding, padding)
}

fun Context.horizontal() = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

/** Card branco de cantos arredondados com uma borda fina, sem sombra pesada. */
fun Context.card(): LinearLayout = vertical(dp(16)).apply {
    background = rounded(Palette.SURFACE, dpf(CARD_RADIUS), Palette.BORDER, dp(1))
    layoutParams = lp().margins(t = dp(6), b = dp(6))
}

enum class BtnStyle { PRIMARY, SECONDARY, OUTLINE, DANGER, GHOST }

fun Context.button(label: String, style: BtnStyle = BtnStyle.PRIMARY, onClick: () -> Unit): TextView =
    TextView(this).apply {
        text = label
        textSize = 16f
        typeface = font(Weight.EXTRABOLD)
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(12), dp(18), dp(12))
        val radius = dpf(24f)
        val (bg, fg) = when (style) {
            BtnStyle.PRIMARY -> Pair(rounded(Palette.ACCENT, radius), Palette.WHITE)
            BtnStyle.SECONDARY -> Pair(rounded(Palette.ACCENT_SOFT, radius), Palette.ACCENT)
            BtnStyle.OUTLINE -> Pair(rounded(Palette.WHITE, radius, Palette.BORDER, dp(2)), Palette.TEXT)
            BtnStyle.DANGER -> Pair(rounded(Palette.WHITE, radius, Palette.DANGER, dp(2)), Palette.DANGER)
            BtnStyle.GHOST -> Pair(rounded(0x00000000, radius), Palette.ACCENT)
        }
        background = ripple(bg)
        setTextColor(fg)
        stateListAnimator = pressScale()
        isClickable = true
        setOnClickListener { onClick() }
    }

fun Context.input(hint: String, value: String = "", type: Int = INPUT_TEXT): EditText =
    EditText(this).apply {
        this.hint = hint
        setText(value)
        inputType = type
        textSize = 16f
        typeface = font(Weight.SEMIBOLD)
        setTextColor(Palette.TEXT)
        setHintTextColor(withAlpha(Palette.MUTED, 0x99))
        // Campo claro que ganha borda verde ao receber o foco.
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), rounded(Palette.WHITE, dpf(14f), Palette.ACCENT, dp(2)))
            addState(intArrayOf(), rounded(Palette.INPUT_BG, dpf(14f), Palette.BORDER, dp(2)))
        }
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

/** Rótulo pequeno acima de um campo. */
fun Context.field(label: String, view: View): LinearLayout = vertical().apply {
    addView(text(label, 12f, Palette.MUTED, bold = true).apply { letterSpacing = 0.03f }, lp().margins(b = dp(5), l = dp(2)))
    addView(view, lp())
    layoutParams = lp().margins(t = dp(12))
}

fun Context.dialogBuilder(): AlertDialog.Builder =
    AlertDialog.Builder(this, R.style.AppDialog)

fun Context.confirm(title: String, message: String, okLabel: String, onDismiss: () -> Unit = {}, onOk: () -> Unit) {
    dialogBuilder()
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(okLabel) { _, _ -> onOk() }
        .setNegativeButton("Cancelar", null)
        .setOnDismissListener { onDismiss() }
        .show()
}

/**
 * Base das telas: fundo de papel, cabeçalho simples (título + subtítulo), conteúdo rolável
 * e botão flutuante opcional.
 */
abstract class RootsActivity : Activity() {
    protected lateinit var content: LinearLayout
    protected lateinit var scroll: ScrollView
    private lateinit var frame: FrameLayout
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView

    @Suppress("DEPRECATION")
    private fun styleSystemBars() {
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    protected fun setupScreen(title: String, subtitle: String? = null) {
        styleSystemBars()

        frame = FrameLayout(this).apply { setBackgroundColor(Palette.BG) }
        val column = vertical()

        val header = vertical().apply { setPadding(dp(22), dp(18), dp(22), dp(6)) }
        titleView = text(title, 30f, Palette.TEXT).apply { typeface = font(Weight.BLACK) }
        subtitleView = text(subtitle.orEmpty(), 14f, Palette.MUTED).apply {
            typeface = font(Weight.SEMIBOLD)
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        header.addView(titleView)
        header.addView(subtitleView)
        column.addView(header)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        content = vertical().apply { setPadding(dp(16), 0, dp(16), dp(110)) }
        scroll.addView(content)
        column.addView(scroll, lp(height = 0, weight = 1f))

        frame.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    }

    protected fun setHeader(title: String, subtitle: String? = null) {
        titleView.text = title
        subtitleView.text = subtitle.orEmpty()
        subtitleView.visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    protected fun addFab(label: String, onClick: () -> Unit) {
        val size = dp(60)
        val fab = text(label, 30f, Palette.WHITE).apply {
            typeface = font(Weight.BLACK)
            gravity = Gravity.CENTER
            background = ripple(oval(Palette.ACCENT), 0x33FFFFFF)
            elevation = dpf(6f)
            if (Build.VERSION.SDK_INT >= 28) outlineSpotShadowColor = Palette.ACCENT
            stateListAnimator = pressScale()
            setOnClickListener { onClick() }
        }
        frame.addView(fab, FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, dp(20), dp(24))
        })
    }

    /** Estado vazio simples. */
    protected fun emptyState(emoji: String, title: String, message: String) {
        content.addView(vertical(dp(24)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(text(emoji, 52f).apply { gravity = Gravity.CENTER })
            addView(text(title, 18f, Palette.TEXT, bold = true).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(8)))
            addView(text(message, 14f, Palette.MUTED).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(6)))
        }, lp().margins(t = dp(40)))
    }

    /** Título de seção em caixa alta, discreto. */
    protected fun sectionTitle(label: String) {
        content.addView(
            text(label, 12f, Palette.MUTED, bold = true).apply { letterSpacing = 0.1f },
            lp().margins(t = dp(18), b = dp(4), l = dp(6))
        )
    }
}
