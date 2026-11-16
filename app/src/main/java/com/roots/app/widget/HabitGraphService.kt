package com.roots.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.roots.app.R
import com.roots.app.data.Habit
import com.roots.app.data.Repository
import com.roots.app.ui.Fmt
import com.roots.app.ui.MonthWindow
import com.roots.app.ui.Palette
import com.roots.app.ui.ThemeSettings
import java.time.LocalDate
import java.time.YearMonth

/** Fornece os cards do widget "Meus gráficos": um por hábito, com o calendário dos últimos 3 meses. */
class HabitGraphService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

        private var habits: List<Habit> = emptyList()
        private var done: Map<Long, Set<String>> = emptyMap()
        private var window: MonthWindow = MonthWindow(YearMonth.now(), LocalDate.now())

        override fun onCreate() {
            // Os dados são lidos em onDataSetChanged.
        }

        /** Chamado a cada atualização: a janela vem da data de agora e os registros vêm do banco. */
        override fun onDataSetChanged() {
            val repo = Repository.get(context)
            val today = LocalDate.now()
            window = MonthWindow(YearMonth.from(today), today)
            habits = repo.habits()
            done = repo.datesByHabit(window.first.toString(), window.last.toString())
        }

        override fun onDestroy() {
            habits = emptyList()
            done = emptyMap()
        }

        override fun getCount(): Int = habits.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= habits.size) return null
            val habit = habits[position]
            val dates = done[habit.id] ?: emptySet()

            // Tema escolhido no app: null = automático (layout com cores de recurso, que o Android troca
            // junto com o aparelho); não nulo = claro/escuro forçado (layout sem cor fixa, pintado aqui).
            val forced = ThemeSettings.forcedDark(context) != null

            val card = RemoteViews(
                context.packageName,
                if (forced) R.layout.widget_graph_card_forced else R.layout.widget_graph_card
            )
            card.setTextViewText(R.id.card_icon, habit.icon)
            card.setTextViewText(R.id.card_name, habit.name)
            card.setTextViewText(R.id.card_count, Fmt.dayCount(dates.size))
            card.setImageViewBitmap(R.id.card_graph, GraphDraw.render(window, dates, habit.color))
            if (forced) {
                Palette.use(context)
                card.setInt(R.id.card_bg, "setColorFilter", Palette.SURFACE)
                card.setTextColor(R.id.card_name, Palette.TEXT)
                card.setTextColor(R.id.card_count, Palette.MUTED)
            }

            // Tocar no card abre o app (o modelo do clique está no PendingIntent da lista).
            card.setOnClickFillInIntent(R.id.card_root, Intent())
            return card
        }

        override fun getLoadingView(): RemoteViews? = null

        /** Dois modelos de card: o automático (cores de recurso) e o do tema forçado no app. */
        override fun getViewTypeCount(): Int = 2

        override fun getItemId(position: Int): Long =
            if (position >= 0 && position < habits.size) habits[position].id else position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
