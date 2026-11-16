package com.roots.app.data

/** Um hábito acompanhado. A cor e o ícone pertencem ao hábito; os registros guardam só a data. */
data class Habit(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/** Opções oferecidas ao criar/editar um hábito e os hábitos iniciais do primeiro uso. */
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

    /** Hábitos criados na primeira vez que o banco é aberto (editáveis e excluíveis). */
    val SEED: List<Habit> = listOf(
        Habit(name = "Exercício", icon = "🏃", color = COLORS[0]),
        Habit(name = "Bíblia", icon = "📖", color = COLORS[1]),
        Habit(name = "Estudo", icon = "📚", color = COLORS[2])
    )

    const val NAME_MAX_LENGTH = 30
}
