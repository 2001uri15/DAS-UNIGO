package com.example.unigo.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;

import retrofit2.http.POST;

public class DBServer extends Worker {
    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/alarrazabal025/WEB/";
    private static final String TAG = "DBServer";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    public DBServer(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String action = getInputData().getString("action");
        String password = getInputData().getString("password");
        String nombre = getInputData().getString("nombre");
        String apellido = getInputData().getString("apellido");
        String mail = getInputData().getString("mail");
        String token = getInputData().getString("token");
        String tokenFCM = getInputData().getString("tokenFCM");
        String origen = getInputData().getString("origen");
        String destino = getInputData().getString("destino");
        String fecha = getInputData().getString("fecha");
        String hora = getInputData().getString("hora");
        String lat = String.valueOf(getInputData().getDouble("lat", 0));
        String log = String.valueOf(getInputData().getDouble("log", 0));


        try {
            String result;
            switch (Objects.requireNonNull(action)) {
                case "obtBusParadas":
                    result = obtenerParadas();
                    break;
                case "registrar":
                    result = registrar(nombre, apellido, password, mail);
                    break;
                case "sartu":
                    result = sartu(password, mail);
                    break;
                case "borrarSesion":
                    result = borrarSesion(token);
                    break;
                case "busRuta":
                    result = buscarRuta(origen, destino, fecha, hora);
                    break;
                case "busRutaTransbordo":
                    result = buscarRutaTransbordo(origen, destino, fecha, hora);
                    break;
                case "obtenerMarquesinas":
                    result = obtenerMarquesinas();
                    break;
                case "busCementerio":
                    result = buscarRutaCementerio(fecha);
                    break;
                case "obtParadaCercana":
                    result = paradaCercana(log, lat);
                    break;
                case "guardarFCM":
                    result = guardarFCM(tokenFCM);
                    break;
                case "olvimail":
                    result = olviMail(mail);
                    break;
                default:
                    return Result.failure(createOutputData("Error: Acción no válida"));
            }

            // Verifica si la respuesta es un array JSON (como ["parada1", "parada2"])
            if (result.startsWith("[") && result.endsWith("]")) {
                // Es un array directo, lo devolvemos tal cual
                return Result.success(createOutputData(result));
            }

            // Si no es array, intentamos parsear como objeto JSON
            JSONObject jsonResponse = new JSONObject(result);
            String status = jsonResponse.getString("status");

            if (status.equals("error")) {
                return Result.failure(createOutputData(jsonResponse.getString("message")));
            }

            return Result.success(createOutputData(result));
        } catch (IOException e) {
            Log.e(TAG, "Error de conexión: " + e.getMessage());
            return Result.failure(createOutputData("Error de conexión"));
        } catch (JSONException e) {
            Log.e(TAG, "Error al parsear JSON: " + e.getMessage());
            return Result.failure(createOutputData("Error al procesar la respuesta"));
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado: " + e.getMessage());
            return Result.failure(createOutputData("Error inesperado"));
        }
    }

    private String hacerPeticion(String endpoint, Map<String, String> params, String method) throws IOException {
        // Validación de parámetros de entrada
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("El endpoint no puede ser nulo o vacío");
        }
        if (method == null || (!method.equals(METHOD_GET) && !method.equals(METHOD_POST))) {
            throw new IllegalArgumentException("Método HTTP no válido. Use GET o POST");
        }

        HttpURLConnection urlConnection = null;
        String response;

        try {
            // Construir URL
            String fullUrl = BASE_URL + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);

