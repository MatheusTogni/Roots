package com.roots.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.roots.app.data.Habit
import com.roots.app.data.Repository
import com.roots.app.ui.CheckCircle
import com.roots.app.ui.Fmt
import com.roots.app.ui.HabitCalendarCard
import com.roots.app.ui.MonthWindow
import com.roots.app.ui.Palette
import com.roots.app.ui.RootsActivity
import com.roots.app.ui.card
import com.roots.app.ui.dp
import com.roots.app.ui.dpf
import com.roots.app.ui.emojiBadge
import com.roots.app.ui.horizontal
import com.roots.app.ui.lp
import com.roots.app.ui.margins
import com.roots.app.ui.pressScale
import com.roots.app.ui.ripple
import com.roots.app.ui.rounded
import com.roots.app.ui.text
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tela inicial: navegação de meses (vale para todos os hábitos), a lista "Hoje" para marcar
 * os hábitos do dia e um calendário independente para cada hábito.
 */
class MainActivity : RootsActivity() {

    private val repo by lazy { Repository.get(this) }

    private var selectedMonth: YearMonth = YearMonth.now()
    private var today: LocalDate = LocalDate.now()
    private val cards = HashMap<Long, HabitCalendarCard>()

    /** Vira o dia com o app aberto: data, hora ou fuso mudaram. */
    private val dayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Roots", Fmt.capitalize(Fmt.dayLabel(LocalDate.now())))
        addFab("+") { showHabitDialog(null) { render() } }
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
    }

    override fun onStop() {
        unregisterReceiver(dayReceiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // O widget pode ter alterado registros, ou o dia pode ter virado, enquanto o app estava em segundo plano.
        render()
    }

    private fun render() {
        today = LocalDate.now()
        if (selectedMonth.isAfter(YearMonth.from(today))) selectedMonth = YearMonth.from(today)
        setHeader("Roots", Fmt.capitalize(Fmt.dayLabel(today)))

        content.removeAllViews()
        cards.clear()

        val habits = repo.habits()
        if (habits.isEmpty()) {
            emptyState("🌱", "Nenhum hábito por aqui", "Toque no + para criar o primeiro.")
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

    // ---------- Hoje ----------

    private fun todayCard(habits: List<Habit>, doneToday: Set<Long>, window: MonthWindow): View {
        val box = card()
        box.setPadding(dp(6), dp(6), dp(6), dp(6))
        habits.forEachIndexed { index, habit ->
            box.addView(habitRow(habit, doneToday.contains(habit.id), window))
            if (index < habits.lastIndex) {
                val divider = View(this)
                divider.setBackgroundColor(Palette.BORDER)
                box.addView(divider, lp(height = dp(1)).margins(l = dp(62), r = dp(10)))
            }
        }
        return box
    }

    private fun habitRow(habit: Habit, done: Boolean, window: MonthWindow): View {
        val row: LinearLayout = horizontal()
        row.setPadding(dp(10), dp(8), dp(12), dp(8))
        row.background = ripple(rounded(0x00000000, dpf(14f)))
        row.isClickable = true

        row.addView(emojiBadge(habit.icon, habit.color, 40))
        val name = text(habit.name, 17f, Palette.TEXT, bold = true)
        name.maxLines = 1
        name.ellipsize = android.text.TextUtils.TruncateAt.END
        row.addView(name, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(12), r = dp(8)))
        val check = CheckCircle(this, habit.color)
        check.setChecked(done, false)
        row.addView(check, lp(dp(30), dp(30)))

        row.setOnClickListener {
            // Se o dia virou com o app aberto, a lista está velha: redesenha em vez de gravar no dia errado.
            if (LocalDate.now() != today) {
                render()
            } else {
                val nowDone = repo.toggleRecord(habit.id, today.toString())
                check.setChecked(nowDone, true)
                val dates = repo.datesByHabit(window.first.toString(), window.last.toString())
                cards[habit.id]?.refresh(dates[habit.id] ?: emptySet(), if (nowDone) today else null)
            }
        }
        return row
    }
}
