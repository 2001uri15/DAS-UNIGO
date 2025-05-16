package com.example.unigo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DBLocal extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "unigo_bus.db";
    private static final int DATABASE_VERSION = 1;

    // Tabla de rutas favoritas
    public static final String TABLE_FAVORITE_ROUTES = "favorite_routes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ORIGIN = "origin";
    public static final String COLUMN_DESTINATION = "destination";

    public DBLocal(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FAVORITE_ROUTES_TABLE = "CREATE TABLE " + TABLE_FAVORITE_ROUTES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ORIGIN + " TEXT NOT NULL,"
                + COLUMN_DESTINATION + " TEXT NOT NULL,"
                + "UNIQUE(" + COLUMN_ORIGIN + ", " + COLUMN_DESTINATION + ") ON CONFLICT IGNORE)";

        db.execSQL(CREATE_FAVORITE_ROUTES_TABLE);
        Log.d("DBLocal", "Tabla de rutas favoritas creada");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE_ROUTES);
        onCreate(db);
    }

    // Métodos CRUD para rutas favoritas

    /**
     * Añade una nueva ruta favorita
     */
    public long addFavoriteRoute(String origin, String destination) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_ORIGIN, origin);
        values.put(COLUMN_DESTINATION, destination);

        long id = db.insert(TABLE_FAVORITE_ROUTES, null, values);
        db.close();
        return id;
    }

    /**
     * Verifica si una ruta ya está marcada como favorita
     */
    public boolean isRouteFavorite(String origin, String destination) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_FAVORITE_ROUTES +
                " WHERE " + COLUMN_ORIGIN + " = ? AND " + COLUMN_DESTINATION + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{origin, destination});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Elimina una ruta favorita
     */
    public int deleteFavoriteRoute(String origin, String destination) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_FAVORITE_ROUTES,
                COLUMN_ORIGIN + " = ? AND " + COLUMN_DESTINATION + " = ?",
                new String[]{origin, destination});
    }

    /**
     * Obtiene todas las rutas favoritas
     */
    public List<FavoriteRoute> getAllFavoriteRoutes() {
        List<FavoriteRoute> routes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITE_ROUTES,
                new String[]{COLUMN_ID, COLUMN_ORIGIN, COLUMN_DESTINATION},
                null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                FavoriteRoute route = new FavoriteRoute();
                route.setId(cursor.getInt(0));
                route.setOrigin(cursor.getString(1));
                route.setDestination(cursor.getString(2));
                routes.add(route);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return routes;
    }

    // Clase modelo para las rutas favoritas
    public static class FavoriteRoute {
        private int id;
        private String origin;
        private String destination;

        // Getters y Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
    }
}