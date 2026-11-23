package com.roots.app.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.roots.app.R
import com.roots.app.data.Repository
import com.roots.app.ui.Palette
import com.roots.app.ui.ThemeSettings
import com.roots.app.ui.overlay
import com.roots.app.ui.withAlpha
import java.time.LocalDate

/**
 * Fornece as linhas da lista do widget lendo o banco (a mesma fonte de dados do app): primeiro os hábitos do dia,
 * depois as tarefas que aparecem hoje (normais pendentes ou concluídas hoje, e agendadas para hoje).
 */
class HabitWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory =
        Factory(applicationContext)

    /** Uma linha da lista, já reduzida ao que o desenho precisa: hábito ou tarefa, com o estado de hoje. */
    private class Row(
        val id: Long,
        val icon: String,
        val name: String,
        val color: Int,
        val done: Boolean,
        val isTask: Boolean
    )

    private class Factory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

        private var rows: List<Row> = emptyList()

        /** Dia que esta lista está mostrando; viaja no clique para o provider conferir se ainda é hoje. */
        private var shownDate: String = ""

        override fun onCreate() {
            // Os dados são lidos em onDataSetChanged.
        }

        /** Chamado a cada atualização: consulta SEMPRE a data local de agora e o que foi feito nela. */
        override fun onDataSetChanged() {
            val repo = Repository.get(context)
            val today = LocalDate.now()
            shownDate = today.toString()
            val doneHabits = repo.doneOn(shownDate)
            val habitRows = repo.habits().map {
                Row(it.id, it.icon, it.name, it.color, doneHabits.contains(it.id), isTask = false)
            }
            val taskRows = repo.tasksFor(today).map {
                Row(it.task.id, it.task.icon, it.task.name, it.task.color, it.done, isTask = true)
            }
            rows = habitRows + taskRows
        }

        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= rows.size) return null
            val item = rows[position]
            val isDone = item.done

            // Tema escolhido no app: null = automático (cores de recurso, o Android troca junto com o aparelho);
            // não nulo = claro/escuro forçado (layout sem cor fixa, pintado aqui com a Palette).
            val forced = ThemeSettings.forcedDark(context) != null

            val row = RemoteViews(
                context.packageName,
                if (forced) R.layout.widget_habit_row_forced else R.layout.widget_habit_row
            )
            row.setTextViewText(R.id.row_icon, item.icon)
            row.setTextViewText(R.id.row_name, item.name)
            if (!forced) {
                // Pendente: nome em cor normal; concluído: nome apagado (a outra view, de cor mais fraca).
                row.setTextViewText(R.id.row_name_done, item.name)
                row.setViewVisibility(R.id.row_name, if (isDone) View.GONE else View.VISIBLE)
                row.setViewVisibility(R.id.row_name_done, if (isDone) View.VISIBLE else View.GONE)
                // Fundo da linha vem do recurso (claro/escuro); concluída, ganha um tom leve da cor do item.
                row.setInt(R.id.row_bg, "setColorFilter", if (isDone) withAlpha(item.color, 0x40) else 0x00FFFFFF)
            } else {
                Palette.use(context)
                row.setTextColor(R.id.row_name, if (isDone) Palette.MUTED else Palette.TEXT)
                // O filtro substitui a cor da forma branca; concluída = cor da linha com um tom do item por cima.
                val base = Palette.SURFACE
                row.setInt(R.id.row_bg, "setColorFilter", if (isDone) overlay(base, withAlpha(item.color, 0x40)) else base)
            }
            row.setImageViewBitmap(R.id.row_check, WidgetDraw.checkBitmap(context, item.color, isDone))

            // Hábito e tarefa têm ids de tabelas diferentes: cada um viaja no seu extra.
            val fillIn = Intent()
            fillIn.putExtra(if (item.isTask) HabitWidgetProvider.EXTRA_TASK_ID else HabitWidgetProvider.EXTRA_HABIT_ID, item.id)
            fillIn.putExtra(HabitWidgetProvider.EXTRA_DATE, shownDate)
            row.setOnClickFillInIntent(R.id.row_root, fillIn)
            return row
        }

        override fun getLoadingView(): RemoteViews? = null

        /** Dois modelos de linha: o automático (cores de recurso) e o do tema forçado no app. */
        override fun getViewTypeCount(): Int = 2

        /** Ids de hábito e de tarefa vêm de tabelas diferentes; as tarefas ganham um deslocamento para não colidir. */
        override fun getItemId(position: Int): Long {
            if (position < 0 || position >= rows.size) return position.toLong()
            val item = rows[position]
            return if (item.isTask) TASK_ID_BASE + item.id else item.id
        }

        override fun hasStableIds(): Boolean = true
    }

    private companion object {
        const val TASK_ID_BASE = 1L shl 40
    }
}
