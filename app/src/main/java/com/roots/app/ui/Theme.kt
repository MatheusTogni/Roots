package com.roots.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
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

/**
 * Cores do Roots. Claro: papel quente, texto verde-escuro e o verde "raiz" como acento.
 * Escuro: fundo verde quase preto com as mesmas cores de destaque, mais claras onde precisam de contraste.
 *
 * O tema vem da escolha do usuário no app ([ThemeSettings]): claro, escuro ou, no automático, o do
 * aparelho. Cada tela chama [use] antes de montar as views (o Android também recria a tela quando o tema
 * do sistema muda), e as propriedades abaixo devolvem o tom do tema ativo.
 */
object Palette {
    /** true quando o tema escuro está ativo. */
    var dark: Boolean = false
        private set

    /** Resolve o tema ativo (escolha do app ou, no automático, o do aparelho). Chamar antes de criar views. */
    fun use(context: Context) {
        dark = when (ThemeSettings.mode(context)) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> {
                val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                night == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun pick(light: Long, night: Long): Int = (if (dark) night else light).toInt()

    val BG: Int get() = pick(0xFFF6F5F0, 0xFF0F1411)
    val SURFACE: Int get() = pick(0xFFFFFFFF, 0xFF18201B)

    /** Fundo da folha do formulário (sobe da base da tela): um pouco mais clara que os cards no escuro. */
    val SHEET: Int get() = pick(0xFFFFFFFF, 0xFF1E2721)
    val BORDER: Int get() = pick(0xFFECEAE3, 0xFF28322C)
    val EMPTY_CELL: Int get() = pick(0xFFE7E5DE, 0xFF2E3A33)
    val TEXT: Int get() = pick(0xFF1F2A24, 0xFFE6EDE8)
    val MUTED: Int get() = pick(0xFF7B867F, 0xFF8C988F)
    val ACCENT: Int get() = pick(0xFF2F7D4F, 0xFF35895A)

    /** Verde para texto sobre o fundo (mais claro no escuro, para manter o contraste). */
    val ACCENT_TEXT: Int get() = pick(0xFF2F7D4F, 0xFF5FBF86)
    val ACCENT_SOFT: Int get() = pick(0xFFE3F0E8, 0xFF1E3226)
    val DANGER: Int get() = pick(0xFFD1463F, 0xFFE5655E)
    val INPUT_BG: Int get() = pick(0xFFF3F2EC, 0xFF131A16)

    /** Cor da ondulação (ripple) ao tocar: escura no claro, clara no escuro. */
    val RIPPLE: Int get() = pick(0x1A000000, 0x26FFFFFF)

    /** Opacidade do tom da cor do hábito atrás do emoji e nas seleções. */
    val TINT_ALPHA: Int get() = if (dark) 0x40 else 0x2E

    /** Branco fixo: marca de concluído e texto sobre o verde (não muda com o tema). */
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

/** Cor [tint] (com a opacidade dela) aplicada sobre [base]; o resultado é uma cor opaca. */
fun overlay(base: Int, tint: Int): Int {
    val a = (tint ushr 24) / 255f
    fun mix(shift: Int): Int {
        val b = (base shr shift) and 0xFF
        val t = (tint shr shift) and 0xFF
        return (t * a + b * (1f - a) + 0.5f).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

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

fun ripple(base: Drawable, rippleColor: Int = Palette.RIPPLE) =
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
            BtnStyle.SECONDARY -> Pair(rounded(Palette.ACCENT_SOFT, radius), Palette.ACCENT_TEXT)
            BtnStyle.OUTLINE -> Pair(rounded(Palette.SURFACE, radius, Palette.BORDER, dp(2)), Palette.TEXT)
            BtnStyle.DANGER -> Pair(rounded(Palette.SURFACE, radius, Palette.DANGER, dp(2)), Palette.DANGER)
            BtnStyle.GHOST -> Pair(rounded(0x00000000, radius), Palette.ACCENT_TEXT)
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
            addState(intArrayOf(android.R.attr.state_focused), rounded(Palette.SURFACE, dpf(14f), Palette.ACCENT, dp(2)))
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
    AlertDialog.Builder(this, if (Palette.dark) R.style.AppDialogDark else R.style.AppDialogLight)

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
    private lateinit var headerRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // O tema (claro/escuro) precisa estar definido antes de qualquer view ser criada.
        Palette.use(this)
        applyThemeStyle()
        super.onCreate(savedInstanceState)
    }

    /** Aplica à Activity o estilo claro ou escuro conforme a Palette (vale para as views criadas depois). */
    protected fun applyThemeStyle() {
        setTheme(if (Palette.dark) R.style.AppThemeDark else R.style.AppThemeLight)
    }

    /** Cor e ícones das barras de status e de navegação, de acordo com o tema ativo. */
    @Suppress("DEPRECATION")
    protected fun styleSystemBars() {
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        // Ícones escuros nas barras só no tema claro; no escuro ficam claros.
        val lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val flags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = if (Palette.dark) flags and lightBars.inv() else flags or lightBars
    }

    /**
     * Monta a tela. Pode ser chamada de novo (troca de tema no lugar): substitui o conteúdo por uma tela nova.
     * [styleBars] = false deixa as barras do sistema como estão, para a transição de tema trocá-las no meio.
     */
    protected fun setupScreen(title: String, subtitle: String? = null, styleBars: Boolean = true) {
        if (styleBars) styleSystemBars()
        window.setBackgroundDrawable(ColorDrawable(Palette.BG))

        frame = FrameLayout(this).apply { setBackgroundColor(Palette.BG) }
        val column = vertical()

        // Cabeçalho: título e subtítulo à esquerda; botões de ação (addHeaderAction) à direita.
        headerRow = horizontal().apply { setPadding(dp(22), dp(18), dp(14), dp(6)) }
        val headerText = vertical()
        titleView = text(title, 30f, Palette.TEXT).apply { typeface = font(Weight.BLACK) }
        subtitleView = text(subtitle.orEmpty(), 14f, Palette.MUTED).apply {
            typeface = font(Weight.SEMIBOLD)
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        headerText.addView(titleView)
        headerText.addView(subtitleView)
        headerRow.addView(headerText, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(headerRow)

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

    /** Botão redondo no canto direito do cabeçalho. [glyph] costuma ser um emoji. Devolve o botão criado. */
    protected fun addHeaderAction(glyph: String, description: String, onClick: () -> Unit): TextView {
        val button = text(glyph, 22f).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            background = ripple(oval(0x00000000))
            stateListAnimator = pressScale()
            isClickable = true
            setOnClickListener { onClick() }
        }
        headerRow.addView(button, lp(dp(44), dp(44)))
        return button
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

    /** Estado vazio simples, com um botão de ação opcional ([actionLabel] + [onAction]). */
    protected fun emptyState(
        emoji: String,
        title: String,
        message: String,
        actionLabel: String? = null,
        onAction: () -> Unit = {}
    ) {
        content.addView(vertical(dp(24)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(text(emoji, 52f).apply { gravity = Gravity.CENTER })
            addView(text(title, 18f, Palette.TEXT, bold = true).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(8)))
            addView(text(message, 14f, Palette.MUTED).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(6)))
            if (actionLabel != null) {
                addView(button(actionLabel, BtnStyle.PRIMARY) { onAction() }, lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(t = dp(22)))
            }
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
