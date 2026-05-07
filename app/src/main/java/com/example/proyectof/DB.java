package com.example.proyectof;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "traductor.db";
    private static final int VERSION = 1;

    public DB(Context context) {
        super(context, NOMBRE_BD, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla de usuarios
        db.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario TEXT NOT NULL," +
                "contrasena TEXT NOT NULL)");

        // Insertar un usuario por defecto para pruebas
        db.execSQL("INSERT INTO usuarios (usuario, contrasena) VALUES ('admin', '1234')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }

    // Validar login
    public boolean validarLogin(String usuario, String contrasena) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM usuarios WHERE usuario=? AND contrasena=?",
                new String[]{usuario, contrasena}
        );
        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    // Registrar nuevo usuario
    public boolean registrarUsuario(String usuario, String contrasena) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("usuario", usuario);
        values.put("contrasena", contrasena);
        long resultado = db.insert("usuarios", null, values);
        return resultado != -1;
    }
}
