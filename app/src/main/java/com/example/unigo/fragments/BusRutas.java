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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unigo.Home;
import com.example.unigo.R;
import com.example.unigo.adapter.RutaBusAdapter;
import com.example.unigo.adapter.RutaTransbordoAdapter;
import com.example.unigo.database.DBServer;
import com.example.unigo.model.RutaBus;
import com.example.unigo.model.RutaBusTransb;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class BusRutas extends Fragment {

    private RecyclerView recyclerView;
    private RutaBusAdapter adapter;
    private List<RutaBus> listaRutas = new ArrayList<>();
    private TextView txtNoRutas;


    public BusRutas() {
        // Required empty public constructor
    }

    public static BusRutas newInstance() {
        BusRutas fragment = new BusRutas();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bus_rutas, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRutas);
        txtNoRutas = view.findViewById(R.id.txtNoRutas);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RutaBusAdapter(getContext(), listaRutas);
        recyclerView.setAdapter(adapter);

        obtenerParadas(); // O tu lógica de carga de rutas

        return view;
    }

    private void obtenerParadas() {
        SharedPreferences busRutas = getActivity().getSharedPreferences("BusRutas", Context.MODE_PRIVATE);
        String origen = busRutas.getString("origen", "");
        String destino = busRutas.getString("destino", "");
        String fecha = busRutas.getString("fecha", "");
        String hora = busRutas.getString("hora", "");

        Data inputData = new Data.Builder()
                .putString("action", "busRuta")
                .putString("origen", origen)
                .putString("destino", destino)
                .putString("fecha", fecha)
                .putString("hora", hora)

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
                                    listaRutas.clear(); // Limpiar por si hay datos anteriores

                                    for (int i = 0; i < response.getJSONArray("data").length(); i++) {
                                        JSONObject rutaJson = response.getJSONArray("data").getJSONObject(i);

                                        String routeShortName = rutaJson.getString("route_short_name");
                                        String tripId = rutaJson.getString("trip_id");
                                        String origenRuta = rutaJson.getString("origen");
                                        String destinoRuta = rutaJson.getString("destino");
                                        String departureTime = rutaJson.getString("departure_time");

                                        RutaBus ruta = new RutaBus(routeShortName, tripId, origenRuta, destinoRuta, departureTime);
                                        listaRutas.add(ruta);
                                    }

                                    // Mostrar u ocultar mensaje de "no hay rutas"
                                    if (listaRutas.isEmpty()) {
                                        txtNoRutas.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        obtenerParadasTransbordo();

                                    } else {
                                        txtNoRutas.setVisibility(View.GONE);
                                        recyclerView.setVisibility(View.VISIBLE);
                                    }

                                    adapter.notifyDataSetChanged();

                                } else {
                                    showError(response.getString("message"));
                                    txtNoRutas.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                    obtenerParadasTransbordo();
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


    private void obtenerParadasTransbordo() {
        SharedPreferences busRutas = getActivity().getSharedPreferences("BusRutas", Context.MODE_PRIVATE);
        String origen = busRutas.getString("origen", "");
        String destino = busRutas.getString("destino", "");
        String fecha = busRutas.getString("fecha", "");
        String hora = busRutas.getString("hora", "");

        Data inputData = new Data.Builder()
                .putString("action", "busRutaTransbordo")
                .putString("origen", origen)
                .putString("destino", destino)
                .putString("fecha", fecha)
                .putString("hora", hora)
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
                                    List<RutaBusTransb> listaRutasTransbordo = new ArrayList<>();

                                    for (int i = 0; i < response.getJSONArray("data").length(); i++) {
                                        JSONObject rutaJson = response.getJSONArray("data").getJSONObject(i);

                                        String ruta1 = rutaJson.getString("ruta1");
                                        String viaje1 = rutaJson.getString("viaje1");
                                        String origenRuta = rutaJson.getString("origen");
                                        String transbordo = rutaJson.getString("transbordo");
                                        String salidaOrigen = rutaJson.getString("salida_origen");
                                        String ruta2 = rutaJson.getString("ruta2");
                                        String viaje2 = rutaJson.getString("viaje2");
                                        String transbordoConfirmado = rutaJson.getString("transbordo_confirmado");
                                        String destinoRuta = rutaJson.getString("destino");
                                        String salidaTransbordo = rutaJson.getString("salida_transbordo");

                                        RutaBusTransb ruta = new RutaBusTransb(
                                                ruta1, viaje1, origenRuta, transbordo, salidaOrigen,
                                                ruta2, viaje2, transbordoConfirmado, destinoRuta, salidaTransbordo
                                        );
                                        listaRutasTransbordo.add(ruta);
                                    }

                                    // Configurar RecyclerView
                                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    RutaTransbordoAdapter adapter = new RutaTransbordoAdapter(listaRutasTransbordo);
                                    recyclerView.setAdapter(adapter);

                                    // Mostrar u ocultar mensaje de "no hay rutas"
                                    if (listaRutasTransbordo.isEmpty()) {
                                        txtNoRutas.setVisibility(View.VISIBLE);
                                        txtNoRutas.setText("No hay ninuna ruta.");
                                        recyclerView.setVisibility(View.GONE);
                                    } else {
                                        txtNoRutas.setVisibility(View.GONE);
                                        recyclerView.setVisibility(View.VISIBLE);
                                    }

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