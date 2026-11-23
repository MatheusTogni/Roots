package com.roots.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.roots.app.R
import java.time.LocalDate
import java.time.ZoneId

/**
 * Atualização dos widgets: redesenho de todas as instâncias e o alarme da próxima meia-noite.
 * O banco continua sendo a única fonte de dados; aqui só se pede aos widgets que releiam.
 */
object WidgetUpdater {

    /** Instâncias do widget "Hábitos e tarefas" (checklist do dia). */
    private fun habitWidgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))

    /** Instâncias do widget "Meus gráficos" (calendários). */
    private fun graphWidgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, GraphWidgetProvider::class.java))

    /** Existe algum widget do app na tela inicial? */
    private fun hasWidgets(context: Context): Boolean =
        habitWidgetIds(context).isNotEmpty() || graphWidgetIds(context).isNotEmpty()

    /** Redesenha todas as instâncias dos dois widgets (cabeçalho e lista) com o que está no banco agora. */
    fun refreshAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)

        val habitIds = habitWidgetIds(app)
        if (habitIds.isNotEmpty()) {
            for (id in habitIds) {
                manager.updateAppWidget(id, HabitWidgetProvider.buildViews(app, id))
            }
            manager.notifyAppWidgetViewDataChanged(habitIds, R.id.habit_list)
        }

        val graphIds = graphWidgetIds(app)
        if (graphIds.isNotEmpty()) {
            for (id in graphIds) {
                manager.updateAppWidget(id, GraphWidgetProvider.buildViews(app, id))
            }
            manager.notifyAppWidgetViewDataChanged(graphIds, R.id.graph_list)
        }
    }

    private fun midnightIntent(context: Context): PendingIntent {
        val intent = Intent(context, DayChangeReceiver::class.java)
        intent.action = DayChangeReceiver.ACTION_NEW_DAY
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Agenda um alarme único (inexato, sem permissão especial) logo depois da próxima meia-noite local.
     * Sem nenhum widget instalado (de nenhum dos dois tipos) cancela o alarme em vez de agendar.
     * O horário vem da data e do fuso do aparelho no momento da chamada.
     */
    fun scheduleMidnight(context: Context) {
        val app = context.applicationContext
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!hasWidgets(app)) {
            alarms.cancel(midnightIntent(app))
            return
        }
        val nextDay = LocalDate.now().plusDays(1)
        val triggerAt = nextDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 2000L
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, midnightIntent(app))
    }
}
