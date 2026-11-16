package com.roots.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.roots.app.R
import com.roots.app.data.Habit
import com.roots.app.data.Repository
import com.roots.app.ui.Palette
import com.roots.app.ui.withAlpha
import java.time.LocalDate

/** Fornece as linhas da lista do widget lendo o banco (a mesma fonte de dados do app). */
class HabitWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

        private var habits: List<Habit> = emptyList()
        private var done: Set<Long> = emptySet()

        /** Dia que esta lista está mostrando; viaja no clique para o provider conferir se ainda é hoje. */
        private var shownDate: String = ""

        override fun onCreate() {
            // Os dados são lidos em onDataSetChanged.
        }

        /** Chamado a cada atualização: consulta SEMPRE a data local de agora e o que foi feito nela. */
        override fun onDataSetChanged() {
            val repo = Repository.get(context)
            shownDate = LocalDate.now().toString()
            habits = repo.habits()
            done = repo.doneOn(shownDate)
        }

        override fun onDestroy() {
            habits = emptyList()
            done = emptySet()
        }

        override fun getCount(): Int = habits.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= habits.size) return null
            val habit = habits[position]
            val isDone = done.contains(habit.id)

            val row = RemoteViews(context.packageName, R.layout.widget_habit_row)
            row.setTextViewText(R.id.row_icon, habit.icon)
            row.setTextViewText(R.id.row_name, habit.name)
            row.setTextColor(R.id.row_name, if (isDone) Palette.MUTED else Palette.TEXT)
            // Linha branca; concluída, ganha um tom leve da cor do hábito.
            row.setInt(R.id.row_bg, "setColorFilter", if (isDone) withAlpha(habit.color, 0x38) else 0x00FFFFFF)
            row.setImageViewBitmap(R.id.row_check, WidgetDraw.checkBitmap(context, habit.color, isDone))

            val fillIn = Intent()
            fillIn.putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
            fillIn.putExtra(HabitWidgetProvider.EXTRA_DATE, shownDate)
            row.setOnClickFillInIntent(R.id.row_root, fillIn)
            return row
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long =
            if (position >= 0 && position < habits.size) habits[position].id else position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
