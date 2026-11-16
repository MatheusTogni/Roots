package com.roots.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.roots.app.MainActivity
import com.roots.app.R
import com.roots.app.ui.Fmt
import com.roots.app.ui.MonthWindow
import com.roots.app.ui.Palette
import com.roots.app.ui.ThemeSettings
import java.time.LocalDate
import java.time.YearMonth

/**
 * Widget "Meus gráficos": um card por hábito com o calendário dos últimos 3 meses, na cor do hábito.
 * Lista rolável, redimensionável e com várias instâncias. Lê o mesmo banco do app e é atualizado
 * junto com o outro widget (WidgetUpdater.refreshAll). Tocar abre o app.
 */
class GraphWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        WidgetUpdater.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        // Cancela o alarme da meia-noite só se não sobrou nenhum widget de nenhum tipo.
        WidgetUpdater.scheduleMidnight(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.graph_list)
        WidgetUpdater.scheduleMidnight(context)
    }

    companion object {

        fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            // Tema escolhido no app. Automático (null): layout com cores de recurso, que o Android troca sozinho
            // junto com o aparelho. Claro/escuro forçado: layout *_forced, sem cor fixa, pintado abaixo com a Palette.
            val forced = ThemeSettings.forcedDark(context) != null
            val views = RemoteViews(
                context.packageName,
                if (forced) R.layout.widget_graphs_forced else R.layout.widget_graphs
            )
            if (forced) {
                Palette.use(context)
                // O filtro troca a cor da forma branca do fundo (mantendo os cantos arredondados).
                views.setInt(R.id.graph_bg_img, "setColorFilter", Palette.BG)
                views.setTextColor(R.id.graph_title, Palette.TEXT)
                views.setTextColor(R.id.graph_range, Palette.MUTED)
                views.setTextColor(R.id.graph_empty, Palette.MUTED)
            }

            // Período mostrado nos cards: o mês atual e os 2 anteriores.
            val today = LocalDate.now()
            val window = MonthWindow(YearMonth.from(today), today)
            views.setTextViewText(R.id.graph_range, Fmt.windowLabel(window.startMonth, window.endMonth))

            // Lista de cards, fornecida por HabitGraphService (uma URI única por instância).
            val serviceIntent = Intent(context, HabitGraphService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.graph_list, serviceIntent)
            views.setEmptyView(R.id.graph_list, R.id.graph_empty)

            // Tocar no cabeçalho, num card ou no aviso de lista vazia abre o app.
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.graph_header, open)
            views.setOnClickPendingIntent(R.id.graph_empty, open)

            // Modelo de clique dos cards (o fill-in exige PendingIntent mutável; o destino é explícito).
            val cardClick = PendingIntent.getActivity(
                context,
                2,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.graph_list, cardClick)
            return views
        }
    }
}
