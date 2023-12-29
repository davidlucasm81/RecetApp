package com.david.recetapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.actividades.AddRecetasActivity;
import com.david.recetapp.actividades.CalendarioActivity;
import com.david.recetapp.actividades.VerRecetasActivity;

public class MainActivity extends AppCompatActivity {
    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // No hace nada
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddRecetas = findViewById(R.id.btnAddRecetas);
        Button btnVerRecetas = findViewById(R.id.btnVerRecetas);
        Button btnCalendario = findViewById(R.id.btnCalendario);

        btnAddRecetas.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Añadir Recetas"
            Intent intent = new Intent(MainActivity.this, AddRecetasActivity.class);
            startActivity(intent);
        });

        btnVerRecetas.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Ver Recetas"
            Intent intent = new Intent(MainActivity.this, VerRecetasActivity.class);
            startActivity(intent);
        });

        btnCalendario.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Ver Calendario"
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });
    }
}