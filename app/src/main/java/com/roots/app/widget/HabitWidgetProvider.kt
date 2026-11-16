package com.roots.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.roots.app.MainActivity
import com.roots.app.R
import com.roots.app.data.Repository
import com.roots.app.ui.Fmt
import java.time.LocalDate

/**
 * Widget "Meus hábitos": lista os hábitos ativos do dia e permite marcar/desmarcar direto da tela inicial.
 * Pode haver várias instâncias; todas leem o mesmo banco e são atualizadas juntas.
 */
class HabitWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        WidgetUpdater.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.cancelMidnight(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.habit_list)
        WidgetUpdater.scheduleMidnight(context)
    }

    /** O usuário redimensionou o widget: troca entre o layout com cabeçalho e o compacto. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.habit_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
            val shownDate = intent.getStringExtra(EXTRA_DATE)
            // A data é sempre consultada agora, no momento do toque.
            val today = LocalDate.now().toString()
            if (habitId != 0L && shownDate == today) {
                // Grava no SQLite; o Repository avisa todos os widgets (e o app relê no onResume).
                Repository.get(context).toggleRecord(habitId, today)
            } else {
                // O widget ainda mostrava outro dia (virada recente): só atualiza, sem gravar no dia errado.
                WidgetUpdater.refreshAll(context)
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.roots.app.action.TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_DATE = "date"

        /** Widgets mais baixos que isto (em dp) usam o layout compacto, sem cabeçalho. */
        private const val COMPACT_MAX_HEIGHT_DP = 90

        fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            val manager = AppWidgetManager.getInstance(context)
            val options = manager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val compact = minHeight in 1..COMPACT_MAX_HEIGHT_DP

            val views = RemoteViews(
                context.packageName,
                if (compact) R.layout.widget_habits_compact else R.layout.widget_habits
            )

            // Lista de hábitos, fornecida por HabitWidgetService (uma URI única por instância).
            val serviceIntent = Intent(context, HabitWidgetService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.habit_list, serviceIntent)
            views.setEmptyView(R.id.habit_list, R.id.empty_view)

            // Toque numa linha: broadcast explícito para este provider (os extras vêm de cada linha).
            val toggle = Intent(context, HabitWidgetProvider::class.java)
            toggle.action = ACTION_TOGGLE
            val template = PendingIntent.getBroadcast(
                context,
                0,
                toggle,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.habit_list, template)

            // Abrir o app: toque no cabeçalho (ou no aviso de lista vazia).
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.empty_view, open)
            if (!compact) {
                views.setTextViewText(R.id.widget_date, Fmt.dayMonth(LocalDate.now()))
                views.setOnClickPendingIntent(R.id.header, open)
            }
            return views
        }
    }
}
