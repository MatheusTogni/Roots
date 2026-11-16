package com.roots.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.roots.app.widget.WidgetUpdater

/**
 * Único ponto de acesso ao SQLite, usado pelo app e pelo widget (mesmo processo, mesma conexão).
 * Todo método que grava avisa os widgets, então app e widget nunca divergem.
 */
class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = Db(appContext)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }

    private fun changed() {
        WidgetUpdater.refreshAll(appContext)
    }

    // ---------- Hábitos ----------

    fun habits(): List<Habit> =
        db.readableDatabase.rawQuery(
            """
            SELECT id, name, icon, color, created_at, sort_order
            FROM habits WHERE is_active = 1
            ORDER BY sort_order, id
            """,
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(c.toHabit())
            }
        }

    fun habit(id: Long): Habit? =
        db.readableDatabase.rawQuery(
            "SELECT id, name, icon, color, created_at, sort_order FROM habits WHERE id = ? AND is_active = 1",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) c.toHabit() else null
        }

    private fun Cursor.toHabit() = Habit(
        id = getLong(0),
        name = getString(1),
        icon = getString(2),
        color = getInt(3),
        createdAt = getLong(4),
        sortOrder = getInt(5)
    )

    fun saveHabit(h: Habit): Long {
        val values = ContentValues().apply {
            put("name", h.name)
            put("icon", h.icon)
            put("color", h.color)
        }
        val wdb = db.writableDatabase
        val id: Long
        if (h.id == 0L) {
            val next = wdb.rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM habits", null)
                .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            values.put("created_at", h.createdAt)
            values.put("sort_order", next)
            values.put("is_active", 1)
            id = wdb.insertOrThrow("habits", null, values)
        } else {
            wdb.update("habits", values, "id = ?", arrayOf(h.id.toString()))
            id = h.id
        }
        changed()
        return id
    }

    /** Exclui o hábito e, por ON DELETE CASCADE, todos os registros dele. */
    fun deleteHabit(id: Long) {
        db.writableDatabase.delete("habits", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Quantos dias já foram registrados para o hábito (mostrado na confirmação de exclusão). */
    fun recordCount(habitId: Long): Int =
        db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM habit_records WHERE habit_id = ?",
            arrayOf(habitId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    // ---------- Registros ----------

    /** Ids dos hábitos ativos concluídos na data [date] (yyyy-MM-dd). */
    fun doneOn(date: String): Set<Long> =
        db.readableDatabase.rawQuery(
            """
            SELECT r.habit_id FROM habit_records r
            JOIN habits h ON h.id = r.habit_id
            WHERE r.date = ? AND h.is_active = 1
            """,
            arrayOf(date)
        ).use { c ->
            val out = HashSet<Long>()
            while (c.moveToNext()) out.add(c.getLong(0))
            out
        }

    /** Datas concluídas de cada hábito entre [from] e [to] (inclusive), numa única consulta. */
    fun datesByHabit(from: String, to: String): Map<Long, Set<String>> =
        db.readableDatabase.rawQuery(
            """
            SELECT r.habit_id, r.date FROM habit_records r
            JOIN habits h ON h.id = r.habit_id
            WHERE r.date BETWEEN ? AND ? AND h.is_active = 1
            """,
            arrayOf(from, to)
        ).use { c ->
            val out = HashMap<Long, MutableSet<String>>()
            while (c.moveToNext()) {
                out.getOrPut(c.getLong(0)) { HashSet() }.add(c.getString(1))
            }
            out
        }

    /**
     * Alterna a conclusão do hábito na data: remove o registro se existir, senão cria.
     * Devolve true se o hábito ficou concluído. Só mexe no par hábito/data informado.
     */
    fun toggleRecord(habitId: Long, date: String): Boolean {
        val wdb = db.writableDatabase
        var nowDone = false
        wdb.beginTransaction()
        try {
            val removed = wdb.delete(
                "habit_records",
                "habit_id = ? AND date = ?",
                arrayOf(habitId.toString(), date)
            )
            if (removed == 0) {
                val v = ContentValues().apply {
                    put("habit_id", habitId)
                    put("date", date)
                    put("created_at", System.currentTimeMillis())
                }
                wdb.insertOrThrow("habit_records", null, v)
                nowDone = true
            }
            wdb.setTransactionSuccessful()
        } catch (e: SQLiteConstraintException) {
            // O hábito foi excluído enquanto uma tela (ou widget) antiga ainda o exibia.
            nowDone = false
        } finally {
            wdb.endTransaction()
        }
        changed()
        return nowDone
    }
}
