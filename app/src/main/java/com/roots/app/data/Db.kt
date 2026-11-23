package com.roots.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db(context: Context) : SQLiteOpenHelper(context, "roots.db", null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        // Necessário para o ON DELETE CASCADE funcionar.
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE habits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                icon TEXT NOT NULL,
                color INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """
        )
        // date = dia local no formato yyyy-MM-dd. O UNIQUE garante no máximo um registro por hábito/dia
        // e seu índice implícito cobre as consultas por hábito.
        db.execSQL(
            """
            CREATE TABLE habit_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
                date TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE (habit_id, date)
            )
            """
        )
        db.execSQL("CREATE INDEX idx_records_date ON habit_records(date)")
        createTaskTables(db)
        // O app começa sem nenhum hábito nem tarefa: quem instala cria os seus.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrações: um bloco por versão, em ordem, sem apagar dados. Exemplo:
        // if (oldVersion < 3) { db.execSQL("ALTER TABLE tasks ADD COLUMN ...") }
        if (oldVersion < 2) createTaskTables(db)
    }

    /**
     * Tarefas (versão 2). weekdays = 0 é a tarefa normal; senão é uma máscara dos dias da semana em que a
     * tarefa agendada aparece (bit 0 = segunda … bit 6 = domingo, ver TaskDays). task_records.date é o dia local
     * (yyyy-MM-dd) em que a tarefa foi concluída; o UNIQUE garante no máximo um registro por tarefa/dia.
     */
    private fun createTaskTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                icon TEXT NOT NULL,
                color INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                weekdays INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL(
            """
            CREATE TABLE task_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                date TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE (task_id, date)
            )
            """
        )
    }

    companion object {
        const val VERSION = 2
    }
}
