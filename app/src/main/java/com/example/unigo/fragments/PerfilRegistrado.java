package com.example.unigo.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo.R;
import com.example.unigo.database.DBServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class PerfilRegistrado extends Fragment {

    private SharedPreferences prefs;

    public PerfilRegistrado() {}

    public static PerfilRegistrado newInstance(String param1, String param2) {
        return new PerfilRegistrado();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("Usuario", Context.MODE_PRIVATE);
        aplicarTemaGuardado();
        aplicarIdiomaGuardado();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_registrado, container, false);

        // Referencias a la vista
        TextView tvNombre = view.findViewById(R.id.tvNombre);
        TextView tvCorreo = view.findViewById(R.id.tvCorreo);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        Button btnNotificaciones = view.findViewById(R.id.btnNotificaciones);
        Spinner spinnerIdioma = view.findViewById(R.id.spinnerIdioma);
        Spinner spinnerTema = view.findViewById(R.id.spinnerTema);

        // Datos de usuario
        String nombre = prefs.getString("nombre", "Nombre");
        String apellido = prefs.getString("apellido", "Apellido");
        String correo = prefs.getString("mail", "");

        tvNombre.setText(nombre + " " + apellido);
        tvCorreo.setText(correo);

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Notificaciones
        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
            startActivity(intent);
        });

        // Spinner idioma
        ArrayAdapter<CharSequence> idiomaAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.language_options, android.R.layout.simple_spinner_item);
        idiomaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(idiomaAdapter);

        String lang = prefs.getString("idioma", "es");
        int pos = idiomaToPosition(lang);
        spinnerIdioma.setSelection(pos);
        spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nuevoIdioma = positionToIdioma(position);
                if (!nuevoIdioma.equals(prefs.getString("idioma", "es"))) {
                    prefs.edit().putString("idioma", nuevoIdioma).apply();
                    recargarFragmento();
                    requireActivity().recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner tema
        String[] temas = {"Verde", "Morado"};
        ArrayAdapter<String> temaAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, temas);
        temaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTema.setAdapter(temaAdapter);

        String temaActual = prefs.getString("tema", "Verde");
        spinnerTema.setSelection(temaActual.equals("Verde") ? 0 : 1);
        spinnerTema.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nuevoTema = position == 0 ? "Verde" : "Morado";
                if (!nuevoTema.equals(temaActual)) {
                    prefs.edit().putString("tema", nuevoTema).apply();
                    requireActivity().recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    private void cerrarSesion() {
        String token = prefs.getString("token", "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putBoolean("iniciado", false);
        editor.apply();
        borrarSesion(token);
    }

    private void borrarSesion(String token) {
        Data inputData = new Data.Builder()
                .putString("action", "borrarSesion")
                .putString("token", token)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DBServer.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance().getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            try {
                                JSONObject response = new JSONObject(workInfo.getOutputData().getString("result"));
                                if (response.getString("status").equals("success")) {
                                    requireActivity().recreate();
                                } else {
                                    showError(response.getString("message"));
                                }
                            } catch (JSONException e) {
                                showError("Error al procesar la respuesta");
                            }
                        } else {
                            showError("Error en la conexión con el servidor");
                        }
                    }
                });

        WorkManager.getInstance().enqueue(request);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void aplicarIdiomaGuardado() {
        String idioma = prefs.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
    }

    private void aplicarTemaGuardado() {
        String tema = prefs.getString("tema", "Verde");
        int themeId = tema.equals("Morado") ? R.style.Theme_Vitoria_Purple : R.style.Theme_Vitoria_Green;
        requireActivity().setTheme(themeId);
    }

    private void recargarFragmento() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new PerfilRegistrado())
                .commit();

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
