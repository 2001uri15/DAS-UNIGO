package com.example.unigo.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.unigo.R;

import java.util.Locale;

public class Bici extends Fragment {
    private SharedPreferences prefs2;


    public Bici() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        prefs2 = requireActivity().getSharedPreferences("Ajustes", Context.MODE_PRIVATE);
        aplicarIdiomaGuardado();
        return inflater.inflate(R.layout.fragment_bici, container, false);
    }

    private void aplicarIdiomaGuardado() {
        String idioma = prefs2.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
    }
}