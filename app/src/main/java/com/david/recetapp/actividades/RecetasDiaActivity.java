package com.david.recetapp.actividades;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.DiaRecetas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecetasDiaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recetas_dia_activity);

        /// Obtén la lista de recetas y el índice del día seleccionado del Intent
        DiaRecetas diaRecetas = (DiaRecetas) getIntent().getSerializableExtra("diaRecetas");

        // Obten el TextView del título
        TextView tituloTextView = findViewById(R.id.tituloTextView);

        String diaSemana = obtenerDiaSemana(diaRecetas.getFecha());
        tituloTextView.setText(diaSemana);
    }

    // Método para obtener el nombre del día de la semana
    private String obtenerDiaSemana(Date fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(fecha);
    }
}