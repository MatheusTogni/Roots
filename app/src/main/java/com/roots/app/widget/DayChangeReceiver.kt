package com.roots.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Recebe a virada do dia (alarme de meia-noite) e tudo que pode mudar a data do aparelho:
 * ajuste de data/hora, troca de fuso, boot e atualização do app. Em todos os casos os widgets
 * são redesenhados com a data local atual e o alarme da próxima meia-noite é reagendado.
 * Nada é apagado: cada registro pertence a uma data, então o dia novo simplesmente aparece pendente.
 */
class DayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.refreshAll(context)
        WidgetUpdater.scheduleMidnight(context)
    }

    companion object {
        const val ACTION_NEW_DAY = "com.roots.app.action.NEW_DAY"
    }
}
