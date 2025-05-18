package com.example.unigo.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.unigo.R;
import com.example.unigo.database.DBLocal;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFrag extends Fragment {

    private TextView welcomeText;
    private TextView weatherCurrentTemp, weatherMinMaxTemp;
    private ImageView weatherIcon;
    private SharedPreferences prefs, prefs2;
    private final OkHttpClient client = new OkHttpClient();
    private Call currentCall;
    private LinearLayout favoritesContainer;
    private DBLocal dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inicializar dbHelper y prefs
        dbHelper = new DBLocal(getContext());
        prefs = requireActivity().getSharedPreferences("Usuario", Context.MODE_PRIVATE);
        prefs2 = requireActivity().getSharedPreferences("Ajustes", Context.MODE_PRIVATE);

        // Inflar la vista PRIMERO
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar vistas
        favoritesContainer = view.findViewById(R.id.favoritesContainer);
        welcomeText = view.findViewById(R.id.welcomeText);
        weatherCurrentTemp = view.findViewById(R.id.weatherCurrentTemp);
        weatherMinMaxTemp = view.findViewById(R.id.weatherMinMaxTemp);
        weatherIcon = view.findViewById(R.id.weatherIcon);


        // Configuración inicial
        aplicarIdiomaGuardado();

        // Set welcome message
        String nombre = prefs.getString("nombre", "");
        welcomeText.setText(nombre.isEmpty() ?
                getString(R.string.bienvenido) :
                getString(R.string.bienvenido_con_nombre, nombre));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchWeatherData();
        loadFavoriteRoutes();
        getTemperature();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any ongoing network requests when view is destroyed
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    private void fetchWeatherData() {
        // Get current date in required formats
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        SimpleDateFormat apiDateParamFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        SimpleDateFormat apiCompareFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String formattedDate = apiDateFormat.format(calendar.getTime());
        String formattedDateParam = apiDateParamFormat.format(calendar.getTime());
        String todayCompareDate = apiCompareFormat.format(calendar.getTime()) + "T22:00:00Z";

        String url = "https://api.euskadi.eus/euskalmet/weather/regions/basque_country/zones/vitoria_gasteiz" +
                "/locations/gasteiz/forecast/trends/at/" + formattedDate + "/for/" + formattedDateParam;

        Log.d("WeatherAPI", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJtZXQwMS5hcGlrZXkiLCJpc3MiOiJtaUFwbGljYWNpb24iLCJleHAiOjE5MDUwMDM1NTEsImlhdCI6MTc0NzMyMzU1MSwidmVyc2lvbiI6IjEuMC4wIiwiZW1haWwiOiJpay5hc2llcmxhcnJhemFiYWxAZ21haWwuY29tIiwibG9naW5JZCI6ImJjNjdjYTA3NzY0ZDEyN2NhMDllZTQ0OWY5MTVjNTQ4ODAyOWRmMDNkMDgzZTZjYTNmM2RhMDk3NTczYjU5N2UifQ.IBzAaAFVBOwGcy2-qjAcrLAVmuNeUnmblYX3PucqMsrUF1eajYWrtfNa2vlothoUOqG6LftJXulG92ATuKAy5jj4649eOfIvpyV22e2o52ZvzQNaka9dvLYOGlPSKM8GGOu0VMxXEK1yAGahIVNkzDxc2L0ZvtKxdEx1NrjaNDKvmOWuGEJtn3yG1YXwRSMpVYy1AB4emv-09pLle3g8f9jgeFthqa0ma6gczlUFqifsLo_c1n2m8Q5h_A2kWbkUA2Dn40HAtOSfEhC3U5D99Wxnaf6ZWUjSfgcYsksTqEHzRLlvT0n4Up4rWZuh7qRPxGrJ7oy8WTCBINeJYmDL5g")
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                Log.e("WeatherAPI", "Request failed", e);
                safeRunOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error al obtener datos del tiempo", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (call.isCanceled()) return;

                if (!response.isSuccessful()) {
                    handleError("HTTP error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    Log.d("WeatherAPI", "Raw response: " + responseData);

                    JSONObject json = new JSONObject(responseData);

                    if (!json.has("trendsByDate")) {
                        handleError("No trendsByDate in response");
                        return;
                    }

                    JSONObject trendsByDate = json.optJSONObject("trendsByDate");
                    if (trendsByDate == null || !trendsByDate.has("set")) {
                        handleError("Invalid trendsByDate format");
                        return;
                    }

                    JSONArray trendsSet = trendsByDate.optJSONArray("set");
                    if (trendsSet == null || trendsSet.length() == 0) {
                        handleError("Empty weather data");
                        return;
                    }

                    JSONObject todayWeather = findTodayWeather(trendsSet, todayCompareDate);

                    if (todayWeather == null) {
                        handleError("No weather data for today");
                        return;
                    }

                    updateWeatherUI(todayWeather);

                } catch (JSONException e) {
                    handleError("Invalid JSON format: " + e.getMessage());
                } catch (Exception e) {
                    handleError("Unexpected error: " + e.getMessage());
                }
            }

            private void handleError(String message) {
                Log.e("WeatherAPI", message);
                safeRunOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(),
                                "Error obteniendo datos del tiempo",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void safeRunOnUiThread(Runnable action) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> {
            if (isAdded() && getView() != null) {
                action.run();
            }
        });
    }

    private JSONObject findTodayWeather(JSONArray trendsSet, String todayDate) throws JSONException {
        for (int i = 0; i < trendsSet.length(); i++) {
            JSONObject trend = trendsSet.getJSONObject(i);
            String trendDate = trend.getString("date");
            if (trendDate.contains(todayDate.substring(0, 10))) {
                return trend;
            }
        }
        return null;
    }

    private void updateWeatherUI(JSONObject todayWeather) {
        try {
            JSONObject tempRange = todayWeather.getJSONObject("temperatureRange");

            JSONObject weather = todayWeather.getJSONObject("weather");
            String iconUrl = weather.getString("url");
            String iconName = iconUrl.substring(iconUrl.lastIndexOf('/') + 1);

            safeRunOnUiThread(() -> {
                if (isAdded() && getView() != null) {
                    //weatherMinMaxTemp.setText(minMaxText);
                    setWeatherIcon(iconName);
                }
            });

        } catch (JSONException e) {
            Log.e("WeatherAPI", "Error parsing weather data", e);
            safeRunOnUiThread(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error al procesar datos meteorológicos",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void getTemperature() {
        // Get current date in required formats
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        SimpleDateFormat apiDateParamFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        String formattedDate = apiDateFormat.format(calendar.getTime());
        String formattedDateParam = apiDateParamFormat.format(calendar.getTime());

        String url = "https://api.euskadi.eus/euskalmet/weather/regions/basque_country/zones/vitoria_gasteiz" +
                "/locations/gasteiz/forecast/at/" + formattedDate + "/for/" + formattedDateParam;

        Log.d("BilbaoWeatherAPI", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJtZXQwMS5hcGlrZXkiLCJpc3MiOiJtaUFwbGljYWNpb24iLCJleHAiOjE5MDUwMDM1NTEsImlhdCI6MTc0NzMyMzU1MSwidmVyc2lvbiI6IjEuMC4wIiwiZW1haWwiOiJpay5hc2llcmxhcnJhemFiYWxAZ21haWwuY29tIiwibG9naW5JZCI6ImJjNjdjYTA3NzY0ZDEyN2NhMDllZTQ0OWY5MTVjNTQ4ODAyOWRmMDNkMDgzZTZjYTNmM2RhMDk3NTczYjU5N2UifQ.IBzAaAFVBOwGcy2-qjAcrLAVmuNeUnmblYX3PucqMsrUF1eajYWrtfNa2vlothoUOqG6LftJXulG92ATuKAy5jj4649eOfIvpyV22e2o52ZvzQNaka9dvLYOGlPSKM8GGOu0VMxXEK1yAGahIVNkzDxc2L0ZvtKxdEx1NrjaNDKvmOWuGEJtn3yG1YXwRSMpVYy1AB4emv-09pLle3g8f9jgeFthqa0ma6gczlUFqifsLo_c1n2m8Q5h_A2kWbkUA2Dn40HAtOSfEhC3U5D99Wxnaf6ZWUjSfgcYsksTqEHzRLlvT0n4Up4rWZuh7qRPxGrJ7oy8WTCBINeJYmDL5g")
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                Log.e("BilbaoWeatherAPI", "Request failed", e);
                safeRunOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error al obtener datos del tiempo de Bilbao", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (call.isCanceled()) return;

                if (!response.isSuccessful()) {
                    handleError("HTTP error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    Log.d("BilbaoWeatherAPI", "Raw response: " + responseData);

                    JSONObject json = new JSONObject(responseData);

                    if (!json.has("temperature")) {
                        handleError("No temperature data in response");
                        return;
                    }

                    JSONObject temperature = json.getJSONObject("temperature");
                    if (!temperature.has("value")) {
                        handleError("No temperature value in response");
                        return;
                    }

                    JSONObject temperatureRange = json.getJSONObject("temperatureRange");
                    if (!temperatureRange.has("min")) {
                        handleError("No temperature value in response");
                        return;
                    }

                    if (!temperatureRange.has("max")) {
                        handleError("No temperature value in response");
                        return;
                    }

                    double currentTemp = temperature.getDouble("value");
                    double min = temperatureRange.getDouble("min");
                    double max = temperatureRange.getDouble("max");

                    safeRunOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            weatherCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°C", currentTemp));
                            String minMaxText = String.format(Locale.getDefault(),
                                    "Mín: %.1f°C / Máx: %.1f°C", min, max);
                            weatherMinMaxTemp.setText(minMaxText);
                        }
                    });

                } catch (JSONException e) {
                    handleError("Invalid JSON format: " + e.getMessage());
                } catch (Exception e) {
                    handleError("Unexpected error: " + e.getMessage());
                }
            }

            private void handleError(String message) {
                Log.e("BilbaoWeatherAPI", message);
                safeRunOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(),
                                "Error obteniendo datos del tiempo de Bilbao",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setWeatherIcon(String iconName) {
        int iconResId = 0;

        if (iconName.contains("i00d")) { // Despejado
            iconResId = R.drawable.icon_despejado;
        } else if (iconName.contains("i01d")) {
            iconResId = R.drawable.icon_chubascos_debiles;
        } else if (iconName.contains("i12d")) {
            iconResId = R.drawable.icon_lluvia_debil;
        } else if (iconName.contains("i01d")) {
            iconResId = R.drawable.icon_poco_nublo;
        } else if (iconName.contains("i15d")) {
            iconResId = R.drawable.icon_tormenta;
        } else if (iconName.contains("i10d")) {
            iconResId = R.drawable.icon_chubascos_debiles;
        } else if(iconName.contains("i04d")){ // Nubloso

        }

        if (iconResId != 0) {
            weatherIcon.setImageResource(iconResId);
        }
    }

    private void aplicarIdiomaGuardado() {
        String idioma = prefs2.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
    }

    private void loadFavoriteRoutes() {
        if (favoritesContainer == null || getContext() == null) return;
        favoritesContainer.removeAllViews();

        List<DBLocal.FavoriteRoute> favorites = dbHelper.getAllFavoriteRoutes();

        if (favorites.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No tienes rutas favoritas aún");
            emptyText.setTextSize(16f);
            emptyText.setPadding(0, 8, 0, 8);
            favoritesContainer.addView(emptyText);
            return;
        }

        for (DBLocal.FavoriteRoute route : favorites) {
            CardView cardView = (CardView) LayoutInflater.from(getContext())
                    .inflate(R.layout.item_favorite_route, favoritesContainer, false);

            TextView routeView = cardView.findViewById(R.id.routeText);
            routeView.setText(String.format("%s → %s", route.getOrigin(), route.getDestination()));

            cardView.setOnClickListener(v -> {
                // Crear el fragmento destino
                Bus busFragment = new Bus();

                // Pasar los parámetros
                Bundle args = new Bundle();
                args.putString("origen", route.getOrigin());
                args.putString("destino", route.getDestination());
                busFragment.setArguments(args);

                // Realizar la transacción
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, busFragment)
                        .addToBackStack("bus_fragment")
                        .commit();
            });

            favoritesContainer.addView(cardView);

            // Añadir separador si no es el último elemento
            if (favorites.indexOf(route) < favorites.size() - 1) {
                View separator = new View(getContext());
                separator.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                separator.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.divider));
                favoritesContainer.addView(separator);
            }
        }
    }
}