package com.example.unigo.database;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

public class DBInfoPesada {
    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/alarrazabal025/WEB/";
    private static final String TAG = "DBDatos";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer
    private static final int MAX_MEMORY_RESPONSE_SIZE = 1048576; // 1MB (respuestas más grandes se guardarán en archivo)
    private final Context context;

    public DBInfoPesada(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface ApiCallback {
        void onSuccess(JSONObject responseJson);
        void onSuccess(File responseFile);
        void onSuccess(String responseString);
        void onError(String errorMessage);
    }

    public void obtMarquesinas(ApiCallback callback) {
        String recurso = "marquesinas.php";
        hacerPeticionAsync(recurso, new HashMap<>(), callback, false); // Using GET by default
    }

    private void hacerPeticionAsync(String endpoint, Map<String, String> params, ApiCallback callback, boolean usePost) {
        new Thread(() -> {
            try {
                // Validación de parámetros de entrada
                if (endpoint == null || endpoint.trim().isEmpty()) {
                    throw new IllegalArgumentException("El endpoint no puede ser nulo o vacío");
                }

                HttpURLConnection urlConnection = null;
                InputStream inputStream = null;

                try {
                    // Construir URL con validación
                    String fullUrl = BASE_URL + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);

                    // For GET requests, append parameters to URL if they exist
                    if (!usePost && !params.isEmpty()) {
                        fullUrl += "?" + buildQueryString(params);
                    }

                    URL url = new URL(fullUrl);
                    Log.d(TAG, "Conectando a: " + fullUrl);

                    // Configurar conexión
                    urlConnection = (HttpURLConnection) url.openConnection();

                    if (usePost) {
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setDoOutput(true);
                        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    } else {
                        urlConnection.setRequestMethod("GET");
                    }

                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setReadTimeout(30000); // Aumentado para respuestas grandes

                    // For POST requests, send parameters in the body
                    if (usePost && !params.isEmpty()) {
                        String postData = buildQueryString(params);
                        Log.d(TAG, "Enviando parámetros POST: " + postData);

                        try (OutputStream os = urlConnection.getOutputStream()) {
                            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                            os.flush();
                        }
                    }

                    // Procesar respuesta
                    int responseCode = urlConnection.getResponseCode();
                    Log.d(TAG, "Código de respuesta: " + responseCode);

                    if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());

                        // Verificar el tamaño del contenido si está disponible
                        int contentLength = urlConnection.getContentLength();
                        Log.d(TAG, "Tamaño del contenido: " + contentLength + " bytes");

                        // Determinar si guardar en archivo o procesar en memoria
                        if (contentLength > MAX_MEMORY_RESPONSE_SIZE) {
                            File responseFile = guardarRespuestaEnArchivo(inputStream);
                            callback.onSuccess(responseFile);
                        } else {
                            // Procesar en memoria
                            String responseString = leerStream(inputStream);
                            try {
                                JSONObject jsonResponse = new JSONObject(responseString);
                                callback.onSuccess(jsonResponse);
                            } catch (JSONException e) {
                                // If not JSON, return as plain string
                                callback.onSuccess(responseString);
                            }
                        }
                    } else {
                        // Manejar errores HTTP
                        inputStream = urlConnection.getErrorStream();
                        String errorMessage = leerStream(inputStream);
                        throw new IOException("Error en el servidor: " + responseCode + " - " + errorMessage);
                    }
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error al cerrar inputStream", e);
                        }
                    }
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } catch (MalformedURLException e) {
                callback.onError("URL mal formada: " + e.getMessage());
            } catch (SocketTimeoutException e) {
                callback.onError("Tiempo de espera agotado al conectar con el servidor");
            } catch (SSLHandshakeException e) {
                callback.onError("Error de seguridad SSL: " + e.getMessage());
            } catch (IOException e) {
                callback.onError("Error de comunicación: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Error inesperado: " + e.getMessage());
            }
        }).start();
    }

    private String buildQueryString(Map<String, String> params) throws IOException {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (param.getKey() == null || param.getValue() == null) {
                Log.w(TAG, "Parámetro nulo detectado - Key: " + param.getKey() + ", Value: " + param.getValue());
                continue;
            }

            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        return postData.toString();
    }

    private File guardarRespuestaEnArchivo(InputStream inputStream) throws IOException {
        File outputFile = File.createTempFile("api_response_", ".json", context.getCacheDir());
        Log.d(TAG, "Guardando respuesta grande en archivo: " + outputFile.getAbsolutePath());

        try (OutputStream outputStream = Files.newOutputStream(outputFile.toPath())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // Opcional: Log cada 5MB procesados
                if (totalBytes % (5 * 1024 * 1024) == 0) {
                    Log.d(TAG, "Procesados " + (totalBytes / (1024 * 1024)) + "MB");
                }
            }

            Log.d(TAG, "Total de bytes guardados: " + totalBytes);
        }

        return outputFile;
    }

    private String leerStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char[] buffer = new char[BUFFER_SIZE];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                responseBuilder.append(buffer, 0, charsRead);
            }
        }

        return responseBuilder.toString();
    }
}