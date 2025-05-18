package com.example.unigo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo.database.DBServer;
import com.example.unigo.fragments.Andando;
import com.example.unigo.fragments.Bici;
import com.example.unigo.fragments.Bus;
import com.example.unigo.fragments.HomeFrag;
import com.example.unigo.fragments.PerfilRegistrado;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class Home extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actualizarIdioma(); // Actualizamos el idioma
        SharedPreferences prefsIni = getSharedPreferences("Ajustes", Context.MODE_PRIVATE);
        String tema = prefsIni.getString("tema", "Verde");
        int themeId = tema.equals("Morado") ? R.style.Theme_Vitoria_Purple : R.style.Theme_Vitoria_Green;
        setTheme(themeId);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(tokenFCM -> guardarFCM(tokenFCM));

        // Configurar BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFrag();
                } else if (itemId == R.id.nav_walking) {
                    selectedFragment = new Andando();
                } else if (itemId == R.id.nav_bus) {
                    selectedFragment = new Bus();
                } else if (itemId == R.id.nav_bizi) {
                    selectedFragment = new Bici();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new PerfilRegistrado();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });

        // Cargar fragment por defecto al iniciar
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFrag())
                    .commit();
        }


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

    protected void onResume(){
        super.onResume();
        actualizarIdioma();
    }

    private void guardarFCM(String tokenFCM) {
        Data inputData = new Data.Builder()
                .putString("action", "guardarFCM")
                .putString("tokenFCM", tokenFCM)
                .build();

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(DBServer.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            //JSONObject response = new JSONObject(workInfo.getOutputData().getString("result"));
                            Log.d("Info", workInfo.getOutputData().toString());
                        } else {
                            //showError(workInfo.getOutputData().getString("result"));
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(loginRequest);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}