package com.roots.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import com.roots.app.data.Habit
import com.roots.app.data.Repository
import com.roots.app.data.TaskDays
import com.roots.app.data.TaskItem
import com.roots.app.ui.BtnStyle
import com.roots.app.ui.CARD_RADIUS
import com.roots.app.ui.Fmt
import com.roots.app.ui.HabitCalendarCard
import com.roots.app.ui.MonthWindow
import com.roots.app.ui.Palette
import com.roots.app.ui.RootsActivity
import com.roots.app.ui.SegmentedControl
import com.roots.app.ui.ThemeMode
import com.roots.app.ui.ThemeSettings
import com.roots.app.ui.ThemeTransition
import com.roots.app.ui.TodayRow
import com.roots.app.ui.button
import com.roots.app.ui.card
import com.roots.app.ui.confirm
import com.roots.app.ui.dividedCard
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
import com.roots.app.ui.emojiBadge
import com.roots.app.ui.horizontal
import com.roots.app.ui.infoRow
import com.roots.app.ui.lp
import com.roots.app.ui.margins
import com.roots.app.ui.pressScale
import com.roots.app.ui.ripple
import com.roots.app.ui.rounded
import com.roots.app.ui.text
import com.roots.app.ui.vertical
import com.roots.app.update.Updater
import java.time.LocalDate
import java.time.YearMonth

/** Abas da tela inicial. */
private enum class Tab { HABITS, TASKS }

/**
 * Tela inicial, com duas abas:
 * - Hábitos: navegação de meses (vale para todos os hábitos), a lista "Hoje" para marcar os hábitos do dia e um
 *   calendário independente para cada hábito.
 * - Tarefas: a lista "Hoje" das tarefas (normais pendentes e agendadas para hoje) e a lista das agendadas.
 */
class MainActivity : RootsActivity() {

    private val repo by lazy { Repository.get(this) }

    private var tab = Tab.HABITS
    private var selectedMonth: YearMonth = YearMonth.now()
    private var today: LocalDate = LocalDate.now()
    private val cards = HashMap<Long, HabitCalendarCard>()

    private lateinit var themeButton: TextView
    private var switchingTheme = false

    private var updateCardView: View? = null
    private var updateDismissed = false
    private var installAfterPermission = false

