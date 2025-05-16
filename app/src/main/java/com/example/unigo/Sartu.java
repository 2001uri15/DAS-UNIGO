package com.example.unigo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo.database.DBServer;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class Sartu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        actualizarIdioma();
        SharedPreferences prefsIni = getSharedPreferences("Usuario", Context.MODE_PRIVATE);
        String tema = prefsIni.getString("tema", "Verde");
        int themeId = tema.equals("Morado") ? R.style.Theme_Vitoria_Purple : R.style.Theme_Vitoria_Green;
        setTheme(themeId);
        setContentView(R.layout.activity_sartu);

        // Reconocer los elementos
        EditText email = findViewById(R.id.etEmail);
        TextInputEditText pass = findViewById(R.id.etPassword);

        // Botón de login
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            sartu(pass.getText().toString(), email.getText().toString());
        });
    }

    private void sartu(String password, String mail) {
        Data inputData = new Data.Builder()
                .putString("action", "sartu")
                .putString("password", password)
                .putString("mail", mail)
                .build();

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(DBServer.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            try {
                                JSONObject response = new JSONObject(workInfo.getOutputData().getString("result"));

                                if (response.getString("status").equals("success")) {
                                    // Cogemos la info de JSON de la respuesta
                                    String token = response.getString("token");
                                    String nombre2 = response.getString("nombre");
                                    String apellido2 = response.getString("apellido");
                                    String mail2 = response.getString("mail");

                                    // Guardar datos de sesión (usando SharedPreferences, por ejemplo)
                                    SharedPreferences prefs2 = getSharedPreferences("Usuario", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs2.edit();
                                    editor.putBoolean("iniciado", true);
                                    editor.putString("token", token);
                                    editor.putString("nombre", nombre2);
                                    editor.putString("apellido", apellido2);
                                    editor.putString("mail", mail2);
                                    editor.apply();

                                    // Redirigir al main activity
                                    startActivity(new Intent(this, Home.class));
                                    finish();
                                } else {
                                    showError(response.getString("message"));
                                }
                            } catch (JSONException e) {
                                showError("Error al procesar la respuesta");
                            }
                        } else {
                            showError(workInfo.getOutputData().getString("result"));
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(loginRequest);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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