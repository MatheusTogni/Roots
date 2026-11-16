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

    private fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))

    /** Redesenha todas as instâncias (cabeçalho e lista) com o que está no banco agora. */
    fun refreshAll(context: Context) {
        val app = context.applicationContext
        val ids = widgetIds(app)
        if (ids.isEmpty()) return
        val manager = AppWidgetManager.getInstance(app)
        for (id in ids) {
            manager.updateAppWidget(id, HabitWidgetProvider.buildViews(app, id))
        }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.habit_list)
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
     * Sem widgets instalados não agenda nada. O horário vem da data e do fuso do aparelho no momento da chamada.
     */
    fun scheduleMidnight(context: Context) {
        val app = context.applicationContext
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (widgetIds(app).isEmpty()) {
            alarms.cancel(midnightIntent(app))
            return
        }
        val nextDay = LocalDate.now().plusDays(1)
        val triggerAt = nextDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 2000L
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, midnightIntent(app))
    }

    fun cancelMidnight(context: Context) {
        val app = context.applicationContext
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(midnightIntent(app))
    }
}