    /** Vira o dia com o app aberto: data, hora ou fuso mudaram. */
    private val dayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen(styleBars = true)
        Updater.check()
    }

    /** Monta cabeçalho (com o botão de tema) e botão "+". O conteúdo é desenhado por [render]. */
    private fun buildScreen(styleBars: Boolean) {
        setupScreen("Roots", Fmt.capitalize(Fmt.dayLabel(LocalDate.now())), styleBars)
        themeButton = addHeaderAction(themeGlyph(), themeDescription()) { toggleTheme() }
        // Segurar o botão volta a acompanhar o tema do aparelho.
        themeButton.setOnLongClickListener {
            useAutomaticTheme()
            true
        }
        addFab("+") { createNew() }
    }

    /** Botão "+": pergunta primeiro se é hábito ou tarefa e, depois de salvar, mostra a aba do que foi criado. */
    private fun createNew() {
        showNewItemDialog(
            onHabit = { showHabitDialog(null) { openTab(Tab.HABITS) } },
            onTask = { showTaskDialog(null) { openTab(Tab.TASKS) } }
        )
    }

    private fun openTab(target: Tab) {
        val changed = tab != target
        tab = target
        render()
        if (changed) scroll.scrollTo(0, 0)
    }

    // ---------- Tema claro / escuro ----------

    /** Mostra o que o toque vai fazer: lua no tema claro (vai para o escuro), sol no escuro (vai para o claro). */
    private fun themeGlyph(): String = if (Palette.dark) "☀️" else "🌙"

    private fun themeDescription(): String =
        if (Palette.dark) "Mudar para o tema claro" else "Mudar para o tema escuro"

    private fun toggleTheme() {
        switchTheme(if (Palette.dark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    private fun useAutomaticTheme() {
        switchTheme(ThemeMode.AUTO)
        Toast.makeText(this, "Tema automático: igual ao do aparelho", Toast.LENGTH_SHORT).show()
    }

    /**
     * Troca o tema com uma transição: tira uma foto da tela, monta a tela nova já no tema escolhido (sem
     * recriar a Activity, para não piscar) e revela o tema novo num círculo que cresce a partir do botão.
     * O widget acompanha: [ThemeSettings.set] pede o redesenho dele.
     */
    private fun switchTheme(mode: ThemeMode) {
        if (switchingTheme) return
        val wasDark = Palette.dark

        // 1) Foto da tela atual e ponto de partida do efeito (centro do botão).
        val snapshot = ThemeTransition.capture(window.decorView)
        val at = IntArray(2)
        themeButton.getLocationInWindow(at)
        val originX = at[0] + themeButton.width / 2f
        val originY = at[1] + themeButton.height / 2f
        val scrollY = scroll.scrollY

        // 2) Grava a escolha (e avisa os widgets) e resolve o tema que passa a valer.
        ThemeSettings.set(this, mode)
        Palette.use(this)
        if (Palette.dark == wasDark) {
            // Ex.: voltar ao automático quando o aparelho já está no mesmo tema: nada muda na tela.
            snapshot?.recycle()
            return
        }

        // 3) Monta a tela de novo, por baixo da foto, já com as cores novas.
        applyThemeStyle()
        buildScreen(styleBars = snapshot == null)
        render()
        // A tela nova nasce no topo; volta para onde o usuário estava rolando.
        scroll.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                scroll.scrollTo(0, scrollY)
            }
        })
        if (snapshot == null) return

        // 4) O ícone novo gira e aparece enquanto o círculo abre em cima dele.
        themeButton.alpha = 0f
        themeButton.scaleX = 0.5f
        themeButton.scaleY = 0.5f
        themeButton.rotation = -140f
        themeButton.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setStartDelay(120)
            .setDuration(560)
            .setInterpolator(OvershootInterpolator(1.3f))
            .start()

        // 5) Revela. As barras do sistema trocam quando o círculo já passou por elas.
        switchingTheme = true
        var barsDone = false
        ThemeTransition.reveal(this, snapshot, originX, originY, onProgress = { progress ->
            if (!barsDone && progress > 0.2f) {
                barsDone = true
                styleSystemBars()
            }
        }, onEnd = {
            styleSystemBars()
            switchingTheme = false
        })
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dayReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dayReceiver, filter)
        }
        Updater.listener = { refreshUpdateCard() }
        Updater.onConfirm = { startActivity(it) }
    }

    override fun onStop() {
        unregisterReceiver(dayReceiver)
        Updater.listener = null
        Updater.onConfirm = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (installAfterPermission && packageManager.canRequestPackageInstalls()) {
            installAfterPermission = false
            Updater.download(this)
        }
        // O widget pode ter alterado registros, ou o dia pode ter virado, enquanto o app estava em segundo plano.
        render()
    }

    private fun render() {
        today = LocalDate.now()
        if (selectedMonth.isAfter(YearMonth.from(today))) selectedMonth = YearMonth.from(today)
        setHeader("Roots", Fmt.capitalize(Fmt.dayLabel(today)))

        content.removeAllViews()
        cards.clear()
        updateCardView = updateCard()?.also { content.addView(it) }
        content.addView(tabSwitcher())

        if (tab == Tab.HABITS) renderHabits() else renderTasks()
    }

    /** Seletor "Hábitos | Tarefas" no topo; fica à vista mesmo quando a aba está vazia. */
    private fun tabSwitcher(): View {
        val switcher = SegmentedControl(this, listOf("Hábitos", "Tarefas"), tab.ordinal) { index ->
            tab = Tab.values()[index]
            render()
            scroll.scrollTo(0, 0)
        }
        switcher.layoutParams = lp().margins(t = dp(6), b = dp(2))
        return switcher
    }

    // ---------- Aba Hábitos ----------

    private fun renderHabits() {
        val habits = repo.habits()
        if (habits.isEmpty()) {
            emptyState(
                "🌱",
                "Nenhum hábito por aqui",
                "Crie o primeiro e acompanhe a sua constância, um dia de cada vez.",
                "Criar meu primeiro hábito"
            ) { showHabitDialog(null) { render() } }
            return
        }

        val window = MonthWindow(selectedMonth, today)
        val doneToday = repo.doneOn(today.toString())
        val doneByHabit = repo.datesByHabit(window.first.toString(), window.last.toString())

        content.addView(monthNavigator(window))

        sectionTitle("HOJE")
        content.addView(todayCard(habits, doneToday, window))

        sectionTitle("CALENDÁRIOS")
        for (habit in habits) {
            val card = HabitCalendarCard(
                this,
                habit,
                window,
                doneByHabit[habit.id] ?: emptySet(),
                onEdit = { showHabitDialog(habit) { render() } }
            )
            cards[habit.id] = card
            content.addView(card.view)
        }
    }

    // ---------- Atualização do app ----------

    /** Card no topo quando há versão nova no GitHub. Nada é baixado até o usuário tocar em "Atualizar". */
    private fun updateCard(): View? {
        val release = Updater.available ?: return null
        if (updateDismissed) return null

        val box = card()
        box.background = rounded(Palette.ACCENT_SOFT, dpf(CARD_RADIUS), Palette.ACCENT, dp(1))
        box.layoutParams = lp().margins(t = dp(4), b = dp(6))

        val head = horizontal()
        head.addView(emojiBadge("✨", Palette.ACCENT, 40))
        val texts = vertical()
        texts.addView(text("Nova versão disponível", 16f, Palette.TEXT, bold = true))
        texts.addView(text("Atualizar para a versão ${release.versionName}?", 14f, Palette.MUTED))
        head.addView(texts, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(12)))
        box.addView(head)

        if (release.notes.isNotEmpty()) {
            val notes = text(release.notes, 13f, Palette.MUTED)
            notes.maxLines = 2
            notes.ellipsize = TextUtils.TruncateAt.END
            box.addView(notes, lp().margins(t = dp(10)))
        }

        val error = Updater.error
        if (Updater.phase == Updater.Phase.FAILED && error != null) {
            box.addView(text(error, 13f, Palette.DANGER, bold = true), lp().margins(t = dp(10)))
        }

        val actions = horizontal()
        actions.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        when (Updater.phase) {
            Updater.Phase.DOWNLOADING ->
                actions.addView(text("Baixando… ${Updater.progress}%", 14f, Palette.ACCENT_TEXT, bold = true), lp(wrap).margins(r = dp(6)))
            Updater.Phase.INSTALLING -> {
                actions.addView(text("Aguardando o instalador…", 14f, Palette.ACCENT_TEXT, bold = true), lp(0, wrap, 1f))
                actions.addView(button("Tentar de novo", BtnStyle.GHOST) { startUpdate() }, lp(wrap))
            }
            else -> {
                actions.addView(button("Agora não", BtnStyle.GHOST) {
                    updateDismissed = true
                    refreshUpdateCard()
                }, lp(wrap))
                val label = if (Updater.phase == Updater.Phase.FAILED) "Tentar de novo" else "Atualizar"
                actions.addView(button(label, BtnStyle.PRIMARY) { startUpdate() }, lp(wrap).margins(l = dp(8)))
            }
        }
        box.addView(actions, lp().margins(t = dp(12)))
        return box
    }

    /** Troca só o card de atualização, sem redesenhar a tela toda (progresso do download etc.). */
    private fun refreshUpdateCard() {
        val fresh = updateCard()
        val old = updateCardView
        if (old != null && old.parent === content) {
            val index = content.indexOfChild(old)
            content.removeViewAt(index)
            if (fresh != null) content.addView(fresh, index)
        } else if (fresh != null) {
            content.addView(fresh, 0)
        }
        updateCardView = fresh
    }

    private fun startUpdate() {
        if (!packageManager.canRequestPackageInstalls()) {
            confirm(
                "Permitir atualizações",
                "Para instalar a versão nova, o Android precisa que o Roots tenha permissão para instalar apps. " +
                    "Na próxima tela, ative \"Permitir desta fonte\" e volte para o Roots.",
                "Abrir ajustes"
            ) {
                installAfterPermission = true
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            }
            return
        }
        Updater.download(this)
    }

    // ---------- Navegação de meses ----------

    private fun monthNavigator(window: MonthWindow): View {
        val row = horizontal()
        row.addView(arrowButton("‹", window.canGoBack()) {
            selectedMonth = window.endMonth.minusMonths(1)
            render()
        })
        val label = text(Fmt.windowLabel(window.startMonth, window.endMonth), 16f, Palette.TEXT, bold = true)
        label.gravity = Gravity.CENTER
        row.addView(label, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(arrowButton("›", window.canGoForward()) {
            selectedMonth = window.endMonth.plusMonths(1)
            render()
        })
        row.layoutParams = lp().margins(t = dp(4), b = dp(2))
        return row
    }

    private fun arrowButton(glyph: String, enabled: Boolean, onClick: () -> Unit): TextView {
        val button = text(glyph, 26f, if (enabled) Palette.TEXT else Palette.EMPTY_CELL, bold = true)
        button.gravity = Gravity.CENTER
        button.layoutParams = lp(dp(48), dp(44))
        if (enabled) {
            button.background = ripple(rounded(0x00000000, dpf(22f)))
            button.stateListAnimator = pressScale()
            button.setOnClickListener { onClick() }
        }
        return button
    }

    // ---------- Hoje (hábitos) ----------

    private fun todayCard(habits: List<Habit>, doneToday: Set<Long>, window: MonthWindow): View =
        dividedCard(habits.map { habit -> habitRow(habit, doneToday.contains(habit.id), window) })

    private fun habitRow(habit: Habit, done: Boolean, window: MonthWindow): View {
        val row = TodayRow(this, habit.icon, habit.color, habit.name, done)
        row.view.setOnClickListener {
            // Se o dia virou com o app aberto, a lista está velha: redesenha em vez de gravar no dia errado.
            if (LocalDate.now() != today) {
                render()
            } else {
                val nowDone = repo.toggleRecord(habit.id, today.toString())
                row.setDone(nowDone, true)
                val dates = repo.datesByHabit(window.first.toString(), window.last.toString())
                cards[habit.id]?.refresh(dates[habit.id] ?: emptySet(), if (nowDone) today else null)
            }
        }
        return row.view
    }

    // ---------- Aba Tarefas ----------

    private fun renderTasks() {
        if (repo.taskCount() == 0) {
            emptyState(
                "✅",
                "Nenhuma tarefa por aqui",
                "Crie tarefas para hoje ou agende as que se repetem toda semana.",
                "Criar minha primeira tarefa"
            ) { showTaskDialog(null) { render() } }
            return
        }

        sectionTitle("HOJE")
        val items = repo.tasksFor(today)
        if (items.isEmpty()) {
            val empty = card()
            empty.addView(
                text("Tudo em dia: nada pendente para hoje.", 15f, Palette.MUTED, bold = true).apply {
                    gravity = Gravity.CENTER
                },
                lp()
            )
            content.addView(empty)
        } else {
            content.addView(dividedCard(items.map { taskRow(it) }))
            content.addView(
                text("Toque para concluir. Segure uma tarefa para editar ou excluir.", 12f, Palette.MUTED),
                lp().margins(l = dp(6), t = dp(2))
            )
        }

        val scheduled = repo.scheduledTasks()
        if (scheduled.isNotEmpty()) {
            sectionTitle("AGENDADAS")
            content.addView(dividedCard(scheduled.map { task ->
                infoRow(task.icon, task.color, task.name, TaskDays.summary(task.weekdays)).also { row ->
                    row.setOnClickListener { showTaskDialog(task) { render() } }
                }
            }))
        }
    }

    /**
     * Linha da tarefa de hoje. Concluída, fica à vista (riscada) até o fim do dia, e pode ser desmarcada;
     * a regra de quando a tarefa some está em [Repository.tasksFor].
     */
    private fun taskRow(item: TaskItem): View {
        val task = item.task
        val row = TodayRow(this, task.icon, task.color, task.name, item.done, strikeWhenDone = true)
        row.view.setOnClickListener {
            // Mesma proteção dos hábitos: se o dia virou, a lista está velha, então só redesenha.
            if (LocalDate.now() != today) {
                render()
            } else {
                row.setDone(repo.toggleTaskRecord(task.id, today.toString()), true)
            }
        }
        row.view.setOnLongClickListener {
            showTaskDialog(task) { render() }
            true
        }
        return row.view
    }
}
