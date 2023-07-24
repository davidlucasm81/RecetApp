package com.david.recetapp.actividades;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;

public class ListaCompraActivity extends AppCompatActivity {
    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(ListaCompraActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
