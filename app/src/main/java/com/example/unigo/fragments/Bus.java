package com.example.unigo.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo.Home;
import com.example.unigo.R;
import com.example.unigo.database.DBLocal;
import com.example.unigo.database.DBServer;
import com.example.unigo.model.Parada;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class Bus extends Fragment {

    private AutoCompleteTextView inputOrigen, inputDestino;
    private EditText inputFecha, inputHora;
    private Button btnConsultar ;
    private AppCompatImageButton btnFavorito;
    private ListView listSugerenciasOrigen, listSugerenciasDestino;
    private List<Parada> paradas = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();
    private ArrayAdapter<String> adapterOrigen, adapterDestino;
    private TextView textMarquesinas, textCementerio;
    private SharedPreferences prefs;
    private DBLocal dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        prefs = requireActivity().getSharedPreferences("Usuario", Context.MODE_PRIVATE);
        aplicarIdiomaGuardado();
        View view = inflater.inflate(R.layout.fragment_bus, container, false);

        // Inicialización de vistas
        inputOrigen = view.findViewById(R.id.inputOrigen);
        inputDestino = view.findViewById(R.id.inputDestino);
        inputFecha = view.findViewById(R.id.inputFecha);
        inputHora = view.findViewById(R.id.inputHora);
        btnConsultar = view.findViewById(R.id.btnConsultar);
        listSugerenciasOrigen = view.findViewById(R.id.listSugerenciasOrigen);
        listSugerenciasDestino = view.findViewById(R.id.listSugerenciasDestino);
        textMarquesinas = view.findViewById(R.id.textMarquesinas);
        textCementerio = view.findViewById(R.id.textCementerio);
        btnFavorito = view.findViewById(R.id.btnFavorito);

        dbHelper = new DBLocal(getContext());

        // Configuración inicial de los adapters
        adapterOrigen = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );

        adapterDestino = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );

        listSugerenciasOrigen.setAdapter(adapterOrigen);
        listSugerenciasDestino.setAdapter(adapterDestino);

        // Configurar listeners para los ListView
        listSugerenciasOrigen.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = adapterOrigen.getItem(position);
            inputOrigen.setText(selectedItem);
            listSugerenciasOrigen.setVisibility(View.GONE);
            hideKeyboard();
        });

        listSugerenciasDestino.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = adapterDestino.getItem(position);
            inputDestino.setText(selectedItem);
            listSugerenciasDestino.setVisibility(View.GONE);
            hideKeyboard();
        });

        // Configurar focus listeners
        inputOrigen.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && inputOrigen.getText().length() > 0) {
                actualizarSugerencias(inputOrigen.getText().toString(), adapterOrigen, listSugerenciasOrigen);
            } else {
                listSugerenciasOrigen.setVisibility(View.GONE);
            }
        });

        inputDestino.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && inputDestino.getText().length() > 0) {
                actualizarSugerencias(inputDestino.getText().toString(), adapterDestino, listSugerenciasDestino);
            } else {
                listSugerenciasDestino.setVisibility(View.GONE);
            }
        });


        inputFecha.setOnClickListener(v -> mostrarDatePicker());
        inputFecha.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDatePicker();
            }
        });

        // Configurar el diálogo de hora
        inputHora.setOnClickListener(v -> mostrarTimePicker());
        inputHora.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarTimePicker();
            }
        });

        // Establecer fecha y hora de ahora
        establecerFechaYHoraActual();

        // Configurar autocompletado reactivo
        configurarAutocompletadoReactivo();

        // Obtener datos de paradas
        obtenerParadas();


        btnConsultar.setOnClickListener(v -> {
            buscarRutas();
        });


        textMarquesinas.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BusStopsMap())
                    .addToBackStack(null)
                    .commit();
        });


        textCementerio.setOnClickListener(v -> {
            // Crear diálogo para seleccionar fecha
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Seleccione fecha")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // Convertir milisegundos a LocalDate
                LocalDate selectedDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                // Obtener día de la semana
                DayOfWeek selectedDay = selectedDate.getDayOfWeek();

                // Configurar SharedPreferences
                SharedPreferences sharedPref = requireActivity().getSharedPreferences("BusRutas", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("destino", "Cementerio");
                editor.putString("fecha", selectedDate.toString());
                editor.apply();

                Log.d("Cementerio", selectedDate.toString());

                // Verificar si hay rutas según el día seleccionado
                if (selectedDay == DayOfWeek.WEDNESDAY || selectedDay == DayOfWeek.SATURDAY || selectedDay == DayOfWeek.SUNDAY) {
                    // Navegar al fragmento de rutas

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new BusCementerio())
                            .addToBackStack(null)
                            .commit();
                } else {
                    // Mostrar mensaje informativo
                    String dayName = "";
                    switch (selectedDay) {
                        case MONDAY: dayName = "Lunes"; break;
                        case TUESDAY: dayName = "Martes"; break;
                        case WEDNESDAY: dayName = "Miércoles"; break;
                        case THURSDAY: dayName = "Jueves"; break;
                        case FRIDAY: dayName = "Viernes"; break;
                        case SATURDAY: dayName = "Sábado"; break;
                        case SUNDAY: dayName = "Domingo"; break;
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle("No hay rutas disponibles")
                            .setMessage("No hay rutas al cementerio los " + dayName + "s. ¿Desea ver otra fecha?")
                            .setPositiveButton("Sí", (dialog, which) -> datePicker.show(getParentFragmentManager(), "DATE_PICKER"))
                            .setNegativeButton("No", null)
                            .show();
                }
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");


        });
        btnFavorito.setOnClickListener(v1 -> {
            String origen = inputOrigen.getText().toString();
            String destino = inputDestino.getText().toString();

            if(origen.isEmpty() || destino.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa origen y destino", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isFavorite = dbHelper.isRouteFavorite(origen, destino);

            if(!isFavorite) {
                // Añadir a favoritos
                long id = dbHelper.addFavoriteRoute(origen, destino);
                if(id != -1) {
                    btnFavorito.setSupportImageTintList(ColorStateList.valueOf(Color.parseColor("#FFD700")));
                    Log.d("FAVORITOS", "Ruta añadida a favoritos. ID: " + id);
                }
            } else {
                // Quitar de favoritos
                int deleted = dbHelper.deleteFavoriteRoute(origen, destino);
                if(deleted > 0) {
                    btnFavorito.setSupportImageTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3"))); // Gris
                    Log.d("FAVORITOS", "Ruta eliminada de favoritos");
                }
            }
        });
        return view;
    }

    private void mostrarDatePicker() {
        // Obtener la fecha actual
        final Calendar calendario = Calendar.getInstance();
        int año = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int día = calendario.get(Calendar.DAY_OF_MONTH);

        // Crear el DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomDatePickerDialog_Green,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Formatear la fecha seleccionada (mes + 1 porque enero es 0)
                    String fechaSeleccionada = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    inputFecha.setText(fechaSeleccionada);
                },
                año, mes, día);

        // Configurar fecha mínima como hoy
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // Mostrar el diálogo
        datePickerDialog.show();
    }

    private void mostrarTimePicker() {
        // Obtener la hora actual
        final Calendar calendario = Calendar.getInstance();
        int hora = calendario.get(Calendar.HOUR_OF_DAY);
        int minuto = calendario.get(Calendar.MINUTE);


        // Crear el TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                R.style.CustomDatePickerDialog_Green,
                (view, selectedHour, selectedMinute) -> {
                    // Formatear la hora seleccionada
                    String horaSeleccionada = String.format("%02d:%02d", selectedHour, selectedMinute);
                    inputHora.setText(horaSeleccionada);
                },
                hora, minuto, true); // true para formato de 24 horas

        // Mostrar el diálogo
        timePickerDialog.show();
    }


    private void establecerFechaYHoraActual() {
        Calendar calendario = Calendar.getInstance();

        // Formatear fecha según el locale del dispositivo
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaActual = formatoFecha.format(calendario.getTime());

        // Formatear hora según el locale del dispositivo
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String horaActual = formatoHora.format(calendario.getTime());

        inputFecha.setText(fechaActual);
        inputHora.setText(horaActual);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void configurarAutocompletadoReactivo() {
        // Observable para inputOrigen
        disposables.add(Observable.<String>create(emitter -> {
                    TextWatcher watcher = new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void afterTextChanged(Editable s) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (!emitter.isDisposed()) {
                                emitter.onNext(s.toString());
                            }
                        }
                    };
                    inputOrigen.addTextChangedListener(watcher);
                    emitter.setCancellable(() -> inputOrigen.removeTextChangedListener(watcher));
                })
                .debounce(300, TimeUnit.MILLISECONDS)
                .filter(text -> text != null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(text -> {
                    Log.d("RxJava", "Texto origen cambiado: " + text);
                    actualizarSugerencias(text, adapterOrigen, listSugerenciasOrigen);
                }));

        // Observable para inputDestino
        disposables.add(Observable.<String>create(emitter -> {
                    TextWatcher watcher = new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void afterTextChanged(Editable s) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (!emitter.isDisposed()) {
                                emitter.onNext(s.toString());
                            }
                        }
                    };
                    inputDestino.addTextChangedListener(watcher);
                    emitter.setCancellable(() -> inputDestino.removeTextChangedListener(watcher));
                })
                .debounce(300, TimeUnit.MILLISECONDS)
                .filter(text -> text != null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(text -> {
                    Log.d("RxJava", "Texto destino cambiado: " + text);
                    actualizarSugerencias(text, adapterDestino, listSugerenciasDestino);
                }));
    }

    private void actualizarSugerencias(String busqueda, ArrayAdapter<String> adapter, ListView listView) {
        Log.d("RxJava", "=== Actualizando sugerencias ===");
        Log.d("RxJava", "Búsqueda: '" + busqueda + "'");
        Log.d("RxJava", "Total paradas disponibles: " + paradas.size());

        List<String> sugerencias = new ArrayList<>();

        if (paradas.isEmpty()) {
            Log.d("RxJava", "¡ATENCIÓN! No hay paradas cargadas");
            return;
        }

        // Mostrar TODAS las paradas cuando no hay búsqueda (texto vacío)
        if (busqueda.isEmpty()) {
            for (Parada parada : paradas) {
                if (parada.getNombre() != null) {
                    sugerencias.add(parada.getNombre());
                }
            }
        } else {
            // Mostrar sugerencias que coincidan con la búsqueda
            for (Parada parada : paradas) {
                if (parada.getNombre() != null &&
                        parada.getNombre().toLowerCase().contains(busqueda.toLowerCase())) {
                    sugerencias.add(parada.getNombre());
                }
            }
        }

        Log.d("RxJava", "Sugerencias encontradas: " + sugerencias.size());

        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                adapter.clear();
                if (!sugerencias.isEmpty()) {
                    adapter.addAll(sugerencias);
                    listView.setVisibility(View.VISIBLE);

                    // Ajustar altura del ListView para mostrar al menos 3 elementos o todos si son menos
                    int itemHeight = (int) (48 * getResources().getDisplayMetrics().density); // Altura estimada por item
                    int totalHeight = itemHeight * Math.min(sugerencias.size(), 3); // Mostrar máximo 3 items de altura

                    ViewGroup.LayoutParams params = listView.getLayoutParams();
                    params.height = totalHeight;
                    listView.setLayoutParams(params);
                    listView.requestLayout();

                    Log.d("RxJava", "ListView hecho visible con " + sugerencias.size() + " elementos");
                } else {
                    listView.setVisibility(View.GONE);
                    Log.d("RxJava", "ListView ocultado");
                }
                adapter.notifyDataSetChanged();
            });
        }
    }

    private void obtenerParadas() {
        Log.d("RxJava", "Iniciando obtención de paradas...");
        Data inputData = new Data.Builder()
                .putString("action", "obtBusParadas")
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DBServer.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String result = workInfo.getOutputData().getString("result");
                            Log.d("RxJava", "Work request exitoso. Procesando respuesta...");
                            procesarRespuesta(result);
                        } else {
                            Log.e("RxJava", "Work request falló");
                            mostrarError("Error al obtener paradas");
                        }
                    }
                });

        WorkManager.getInstance(requireContext()).enqueue(workRequest);
    }

    private void procesarRespuesta(String jsonResponse) {
        Log.d("RxJava", "=== Procesando respuesta ===");
        Log.d("RxJava", "JSON recibido: " + jsonResponse);
        try {
            JSONArray paradasArray = new JSONArray(jsonResponse);
            paradas.clear();

            for (int i = 0; i < paradasArray.length(); i++) {
                String nombreParada = paradasArray.getString(i);
                if (nombreParada != null && !nombreParada.trim().isEmpty()) {
                    Parada p = new Parada(nombreParada);
                    paradas.add(p);
                    //Log.d("RxJava", "Parada agregada: " + p.getNombre());
                }
            }
            Log.d("RxJava", "Total paradas cargadas: " + paradas.size());

        } catch (JSONException e) {
            mostrarError("Error al procesar respuesta");
            Log.e("BusFragment", "Error JSON: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }

    private void buscarRutas() {
        String origen = inputOrigen.getText().toString().trim();
        String destino = inputDestino.getText().toString().trim();
        String fecha = inputFecha.getText().toString().trim();
        String hora = inputHora.getText().toString().trim();

        if (TextUtils.isEmpty(origen)) {
            inputOrigen.setError("Introduce el origen");
            inputOrigen.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(destino)) {
            inputDestino.setError("Introduce el destino");
            inputDestino.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(fecha)) {
            inputFecha.setError("Introduce la fecha");
            inputFecha.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(hora)) {
            inputHora.setError("Introduce la hora");
            inputHora.requestFocus();
            return;
        }

        SharedPreferences busRutas = getActivity().getSharedPreferences("BusRutas", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = busRutas.edit();
        editor.putString("origen", origen);
        editor.putString("destino", destino);
        editor.putString("fecha", fecha);
        editor.putString("hora", hora);
        editor.apply();


        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BusRutas())
                .addToBackStack(null)
                .commit();

    }

    private void aplicarIdiomaGuardado() {
        String idioma = prefs.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
    }

}