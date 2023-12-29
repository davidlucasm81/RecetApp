package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.adaptadores.CalendarioAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


@SuppressWarnings("deprecation")
public class CalendarioActivity extends AppCompatActivity {

    private GridView calendarGridView;
    private TextView monthYearTextView;

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(CalendarioActivity.this, MainActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        calendarGridView = findViewById(R.id.calendarGridView);
        monthYearTextView = findViewById(R.id.monthYearTextView);

        ImageButton btnActualizar = findViewById(R.id.btnActualizar);

        btnActualizar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.confirmacion)).setMessage(this.getString(R.string.alerta_actualizar_calendario)).setPositiveButton(this.getString(R.string.aceptar), (dialog, which) -> {
                CalendarioSrv.cargarRecetas(this);
            }).setNegativeButton(this.getString(R.string.cancelar), null).show();
        });

        setupCalendar();
    }

    private void setupCalendar() {
        // Cargamos el calendario
        List<Day> days = CalendarioSrv.obtenerCalendario(this);

        // Set the month and year in the TextView
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthYearTextView.setText(monthYearFormat.format(Calendar.getInstance().getTime()));

        CalendarioAdapter calendarAdapter = new CalendarioAdapter(this, days);
        calendarGridView.setAdapter(calendarAdapter);
    }

}
