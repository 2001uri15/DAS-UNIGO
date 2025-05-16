package com.example.unigo.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unigo.R;
import com.example.unigo.adapter.RutaBusAdapter;
import com.example.unigo.database.DBServer;
import com.example.unigo.model.RutaBus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BusCementerio extends Fragment {

    private RecyclerView recyclerView;
    private RutaBusAdapter adapter;
    private List<RutaBus> listaRutas = new ArrayList<>();
    private TextView txtNoRutas;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bus_cementerio, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRutas);
        txtNoRutas = view.findViewById(R.id.txtNoRutas);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RutaBusAdapter(getContext(), listaRutas);
        recyclerView.setAdapter(adapter);

        obtenerParadas();

        return view;
    }

    private void obtenerParadas() {
        SharedPreferences busRutas = getActivity().getSharedPreferences("BusRutas", Context.MODE_PRIVATE);
        String fecha = busRutas.getString("fecha", "");

        Log.d("BusCementerio", "Fecha obtenida: " + fecha);

        Data inputData = new Data.Builder()
                .putString("action", "busCementerio")
                .putString("fecha", fecha)
                .build();

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(DBServer.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getContext()).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            try {
                                String result = workInfo.getOutputData().getString("result");
                                Log.d("BusCementerio", "Respuesta del servidor: " + result);

                                JSONObject response = new JSONObject(result);
                                if (response.getString("status").equals("success")) {
                                    listaRutas.clear();

                                    if (response.has("data")) {
                                        JSONArray data = response.getJSONArray("data");
                                        Log.d("BusCementerio", "Número de horarios: " + data.length());

                                        for (int i = 0; i < data.length(); i++) {
                                            JSONObject horarioJson = data.getJSONObject(i);
                                            String ruta = horarioJson.getString("routeShortName");
                                            String idViaje = horarioJson.getString("tripId");
                                            String origen = horarioJson.getString("origen");
                                            String destino = horarioJson.getString("destino");
                                            String hora = horarioJson.getString("departureTime");

                                            // Simplificar la hora si es necesario (16:00:00 -> 16:00)
                                            if (hora.length() > 5 && hora.endsWith(":00")) {
                                                hora = hora.substring(0, 5);
                                            }

                                            RutaBus rutaBus = new RutaBus(ruta, idViaje, origen, destino, hora);
                                            listaRutas.add(rutaBus);
                                            Log.d("BusCementerio", "Horario añadido: " + rutaBus.toString());
                                        }

                                        if (listaRutas.isEmpty()) {
                                            mostrarMensaje("No hay horarios disponibles para este día.");
                                        } else {
                                            mostrarRutas();
                                        }
                                    } else if (response.has("message")) {
                                        mostrarMensaje(response.getString("message"));
                                    }

                                    adapter.notifyDataSetChanged();
                                } else {
                                    mostrarError(response.getString("message"));
                                }
                            } catch (JSONException e) {
                                Log.e("BusCementerio", "Error al parsear JSON", e);
                                mostrarError("Error al procesar la respuesta");
                            }
                        } else {
                            String error = workInfo.getOutputData().getString("result");
                            Log.e("BusCementerio", "Error en WorkManager: " + error);
                            mostrarError(error);
                        }
                    }
                });

        WorkManager.getInstance(getContext()).enqueue(loginRequest);
    }

    private void mostrarRutas() {
        getActivity().runOnUiThread(() -> {
            txtNoRutas.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        });
    }

    private void mostrarMensaje(String mensaje) {
        getActivity().runOnUiThread(() -> {
            txtNoRutas.setText(mensaje);
            txtNoRutas.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        });
    }

    private void mostrarError(String mensaje) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();
            txtNoRutas.setText(mensaje);
            txtNoRutas.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        });
    }
}