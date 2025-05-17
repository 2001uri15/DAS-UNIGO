package com.example.unigo.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.unigo.Configuraciones;
import com.example.unigo.R;
import com.example.unigo.database.DBServer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_registrado, container, false);

        boolean token = prefs.getBoolean("iniciado", false);

        if(!token){
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new PerfilInisesion())
                    .commit();
        }

        // Referencias a la vista
        TextView tvNombre = view.findViewById(R.id.tvNombre);
        TextView tvCorreo = view.findViewById(R.id.tvCorreo);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        Button btnConfiguraciones = view.findViewById(R.id.btnConfiguraciones);

        // Datos de usuario
        String nombre = prefs.getString("nombre", "Nombre");
        String apellido = prefs.getString("apellido", "Apellido");
        String correo = prefs.getString("mail", "");

        tvNombre.setText(nombre + " " + apellido);
        tvCorreo.setText(correo);

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Configuraciones
        btnConfiguraciones.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Configuraciones.class);
            startActivity(intent);
            requireActivity().finish();
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
}