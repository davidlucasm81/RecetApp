package com.david.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.actividades.AddRecetasActivity;
import com.david.recetapp.actividades.CalendarioActivity;
import com.david.recetapp.actividades.ListaCompraActivity;
import com.david.recetapp.actividades.VerRecetasActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddRecetas = findViewById(R.id.btnAddRecetas);
        Button btnVerRecetas = findViewById(R.id.btnVerRecetas);
        Button btnCalendario = findViewById(R.id.btnCalendario);
        Button btnListaCompra = findViewById(R.id.btnListaCompra);

        btnAddRecetas.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Añadir Recetas"
            // Por ejemplo, puedes abrir una nueva actividad
            Intent intent = new Intent(MainActivity.this, AddRecetasActivity.class);
            startActivity(intent);
        });

        btnVerRecetas.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Ver Recetas"
            // Por ejemplo, puedes abrir una nueva actividad
            Intent intent = new Intent(MainActivity.this, VerRecetasActivity.class);
            startActivity(intent);
        });

        btnCalendario.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Calendario Semanal de Recetas"
            // Por ejemplo, puedes abrir una nueva actividad
            Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
            startActivity(intent);
        });

        btnListaCompra.setOnClickListener(v -> {
            // Acción al hacer click en el botón "Lista de la Compra"
            // Por ejemplo, puedes abrir una nueva actividad
            Intent intent = new Intent(MainActivity.this, ListaCompraActivity.class);
            startActivity(intent);
        });
    }
}