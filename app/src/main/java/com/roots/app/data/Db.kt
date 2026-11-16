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
        // O app começa sem nenhum hábito: quem instala cria os seus.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrações futuras: um bloco por versão, em ordem, sem apagar dados. Exemplo:
        // if (oldVersion < 2) { db.execSQL("ALTER TABLE habits ADD COLUMN ...") }
    }

    companion object {
        const val VERSION = 1
    }
}
