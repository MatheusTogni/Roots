package com.roots.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.roots.app.widget.WidgetUpdater
import java.time.LocalDate

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
        val nowDone = toggleDone("habit_records", "habit_id", habitId, date)
        changed()
        return nowDone
    }

    /**
     * Alterna o registro de um item (hábito ou tarefa) numa data: remove se existir, senão cria.
     * [table] e [ownerColumn] são sempre constantes do código, nunca texto vindo de fora.
     */
    private fun toggleDone(table: String, ownerColumn: String, ownerId: Long, date: String): Boolean {
        val wdb = db.writableDatabase
        var nowDone = false
        wdb.beginTransaction()
        try {
            val removed = wdb.delete(
                table,
                "$ownerColumn = ? AND date = ?",
                arrayOf(ownerId.toString(), date)
            )
            if (removed == 0) {
                val v = ContentValues().apply {
                    put(ownerColumn, ownerId)
                    put("date", date)
                    put("created_at", System.currentTimeMillis())
                }
                wdb.insertOrThrow(table, null, v)
                nowDone = true
            }
            wdb.setTransactionSuccessful()
        } catch (e: SQLiteConstraintException) {
            // O item foi excluído enquanto uma tela (ou widget) antiga ainda o exibia.
            nowDone = false
        } finally {
            wdb.endTransaction()
        }
        return nowDone
    }

    // ---------- Tarefas ----------

    /**
     * Tarefas que aparecem na lista de [date], na ordem de criação, cada uma com o estado de conclusão nesse dia.
     * - Normal: aparece enquanto nunca foi concluída e, se foi concluída hoje, continua à vista até o fim do dia;
     *   concluída em dia anterior, some.
     * - Agendada: aparece só nos dias da semana escolhidos; concluída = há registro nesta data.
     * É sempre calculado na leitura, então não depende de nenhuma rotina de meia-noite.
     */
    fun tasksFor(date: LocalDate): List<TaskItem> {
        val day = date.toString()
        return db.readableDatabase.rawQuery(
            """
            SELECT t.id, t.name, t.icon, t.color, t.created_at, t.sort_order, t.weekdays,
                   EXISTS(SELECT 1 FROM task_records r WHERE r.task_id = t.id AND r.date = ?),
                   EXISTS(SELECT 1 FROM task_records r WHERE r.task_id = t.id)
            FROM tasks t
            ORDER BY t.sort_order, t.id
            """,
            arrayOf(day)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val task = c.toTask()
                    val doneToday = c.getInt(7) == 1
                    val everDone = c.getInt(8) == 1
                    val shown = if (task.isScheduled) task.occursOn(date) else (!everDone || doneToday)
                    if (shown) add(TaskItem(task, doneToday))
                }
            }
        }
    }

    /** Todas as tarefas agendadas, para a lista de gerenciamento (editar e excluir). */
    fun scheduledTasks(): List<Task> =
        db.readableDatabase.rawQuery(
            """
            SELECT id, name, icon, color, created_at, sort_order, weekdays
            FROM tasks WHERE weekdays <> 0
            ORDER BY sort_order, id
            """,
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(c.toTask())
            }
        }

    /** Quantas tarefas existem (normais ocultas, agendadas e as de hoje): decide entre lista e estado vazio. */
    fun taskCount(): Int =
        db.readableDatabase.rawQuery("SELECT COUNT(*) FROM tasks", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun Cursor.toTask() = Task(
        id = getLong(0),
        name = getString(1),
        icon = getString(2),
        color = getInt(3),
        createdAt = getLong(4),
        sortOrder = getInt(5),
        weekdays = getInt(6)
    )

    /**
     * Cria ou atualiza a tarefa. O tipo não muda na edição: os dias só são regravados quando a tarefa
     * continua agendada, então uma tarefa normal nunca vira agendada (nem o contrário) por aqui.
     */
    fun saveTask(t: Task): Long {
        val values = ContentValues().apply {
            put("name", t.name)
            put("icon", t.icon)
            put("color", t.color)
        }
        val wdb = db.writableDatabase
        val id: Long
        if (t.id == 0L) {
            val next = wdb.rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM tasks", null)
                .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            values.put("created_at", t.createdAt)
            values.put("sort_order", next)
            values.put("weekdays", t.weekdays)
            id = wdb.insertOrThrow("tasks", null, values)
        } else {
            if (t.isScheduled) {
                values.put("weekdays", t.weekdays)
                wdb.update("tasks", values, "id = ? AND weekdays <> 0", arrayOf(t.id.toString()))
            } else {
                wdb.update("tasks", values, "id = ?", arrayOf(t.id.toString()))
            }
            id = t.id
        }
        changed()
        return id
    }

    /** Exclui a tarefa e, por ON DELETE CASCADE, os registros de conclusão dela. */
    fun deleteTask(id: Long) {
        db.writableDatabase.delete("tasks", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Alterna a conclusão da tarefa na data. Devolve true se ela ficou concluída. */
    fun toggleTaskRecord(taskId: Long, date: String): Boolean {
        val nowDone = toggleDone("task_records", "task_id", taskId, date)
        changed()
        return nowDone
    }
}