            // Para GET, añadir parámetros a la URL
            if (method.equals(METHOD_GET) && !params.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (param.getKey() == null || param.getValue() == null) {
                        Log.w(TAG, "Parámetro nulo detectado - Key: " + param.getKey() + ", Value: " + param.getValue());
                        continue;
                    }

                    if (queryString.length() != 0) queryString.append('&');
                    queryString.append(URLEncoder.encode(param.getKey(), "UTF-8"))
                            .append('=')
                            .append(URLEncoder.encode(param.getValue(), "UTF-8"));
                }
                fullUrl += "?" + queryString.toString();
            }

            URL url = new URL(fullUrl);
            Log.d(TAG, "Conectando a: " + fullUrl);

            // Configurar conexión
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);

            // Configurar parámetros para POST
            if (method.equals(METHOD_POST) && !params.isEmpty()) {
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Construir parámetros POST
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (param.getKey() == null || param.getValue() == null) {
                        Log.w(TAG, "Parámetro nulo detectado - Key: " + param.getKey() + ", Value: " + param.getValue());
                        continue;
                    }

                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"))
                            .append('=')
                            .append(URLEncoder.encode(param.getValue(), "UTF-8"));
                }

                Log.d(TAG, "Enviando parámetros: " + postData);

                // Enviar datos
                try (OutputStream os = urlConnection.getOutputStream()) {
                    byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Procesar respuesta
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Código de respuesta: " + responseCode);

            InputStream inputStream;
            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                inputStream = urlConnection.getInputStream();
            } else {
                inputStream = urlConnection.getErrorStream();
            }

            // Leer respuesta
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                response = br.lines().collect(Collectors.joining());
            }

            if (response.trim().isEmpty()) {
                throw new IOException("Respuesta vacía del servidor");
            }

            Log.d(TAG, "Respuesta recibida: " + response);

        } catch (Exception e) {
            throw new IOException("Error de comunicación: " + e.getMessage(), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return response;
    }

    private Data createOutputData(String message) {
        return new Data.Builder()
                .putString("result", message)
                .build();
    }

    /*
     * A partir de aqui están todas las funciones para hacer las consultas de las peticiones
     */

    private String obtenerParadas() throws IOException {
        String recurso = "obt_bus_paradas.php";
        return hacerPeticion(recurso, new HashMap<>(), METHOD_GET); // Usa GET con parámetros vacíos
    }

    private String registrar(String nombre, String apellido, String password, String mail) throws IOException {
        String recurso = "registrar.php";

        Log.d(TAG, "Registrando usuario con:");
        Log.d(TAG, "Nombre: " + nombre);
        Log.d(TAG, "Apellido: " + apellido);
        Log.d(TAG, "Mail: " + mail);

        Map<String, String> params = new HashMap<>();
        params.put("nombre", nombre);
        params.put("apellido", apellido);
        params.put("password", password);
        params.put("mail", mail);

        try {
            String response = hacerPeticion(recurso, params, METHOD_POST);
            Log.d(TAG, "Respuesta del servidor: " + response);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error en registrar: " + e.getMessage(), e);
            throw e;
        }
    }

    private String sartu(String password, String mail) throws IOException {
        String recurso = "sartu.php";

        Log.d(TAG, "Registrando usuario con:");
        Log.d(TAG, "Mail: " + mail);

        Map<String, String> params = new HashMap<>();
        params.put("password", password);
        params.put("mail", mail);

        try {
            String response = hacerPeticion(recurso, params, METHOD_POST);
            Log.d(TAG, "Respuesta del servidor: " + response);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error en registrar: " + e.getMessage(), e);
            throw e;
        }
    }

    private String borrarSesion(String token) throws IOException {
        String recurso = "cerrarSesion.php";

        Map<String, String> params = new HashMap<>();
        params.put("token", token);

        return hacerPeticion(recurso, params, METHOD_POST);
    }

    private String buscarRuta(String origen, String destino, String fecha, String hora) throws IOException {
        String recurso = "buscarRutas.php";

        // Convertir la fecha al formato YYYY-MM-DD si no está ya en ese formato
        String fechaFormateada;
        try {
            // Convertir fecha de dd/MM/yyyy a yyyy-MM-dd
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat formatoSalida = new SimpleDateFormat("yyyy-MM-dd");
            Date date = formatoEntrada.parse(fecha);
            fechaFormateada = formatoSalida.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            // Si falla, usa la fecha sin formato
            fechaFormateada = fecha;
        }

        // Si tienes solo hora y minutos, agrega los segundos manualmente
        String horaConSegundos = hora + ":00";

        Map<String, String> params = new HashMap<>();
        params.put("origen", origen);
        params.put("destino", destino);
        params.put("fecha", fechaFormateada);
        params.put("hora", horaConSegundos);

        Log.d(TAG, params.toString());

        return hacerPeticion(recurso, params, METHOD_GET);
    }

    private String buscarRutaTransbordo(String origen, String destino, String fecha, String hora) throws IOException {
        String recurso = "buscarRutasTransbordo.php";

        // Convertir la fecha al formato YYYY-MM-DD si no está ya en ese formato
        String fechaFormateada;
        try {
            // Convertir fecha de dd/MM/yyyy a yyyy-MM-dd
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat formatoSalida = new SimpleDateFormat("yyyy-MM-dd");
            Date date = formatoEntrada.parse(fecha);
            fechaFormateada = formatoSalida.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            // Si falla, usa la fecha sin formato
            fechaFormateada = fecha;
        }

        // Si tienes solo hora y minutos, agrega los segundos manualmente
        String horaConSegundos = hora + ":00";

        Map<String, String> params = new HashMap<>();
        params.put("origen", origen);
        params.put("destino", destino);
        params.put("fecha", fechaFormateada);
        params.put("hora", horaConSegundos);

        Log.d(TAG, params.toString());

        return hacerPeticion(recurso, params, METHOD_GET);
    }

    private String obtenerMarquesinas() throws IOException{
        String recurso = "marquesinas.php";
        return hacerPeticion(recurso, new HashMap<>(), METHOD_GET);
    }

    private String buscarRutaCementerio(String fecha) throws IOException{
        String recuerso = "rutaCementeria.php";
        Map<String, String> params = new HashMap<>();
        params.put("fecha", fecha);

        Log.d("DBServer", fecha);
        return hacerPeticion(recuerso, params, METHOD_GET);
    }

    private String paradaCercana(String log, String lat) throws IOException{
        String recuerso = "StopCercana.php";
        Map<String, String> params = new HashMap<>();
        params.put("log", log);
        params.put("lat", lat);
        return hacerPeticion(recuerso, params, METHOD_GET);
    }

    private String guardarFCM(String tokenFCM) throws IOException{
        String recuerso = "guardarFCM.php";
        Map<String, String> params = new HashMap<>();
        params.put("tokenFCM", tokenFCM);
        return hacerPeticion(recuerso, params, METHOD_POST);
    }

    private String olviMail(String mail) throws IOException{
        String recuerso = "restaurarContra.php";
        Map<String, String> params = new HashMap<>();
        params.put("email", mail);
        return hacerPeticion(recuerso, params, METHOD_POST);
    }
}