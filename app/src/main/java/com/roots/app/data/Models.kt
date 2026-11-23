package com.roots.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Uma tarefa. Diferente do hábito, não tem calendário: ou é pontual ([weekdays] = 0, fica na lista até ser
 * concluída) ou é agendada (aparece só nos dias da semana marcados em [weekdays], ver [TaskDays]).
 * Ícone e cor seguem o mesmo conjunto dos hábitos ([HabitOptions]).
 */
data class Task(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val weekdays: Int = 0
) {
    val isScheduled: Boolean get() = weekdays != 0

    /** A tarefa agendada cai neste dia? Para a normal não faz sentido (ela depende de ter sido concluída). */
    fun occursOn(day: LocalDate): Boolean = (weekdays and TaskDays.bit(day.dayOfWeek)) != 0
}

/** Uma tarefa na lista de um dia, com o estado de conclusão nesse dia. */
data class TaskItem(val task: Task, val done: Boolean)

/** Máscara de dias da semana das tarefas agendadas: segunda = bit 0 … domingo = bit 6 (ordem do ISO-8601). */
object TaskDays {
    const val ALL = 0b1111111

    /** Ordem de exibição: domingo primeiro, igual ao calendário do app. */
    val DISPLAY_ORDER: List<DayOfWeek> = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    /** Rótulos curtos, na mesma ordem de [DISPLAY_ORDER]. */
    val SHORT_LABELS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    private val LONG_LABELS = listOf(
        "domingo", "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado"
    )

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    /** "Todos os dias", "Toda segunda-feira" ou "Seg · Qua · Sex" (na ordem de [DISPLAY_ORDER]). */
    fun summary(mask: Int): String {
        val picked = DISPLAY_ORDER.indices.filter { (mask and bit(DISPLAY_ORDER[it])) != 0 }
        return when {
            picked.size == DISPLAY_ORDER.size -> "Todos os dias"
            picked.size == 1 -> {
                val day = DISPLAY_ORDER[picked[0]]
                val feminine = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                (if (feminine) "Toda " else "Todo ") + LONG_LABELS[picked[0]]
            }
            else -> picked.joinToString(" · ") { SHORT_LABELS[it] }
        }
    }
}

/** Um hábito acompanhado. A cor e o ícone pertencem ao hábito; os registros guardam só a data. */
data class Habit(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/** Opções de cor e ícone oferecidas ao criar ou editar um hábito. */
object HabitOptions {
    val COLORS: IntArray = intArrayOf(
        0xFF30A14E.toInt(), // verde
        0xFFE3B341.toInt(), // amarelo
        0xFF3B82C4.toInt(), // azul
        0xFFE5534B.toInt(), // vermelho
        0xFFF08A3C.toInt(), // laranja
        0xFF8E6BC7.toInt(), // roxo
        0xFFE0709C.toInt(), // rosa
        0xFF2BB3A3.toInt(), // turquesa
        0xFF9A6B4B.toInt(), // marrom
        0xFF6B7A8F.toInt()  // cinza azulado
    )

    /** Emojis antigos o bastante (Unicode 9 ou menos) para aparecer no Android 8. */
    val ICONS: List<String> = listOf(
        "🏃", "💪", "🚴", "🏊", "🚶", "📖", "🙏", "📚",
        "✏️", "💻", "🎸", "🎨", "💧", "🥗", "😴", "💊",
        "🌱", "☀️", "🎯", "🍎", "🎧", "🛌", "🚭", "⏰"
    )

    const val NAME_MAX_LENGTH = 30
}
