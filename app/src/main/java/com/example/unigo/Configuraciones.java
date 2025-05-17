package com.example.unigo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Locale;

public class Configuraciones extends AppCompatActivity {

    private SharedPreferences prefs2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        prefs2 = getSharedPreferences("Ajustes", Context.MODE_PRIVATE);
        obtIdioma();
        setContentView(R.layout.activity_configuraciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar botón de notificaciones
        Button btnNotificaciones = findViewById(R.id.btnNotificaciones);
        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        // Configurar spinner de idioma
        Spinner spinnerIdioma = findViewById(R.id.spinnerIdioma);
        ArrayAdapter<CharSequence> idiomaAdapter = ArrayAdapter.createFromResource(this,
                R.array.language_options, android.R.layout.simple_spinner_item);
        idiomaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(idiomaAdapter);

        String lang = prefs2.getString("idioma", "es");
        int pos = idiomaToPosition(lang);
        spinnerIdioma.setSelection(pos);
        spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nuevoIdioma = positionToIdioma(position);
                if (!nuevoIdioma.equals(prefs2.getString("idioma", "es"))) {
                    prefs2.edit().putString("idioma", nuevoIdioma).apply();
                    aplicarIdioma(nuevoIdioma);
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configurar spinner de tema
        Spinner spinnerTema = findViewById(R.id.spinnerTema);
        String[] temas = {"Verde", "Morado"};
        ArrayAdapter<String> temaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, temas);
        temaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTema.setAdapter(temaAdapter);

        String temaActual = prefs2.getString("tema", "Verde");
        spinnerTema.setSelection(temaActual.equals("Verde") ? 0 : 1);
        spinnerTema.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nuevoTema = position == 0 ? "Verde" : "Morado";
                if (!nuevoTema.equals(temaActual)) {
                    prefs2.edit().putString("tema", nuevoTema).apply();
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(Configuraciones.this, Home.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void aplicarIdioma(String idioma) {
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void obtIdioma(){
        String idioma = prefs2.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private int idiomaToPosition(String idioma) {
        switch (idioma) {
            case "eu": return 1;
            case "en": return 2;
            default: return 0;
        }
    }

    private String positionToIdioma(int pos) {
        switch (pos) {
            case 1: return "eu";
            case 2: return "en";
            default: return "es";
        }
    }
}