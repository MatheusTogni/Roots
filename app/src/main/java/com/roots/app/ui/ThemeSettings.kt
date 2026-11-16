package com.roots.app.ui

import android.content.Context
import com.roots.app.widget.WidgetUpdater

/** Aparência escolhida no app: acompanhar o aparelho, ou forçar o claro ou o escuro. */
enum class ThemeMode { AUTO, LIGHT, DARK }

/** Guarda a aparência escolhida (SharedPreferences, só no aparelho). O padrão é acompanhar o aparelho. */
object ThemeSettings {
    private const val PREFS = "roots_prefs"
    private const val KEY_MODE = "theme_mode"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mode(context: Context): ThemeMode {
        val saved = prefs(context).getString(KEY_MODE, null)
        return ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.AUTO
    }

    /** Grava a escolha e redesenha os widgets para que eles também mudem de tema. */
    fun set(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
        WidgetUpdater.refreshAll(context)
    }

    /**
     * null = automático (o widget usa cores de recurso e o Android troca junto com o aparelho);
     * true/false = o usuário forçou escuro/claro (o widget é pintado em código com a Palette).
     */
    fun forcedDark(context: Context): Boolean? = when (mode(context)) {
        ThemeMode.AUTO -> null
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
