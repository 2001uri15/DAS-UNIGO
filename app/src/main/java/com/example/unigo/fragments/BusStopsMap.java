package com.example.unigo.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.unigo.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.unigo.database.DBInfoPesada;
import com.example.unigo.model.BusStop;

import java.io.File;
import java.util.ArrayList;

public class BusStopsMap extends Fragment implements OnMapReadyCallback {
    private ArrayList<BusStop> list;
    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bus_stops_map, container, false);

        // Obtener el SupportMapFragment y registrar el callback cuando el mapa esté listo
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configurar Retrofit
        loadBusStops();

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Centrar el mapa en una ubicación inicial (ej: Vitoria-Gasteiz)
        LatLng startPoint = new LatLng(42.848755, -2.641533);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 13.0f));
    }

    private void loadBusStops() {
        DBInfoPesada dbDatos = new DBInfoPesada(getContext());
        dbDatos.obtMarquesinas(new DBInfoPesada.ApiCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                // Si la API cambiara en el futuro y devolviera un JSONObject con un array dentro,
                // esta parte se podría usar. Pero actualmente NO aplica.
            }

            @Override
            public void onSuccess(File responseFile) {
                // No aplica en este caso
            }

            @Override
            public void onSuccess(String responseString) {
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONArray paradasArray = new JSONArray(responseString);

                        for (int i = 0; i < paradasArray.length(); i++) {
                            JSONObject parada = paradasArray.getJSONObject(i);

                            int stopId = parada.getInt("stop_id");
                            String stopName = parada.getString("stop_name");
                            String stopLat = parada.getString("stop_lat");
                            String stopLon = parada.getString("stop_lon");
                            addMarkerToMap(new BusStop(String.valueOf(stopId), stopName, Double.parseDouble(stopLat), Double.parseDouble(stopLon)));
                        }

                        Toast.makeText(getContext(), "Paradas actualizadas correctamente", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Log.e("DBDatos", "Error al parsear JSON", e);
                        Toast.makeText(getContext(), "Error al procesar las paradas", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addMarkerToMap(BusStop stop) {
        if (mMap == null) return;

        LatLng point = new LatLng(stop.getStop_lat(), stop.getStop_lon());
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(stop.getStop_name())
                .snippet("ID: " + stop.getStop_id() + "\n" +
                        "Lat: " + stop.getStop_lat() + "\n" +
                        "Lon: " + stop.getStop_lon()));
    }
}