package com.example.unigo.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraExtensionSession;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unigo.Configuraciones;
import com.example.unigo.Home;
import com.example.unigo.R;
import com.example.unigo.Registrar;
import com.example.unigo.database.DBServer;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class PerfilInisesion extends Fragment {


    public PerfilInisesion() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_inisesion, container, false);


        // Referenciar
        Button btnConfiguraciones = view.findViewById(R.id.btnConfiguraciones);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        TextView tvRegister = view.findViewById(R.id.tvRegister);


        // Otras acciones
        btnConfiguraciones.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Configuraciones.class);
            startActivity(intent);
            requireActivity().finish();
        });
        btnLogin.setOnClickListener(v->{
            String user = etEmail.getText().toString();
            String pass = etPassword.getText().toString();
            sartu(pass,user);
        });
        tvRegister.setOnClickListener(v->{
            Intent intent = new Intent(getActivity(), Registrar.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
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

        WorkManager.getInstance(getContext()).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe((LifecycleOwner) getContext(), workInfo -> {
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
                                    SharedPreferences prefs2 = getActivity().getSharedPreferences("Usuario", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs2.edit();
                                    editor.putBoolean("iniciado", true);
                                    editor.putString("token", token);
                                    editor.putString("nombre", nombre2);
                                    editor.putString("apellido", apellido2);
                                    editor.putString("mail", mail2);
                                    editor.apply();

                                    // Redirigir al main activity
                                    startActivity(new Intent(getActivity(), Home.class));
                                    getActivity().finish();
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

        WorkManager.getInstance(getContext()).enqueue(loginRequest);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}