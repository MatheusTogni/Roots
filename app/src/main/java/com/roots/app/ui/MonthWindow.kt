package com.roots.app.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Janela de meses de um calendário: o mês selecionado e os [MONTHS] - 1 anteriores.
 * As semanas viram colunas (começando em [WEEK_START]) e os dias da semana viram as 7 linhas.
 * Matemática pura com java.time, sem Android: ano bissexto e virada de mês/ano vêm da biblioteca.
 */
class MonthWindow(selected: YearMonth, val today: LocalDate) {

    /** Último mês da janela: o selecionado, nunca depois do mês de hoje. */
    val endMonth: YearMonth =
        if (selected.isAfter(YearMonth.from(today))) YearMonth.from(today) else selected

    val startMonth: YearMonth = endMonth.minusMonths((MONTHS - 1).toLong())

    /** Primeiro dia exibido (dia 1 do primeiro mês). */
    val first: LocalDate = startMonth.atDay(1)

    /** Último dia exibido: fim do último mês, mas nunca depois de hoje (não há datas futuras). */
    val last: LocalDate =
        if (endMonth.atEndOfMonth().isAfter(today)) today else endMonth.atEndOfMonth()

    /** Dia que abre a primeira coluna: o início da semana que contém [first]. */
    val gridStart: LocalDate = first.with(TemporalAdjusters.previousOrSame(WEEK_START))

    val columns: Int = (ChronoUnit.DAYS.between(gridStart, last) / 7).toInt() + 1

    /** Data da célula (coluna, linha); a linha 0 é o primeiro dia da semana. */
    fun dateAt(column: Int, row: Int): LocalDate = gridStart.plusDays((column * 7 + row).toLong())

    /** A data aparece no calendário (dentro do período e não futura)? */
    fun contains(date: LocalDate): Boolean = !date.isBefore(first) && !date.isAfter(last)

    /** Coluna da semana que contém a data. */
    fun columnOf(date: LocalDate): Int = (ChronoUnit.DAYS.between(gridStart, date) / 7).toInt()

    /** Meses da janela, do mais antigo ao mais recente. */
    fun months(): List<YearMonth> = (0 until MONTHS).map { startMonth.plusMonths(it.toLong()) }

    /** Dá para voltar mais um mês? */
    fun canGoBack(): Boolean = endMonth.isAfter(MIN_MONTH)

    /** Dá para avançar um mês (sem passar do mês de hoje)? */
    fun canGoForward(): Boolean = endMonth.isBefore(YearMonth.from(today))

    companion object {
        const val MONTHS = 3
        val WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

        /** Piso de segurança da navegação para trás. */
        val MIN_MONTH: YearMonth = YearMonth.of(2000, 1)
    }
}
