package com.roots.app.ui

import java.time.LocalDate
import java.time.YearMonth

/** Textos de data e contagem em pt-BR. Nomes fixos no código para não variar entre aparelhos. */
object Fmt {
    private val MONTHS_SHORT = arrayOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    private val MONTHS_LONG = arrayOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    )
    private val WEEKDAYS_LONG = arrayOf(
        "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"
    )

    /** Letras dos dias da semana na ordem das linhas do calendário (a semana começa no domingo). */
    val WEEKDAY_LETTERS = arrayOf("D", "S", "T", "Q", "Q", "S", "S")

    fun monthShort(m: YearMonth): String = MONTHS_SHORT[m.monthValue - 1]

    /** "segunda-feira, 16 de novembro". */
    fun dayLabel(d: LocalDate): String =
        "${WEEKDAYS_LONG[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} de ${MONTHS_LONG[d.monthValue - 1]}"

    /** "16 de novembro". */
    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} de ${MONTHS_LONG[d.monthValue - 1]}"

    fun capitalize(s: String): String = s.replaceFirstChar { it.uppercase() }

    fun dayCount(n: Int): String = if (n == 1) "1 dia" else "$n dias"

    /** Período exibido: "set – nov 2026" ou, atravessando o ano, "dez 2025 – fev 2026". */
    fun windowLabel(start: YearMonth, end: YearMonth): String =
        if (start.year == end.year) {
            "${monthShort(start)} – ${monthShort(end)} ${end.year}"
        } else {
            "${monthShort(start)} ${start.year} – ${monthShort(end)} ${end.year}"
        }
}
