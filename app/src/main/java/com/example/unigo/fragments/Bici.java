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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo.R;
import com.example.unigo.database.DBInfoPesada;
import com.example.unigo.database.DBServer;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Bici extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_ZOOM = 15;
    private static final LatLng DEFAULT_LOCATION = new LatLng(43.326352, -3.009416); // Las Arenas, Getxo
    private static final int LOCATION_UPDATE_INTERVAL = 10000;
    private static final int LOCATION_FASTEST_UPDATE_INTERVAL = 5000;
    private static final double MAX_CONNECTION_DISTANCE = 100; // metros máx para conectar a rutas

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
    private List<Polyline> bikeRoutes = new ArrayList<>();

    // Clase para manejar conexiones a rutas
    private static class RouteConnection {
        LatLng point;
        double distance;

        RouteConnection(LatLng point, double distance) {
            this.point = point;
            this.distance = distance;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        aplicarIdiomaGuardado();
        View view = inflater.inflate(R.layout.fragment_bici, container, false);

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

        // Poner el icono de ubicación de color blanco
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
            loadBikeRoutes();
        } catch (SecurityException e) {
            Log.e("Bici", "Error de seguridad al acceder a ubicación", e);
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
                Log.e("Bici", "Error al detener actualizaciones", e);
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

        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void loadBikeRoutes() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Thread.sleep(500); // Simular carga de datos
                List<List<LatLng>> routesFromApi = getBikeRoutes();

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    drawBikeRoutes(routesFromApi);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al cargar rutas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private List<List<LatLng>> getBikeRoutes() {
        List<List<LatLng>> routes = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1); // Para esperar la respuesta asíncrona

        DBInfoPesada db = new DBInfoPesada(requireContext());
        db.obtRutaBici(new DBInfoPesada.ApiCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                try {
                    // Procesar el JSON y convertirlo en rutas
                    Iterator<String> keys = responseJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray routeArray = responseJson.getJSONArray(key);
                        List<LatLng> route = new ArrayList<>();

                        for (int i = 0; i < routeArray.length(); i++) {
                            JSONObject point = routeArray.getJSONObject(i);
                            double lat = point.getDouble("lat");
                            double lon = point.getDouble("lon");
                            route.add(new LatLng(lat, lon));
                        }

                        routes.add(route);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown(); // Liberar el latch cuando se complete
                }
            }

            @Override
            public void onSuccess(File responseFile) {
                // No aplicable en este caso
                latch.countDown();
            }

            @Override
            public void onSuccess(String responseString) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    Map<Integer, List<LatLng>> routeMap = new HashMap<>();

                    // Agrupar puntos por ID de ruta
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject point = jsonArray.getJSONObject(i);
                        int routeId = point.getInt("id");
                        double lat = point.getDouble("lat");
                        double lon = point.getDouble("lon");

                        if (!routeMap.containsKey(routeId)) {
                            routeMap.put(routeId, new ArrayList<>());
                        }
                        routeMap.get(routeId).add(new LatLng(lat, lon));
                    }

                    // Añadir todas las rutas al resultado
                    routes.addAll(routeMap.values());
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("BikeRoutes", "Error obteniendo rutas: " + errorMessage);
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS); // Esperar máximo 10 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("BikeRoutes", "Interrupción mientras se esperaba por las rutas");
        }

        return routes;
    }

    private void drawBikeRoutes(List<List<LatLng>> routes) {
        for (Polyline line : bikeRoutes) {
            line.remove();
        }
        bikeRoutes.clear();

        for (List<LatLng> route : routes) {
            PolylineOptions options = new PolylineOptions()
                    .addAll(route)
                    .width(8f)
                    .color(Color.argb(255, 0, 100, 0)) // Verde oscuro
                    .geodesic(true)
                    .zIndex(1);

            bikeRoutes.add(googleMap.addPolyline(options));
        }
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
                List<List<LatLng>> allRoutes = getBikeRoutes();

                // 1. Encontrar los segmentos de ruta más cercanos
                RouteSegment originSegment = findClosestRouteSegment(currentLocation, allRoutes);
                RouteSegment destSegment = findClosestRouteSegment(destination, allRoutes);

                // 2. Verificar distancias de conexión
                if (originSegment == null || originSegment.distance > MAX_CONNECTION_DISTANCE) {
                    throw new Exception("Estás demasiado lejos de la red de rutas (" +
                            (int)originSegment.distance + "m)");
                }

                if (destSegment == null || destSegment.distance > MAX_CONNECTION_DISTANCE) {
                    throw new Exception("El destino está demasiado lejos de la red de rutas (" +
                            (int)destSegment.distance + "m)");
                }

                // 3. Calcular ruta óptima siguiendo exactamente los segmentos
                List<LatLng> optimalRoute = calculateExactRoute(allRoutes, originSegment, destSegment);

                // 4. Construir ruta completa con conexiones
                List<LatLng> fullRoute = buildCompleteRoute(currentLocation, destination, optimalRoute,
                        originSegment, destSegment);

                // 5. Calcular distancia total
                int totalDistance = calculateRouteDistance(fullRoute);
                int durationMinutes = (int) (totalDistance / 250.0); // 15 km/h

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    drawRouteToDestination(fullRoute);
                    updateRouteInfo(totalDistance, durationMinutes);

                    // Actualizar marcador de destino
                    if (destinationMarker != null) {
                        destinationMarker.remove();
                    }
                    destinationMarker = googleMap.addMarker(new MarkerOptions()
                            .position(destination)
                            .title("Destino")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showRouteError(e.getMessage());
                });
            }
        }).start();
    }

    private static class RouteSegment {
        List<LatLng> fullRoute; // Ruta completa a la que pertenece
        int segmentIndex;       // Índice del punto inicial del segmento
        LatLng closestPoint;    // Punto más cercano en el segmento
        double distance;        // Distancia al segmento

        RouteSegment(List<LatLng> fullRoute, int segmentIndex, LatLng closestPoint, double distance) {
            this.fullRoute = fullRoute;
            this.segmentIndex = segmentIndex;
            this.closestPoint = closestPoint;
            this.distance = distance;
        }
    }

    private RouteSegment findClosestRouteSegment(LatLng point, List<List<LatLng>> allRoutes) {
        RouteSegment closestSegment = null;
        double minDistance = Double.MAX_VALUE;

        for (List<LatLng> route : allRoutes) {
            for (int i = 0; i < route.size() - 1; i++) {
                LatLng start = route.get(i);
                LatLng end = route.get(i + 1);

                // Calcular punto más cercano en este segmento
                LatLng closest = closestPointOnSegment(point, start, end);
                double dist = distanceBetween(point, closest);

                if (dist < minDistance) {
                    minDistance = dist;
                    closestSegment = new RouteSegment(route, i, closest, dist);
                }
            }
        }

        return closestSegment;
    }

    private LatLng closestPointOnSegment(LatLng p, LatLng a, LatLng b) {
        // Vector AP
        double apLat = p.latitude - a.latitude;
        double apLng = p.longitude - a.longitude;

        // Vector AB
        double abLat = b.latitude - a.latitude;
        double abLng = b.longitude - a.longitude;

        double ab2 = abLat * abLat + abLng * abLng;
        double ap_ab = apLat * abLat + apLng * abLng;
        double t = ap_ab / ab2;

        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;

        return new LatLng(a.latitude + abLat * t, a.longitude + abLng * t);
    }

    private List<LatLng> calculateExactRoute(List<List<LatLng>> allRoutes,
                                             RouteSegment originSeg, RouteSegment destSeg) {

        // Si ambos puntos están en la misma ruta
        if (originSeg.fullRoute == destSeg.fullRoute) {
            return getRouteBetween(originSeg, destSeg);
        }

        // Para rutas diferentes, usar puntos de conexión predefinidos
        List<LatLng> route = new ArrayList<>();

        // 1. Desde el punto de origen hasta el final de su segmento
        route.addAll(getRouteBetween(originSeg,
                new RouteSegment(originSeg.fullRoute, originSeg.fullRoute.size() - 2,
                        originSeg.fullRoute.get(originSeg.fullRoute.size() - 1), 0)));

        // 2. Encontrar conexión entre rutas (aquí necesitarías tu lógica de conexiones)
        List<LatLng> connection = findConnectionBetweenRoutes(
                originSeg.fullRoute, destSeg.fullRoute);
        route.addAll(connection);

        // 3. Desde inicio de la ruta destino hasta el punto de destino
        route.addAll(getRouteBetween(
                new RouteSegment(destSeg.fullRoute, 0, destSeg.fullRoute.get(0), 0),
                destSeg));

        return route;
    }

    private List<LatLng> getRouteBetween(RouteSegment startSeg, RouteSegment endSeg) {
        List<LatLng> segment = new ArrayList<>();

        // Añadir el punto de conexión inicial
        segment.add(startSeg.closestPoint);

        // Añadir todos los puntos intermedios
        for (int i = startSeg.segmentIndex + 1; i <= endSeg.segmentIndex; i++) {
            segment.add(startSeg.fullRoute.get(i));
        }

        // Añadir el punto de conexión final si es diferente
        if (!endSeg.closestPoint.equals(segment.get(segment.size() - 1))) {
            segment.add(endSeg.closestPoint);
        }

        return segment;
    }

    private List<LatLng> findConnectionBetweenRoutes(List<LatLng> route1, List<LatLng> route2) {
        // Aquí implementarías tu lógica para encontrar cómo se conectan las rutas
        // Esto depende de cómo estén estructuradas tus rutas

        // Ejemplo simplificado: buscar puntos cercanos entre rutas
        for (LatLng p1 : route1) {
            for (LatLng p2 : route2) {
                if (distanceBetween(p1, p2) < 50) { // Umbral de conexión
                    List<LatLng> connection = new ArrayList<>();
                    connection.add(p1);
                    connection.add(p2);
                    return connection;
                }
            }
        }

        // Si no encuentra conexión directa, buscar a través de rutas intermedias
        // (Implementación más compleja necesaria aquí)

        return new ArrayList<>(); // En caso real, esto debería manejarse mejor
    }

    private List<LatLng> buildCompleteRoute(LatLng origin, LatLng destination,
                                            List<LatLng> optimalRoute, RouteSegment originSeg, RouteSegment destSeg) {

        List<LatLng> fullRoute = new ArrayList<>();

        // 1. Conexión desde origen hasta la ruta
        if (distanceBetween(origin, originSeg.closestPoint) > 5) {
            fullRoute.add(origin);
            fullRoute.add(originSeg.closestPoint);
        } else {
            fullRoute.add(originSeg.closestPoint);
        }

        // 2. Ruta óptima
        fullRoute.addAll(optimalRoute);

        // 3. Conexión desde ruta hasta destino
        if (distanceBetween(destSeg.closestPoint, destination) > 5) {
            fullRoute.add(destSeg.closestPoint);
            fullRoute.add(destination);
        } else {
            fullRoute.add(destination);
        }

        return fullRoute;
    }

    private RouteConnection findBestConnection(LatLng point, List<List<LatLng>> allRoutes) {
        LatLng bestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (List<LatLng> route : allRoutes) {
            for (LatLng routePoint : route) {
                double dist = distanceBetween(point, routePoint);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestPoint = routePoint;
                }
            }
        }

        return bestPoint != null ? new RouteConnection(bestPoint, minDistance) : null;
    }

    private List<LatLng> calculateOptimalRoute(List<List<LatLng>> allRoutes, LatLng start, LatLng end) {
        Map<LatLng, List<LatLng>> graph = buildGraphFromRoutes(allRoutes);
        return dijkstraShortestPath(graph, start, end);
    }

    private Map<LatLng, List<LatLng>> buildGraphFromRoutes(List<List<LatLng>> allRoutes) {
        Map<LatLng, List<LatLng>> graph = new HashMap<>();

        for (List<LatLng> route : allRoutes) {
            for (int i = 0; i < route.size() - 1; i++) {
                LatLng current = route.get(i);
                LatLng next = route.get(i + 1);

                if (!graph.containsKey(current)) {
                    graph.put(current, new ArrayList<>());
                }
                graph.get(current).add(next);

                if (!graph.containsKey(next)) {
                    graph.put(next, new ArrayList<>());
                }
                graph.get(next).add(current);
            }
        }
        return graph;
    }

    private List<LatLng> dijkstraShortestPath(Map<LatLng, List<LatLng>> graph, LatLng start, LatLng end) {
        Map<LatLng, Double> distances = new HashMap<>();
        Map<LatLng, LatLng> previous = new HashMap<>();
        PriorityQueue<LatLng> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (LatLng node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            LatLng current = queue.poll();

            if (current.equals(end)) {
                break;
            }

            for (LatLng neighbor : graph.get(current)) {
                double alt = distances.get(current) + distanceBetween(current, neighbor);
                if (alt < distances.get(neighbor)) {
                    distances.put(neighbor, alt);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<LatLng> path = new ArrayList<>();
        for (LatLng node = end; node != null; node = previous.get(node)) {
            path.add(0, node);
        }

        return path;
    }

    private void drawRouteToDestination(List<LatLng> routePoints) {
        if (routePoints == null || routePoints.size() < 2) {
            Toast.makeText(getContext(), "No se pudo calcular la ruta", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRoute != null) {
            currentRoute.remove();
        }

        PolylineOptions options = new PolylineOptions()
                .addAll(routePoints)
                .width(12f)
                .color(Color.RED)
                .geodesic(true)
                .zIndex(2);

        currentRoute = googleMap.addPolyline(options);
        adjustMapView(routePoints);
    }

    private static double distanceBetween(LatLng p1, LatLng p2) {
        double R = 6371000; // Radio de la Tierra en metros
        double dLat = Math.toRadians(p2.latitude - p1.latitude);
        double dLng = Math.toRadians(p2.longitude - p1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int calculateRouteDistance(List<LatLng> route) {
        double distance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            distance += distanceBetween(route.get(i), route.get(i + 1));
        }
        return (int) distance;
    }

    private void adjustMapView(List<LatLng> routePoints) {
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : routePoints) {
                builder.include(point);
            }
            if (currentLocation != null) {
                builder.include(currentLocation);
            }
            if (destinationMarker != null) {
                builder.include(destinationMarker.getPosition());
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (Exception e) {
            Log.e("Bici", "Error al ajustar vista del mapa", e);
        }
    }

    private void updateRouteInfo(int distanceMeters, int durationMinutes) {
        String distanceText = distanceMeters < 1000 ?
                String.format(Locale.getDefault(), "%d metros", distanceMeters) :
                String.format(Locale.getDefault(), "%.1f kilómetros", distanceMeters / 1000.0);

        String durationText = durationMinutes < 60 ?
                String.format("%d minutos", durationMinutes) :
                String.format("%d horas %d minutos", durationMinutes / 60, durationMinutes % 60);

        tvDistance.setText(distanceText);
        tvDuration.setText(durationText);
        routeInfoContainer.setVisibility(View.VISIBLE);
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
            Log.e("Bici", "Error al vibrar", e);
        }
    }

    private void aplicarIdiomaGuardado() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Ajustes", Context.MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
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
}