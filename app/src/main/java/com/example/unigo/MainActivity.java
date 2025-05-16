package com.example.unigo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Spinner languageSpinner;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actualizarIdioma();
        EdgeToEdge.enable(this);
        SharedPreferences prefsIni = getSharedPreferences("Usuario", Context.MODE_PRIVATE);
        boolean iniciado = prefsIni.getBoolean("iniciado", false);
        String tema = prefsIni.getString("tema", "Verde");
        int themeId = tema.equals("Morado") ? R.style.Theme_Vitoria_Purple : R.style.Theme_Vitoria_Green;
        setTheme(themeId);
        if(iniciado){
            Intent intent = new Intent(MainActivity.this, Home.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        languageSpinner = findViewById(R.id.languageSpinner);
        Button btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            String idioma = obtenerCodigoIdioma(languageSpinner.getSelectedItem().toString());

            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("Ajustes", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("idioma", idioma);
            editor.apply();

            // Aplicar idioma
            actualizarIdioma(idioma);

            Intent intent = new Intent(MainActivity.this, Home.class);
            startActivity(intent);
            finish();
        });

        // Permiso para las notificaciones
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }


        Button btnRegi = findViewById(R.id.btnRegister);
        btnRegi.setOnClickListener(v -> {
            String idioma = obtenerCodigoIdioma(languageSpinner.getSelectedItem().toString());

            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("Ajustes", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("idioma", idioma);
            editor.apply();

            // Aplicar idioma
            actualizarIdioma(idioma);

            Intent intent = new Intent(MainActivity.this, Registrar.class);
            startActivity(intent);
            finish();
        });

        Button btnSartu = findViewById(R.id.btnLogin);
        btnSartu.setOnClickListener(v -> {
            String idioma = obtenerCodigoIdioma(languageSpinner.getSelectedItem().toString());

            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("Ajustes", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("idioma", idioma);
            editor.apply();

            // Aplicar idioma
            actualizarIdioma(idioma);

            Intent intent = new Intent(MainActivity.this, Sartu.class);
            startActivity(intent);
            finish();
        });

    }

    private String obtenerCodigoIdioma(String idiomaSeleccionado) {
        switch (idiomaSeleccionado) {
            case "Castellano":
                return "es";
            case "Euskara":
                return "eu";
            case "English":
                return "en";
            default:
                return "es";
        }
    }

    private void actualizarIdioma(String idioma) {
        SharedPreferences prefs2 = getSharedPreferences("Ajustes", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs2.edit();
        editor.putString("idioma", idioma);


        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(nuevaloc);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void actualizarIdioma(){
        // Obtener idioma guardado en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("Ajustes", MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es"); // Por defecto español

        // Aplicar idioma antes de cargar el contenido
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(nuevaloc);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

}