package com.example.unigo.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Andando extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_ZOOM = 15;
    private static final LatLng DEFAULT_LOCATION = new LatLng(42.847809, -2.681558);
    private static final int LOCATION_UPDATE_INTERVAL = 10000;
    private static final int LOCATION_FASTEST_UPDATE_INTERVAL = 5000;

    // Views
    private MapView mapView;
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private FloatingActionButton fabMyLocation;
    private LinearLayout routeInfoContainer;
    private TextView tvDistance;
    private TextView tvDuration;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LatLng currentLocation;
    private boolean isLocationObtained = false;

    // Route
    private Marker destinationMarker;
    private Polyline currentRoute;
    private GeoApiContext geoApiContext;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        aplicarIdiomaGuardado();
        View view = inflater.inflate(R.layout.fragment_andando, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        progressBar = view.findViewById(R.id.progressBar);
        fabMyLocation = view.findViewById(R.id.fabMyLocation);
        routeInfoContainer = view.findViewById(R.id.route_info_container);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvDuration = view.findViewById(R.id.tvDuration);

        // Setup FAB
        fabMyLocation.setOnClickListener(v -> centerMapOnMyLocation());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Directions API
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        // Poner el icono de ubi de color blanco
        FloatingActionButton fabMyLocation = view.findViewById(R.id.fabMyLocation);
        fabMyLocation.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setOnMapClickListener(this);

        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionExplanationDialog();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.location_permission_title)
                .setMessage(R.string.location_permission_message)
                .setPositiveButton(R.string.understood_button, (dialog, which) ->
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_LOCATION_PERMISSION))
                .setNegativeButton(R.string.deny_button, null)
                .show();
    }

    private void enableLocationFeatures() {
        if (!checkLocationSetup()) return;

        try {
            googleMap.setMyLocationEnabled(true);
            setupLocationUpdates();
            getLastKnownLocation();
        } catch (SecurityException e) {
            Log.e("Andando", "Error de seguridad al acceder a ubicación", e);
            useDefaultLocation();
        }
    }

    private boolean checkLocationSetup() {
        return googleMap != null && fusedLocationClient != null && isAdded();
    }

    private void setupLocationUpdates() {
        createLocationRequest();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    updateCurrentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        };
        startLocationUpdates();
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception e) {
                Log.e("Andando", "Error al detener actualizaciones", e);
            }
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        updateCurrentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        useDefaultLocation();
                    }
                });
    }

    private void updateCurrentLocation(LatLng newLocation) {
        if (!isLocationObtained) {
            currentLocation = newLocation;
            isLocationObtained = true;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, DEFAULT_ZOOM));
            fabMyLocation.setVisibility(View.VISIBLE);
        }
        currentLocation = newLocation;
    }

    private void useDefaultLocation() {
        currentLocation = DEFAULT_LOCATION;
        isLocationObtained = true;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
        Toast.makeText(getContext(), "Usando ubicación predeterminada", Toast.LENGTH_LONG).show();
    }

    private void centerMapOnMyLocation() {
        if (currentLocation != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
        } else {
            Toast.makeText(getContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        if (!isLocationObtained) {
            Toast.makeText(getContext(), "Esperando ubicación actual...", Toast.LENGTH_SHORT).show();
            return;
        }

        updateDestination(point);
        calculateRouteToDestination(point);
    }

    private void updateDestination(LatLng destination) {
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        // Ejemplo de cómo asignar un icono a tu marker
        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // Color rojo
    }

    private void calculateRouteToDestination(LatLng destination) {
        if (currentLocation == null || destination == null) {
            Toast.makeText(getContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        routeInfoContainer.setVisibility(View.GONE);

        if (currentRoute != null) {
            currentRoute.remove();
        }

        new Thread(() -> {
            try {
                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                        .mode(TravelMode.WALKING)
                        .origin(new com.google.maps.model.LatLng(
                                currentLocation.latitude,
                                currentLocation.longitude))
                        .destination(new com.google.maps.model.LatLng(
                                destination.latitude,
                                destination.longitude))
                        .await();

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    handleRouteResult(result, destination);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showRouteError("Error al calcular ruta: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleRouteResult(DirectionsResult result, LatLng destination) {
        if (result.routes == null || result.routes.length == 0) {
            showRouteError("No se encontró ruta peatonal");
            return;
        }

        com.google.maps.model.DirectionsRoute route = result.routes[0];

        if (route.legs != null && route.legs.length > 0) {
            com.google.maps.model.DirectionsLeg leg = route.legs[0];
            updateRouteInfo(leg);
        }

        String encodedPolyline = route.overviewPolyline.getEncodedPath();
        List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);

        if (!decodedPath.isEmpty()) {
            drawRouteOnMap(decodedPath);
            adjustMapView(decodedPath, destination);
        }
    }

    private void updateRouteInfo(com.google.maps.model.DirectionsLeg leg) {
        String distanceText = leg.distance.inMeters < 1000 ?
                String.format(Locale.getDefault(),
                        getContext().getString(R.string.distance_meters),
                        leg.distance.inMeters) :
                String.format(Locale.getDefault(),
                        getContext().getString(R.string.distance_kilometers),
                        leg.distance.inMeters / 1000.0);

        String durationText = String.format(
                getContext().getString(R.string.duration),
                leg.duration.humanReadable);

        tvDistance.setText(distanceText);
        tvDuration.setText(durationText);
        routeInfoContainer.setVisibility(View.VISIBLE);
    }

    private void drawRouteOnMap(List<LatLng> routePoints) {
        PolylineOptions options = new PolylineOptions()
                .addAll(routePoints)
                .width(12f)
                .color(Color.argb(255, 0, 100, 0)) // Verde oscuro
                .geodesic(true)
                .zIndex(1);

        currentRoute = googleMap.addPolyline(options);
    }

    private void adjustMapView(List<LatLng> routePoints, LatLng destination) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        builder.include(currentLocation);
        builder.include(destination);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private void showRouteError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        vibrate();
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(200);
            }
        } catch (Exception e) {
            Log.e("Andando", "Error al vibrar", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationFeatures();
            } else {
                useDefaultLocation();
                Toast.makeText(getContext(),
                        "Permiso denegado. Usando ubicación predeterminada",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (isLocationObtained && locationCallback != null) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        stopLocationUpdates();
        if (geoApiContext != null) {
            new Thread(() -> {
                try {
                    geoApiContext.shutdown();
                } catch (Exception e) {
                    Log.e("Andando", "Error al cerrar GeoApiContext", e);
                }
            }).start();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void aplicarIdiomaGuardado() {
        SharedPreferences prefs2 = requireActivity().getSharedPreferences("Ajustes", Context.MODE_PRIVATE);
        String idioma = prefs2.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
    }
}