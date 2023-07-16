package com.david.recetapp.actividades;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Calendario;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarioActivity extends AppCompatActivity {

    private Calendario calendario;
    private Button btnLunes, btnMartes, btnMiercoles, btnJueves, btnViernes, btnSabado, btnDomingo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);
        // Cargar el Calendario desde "calendario.json"
        this.calendario = cargarCalendario();

        // Verificar si el Calendario necesita ser actualizado
        if (necesitaActualizar(calendario)) {
            // Actualizar el Calendario
            actualizarCalendario(calendario);
        }
        // Obtener referencias a los elementos de la interfaz
        btnLunes = findViewById(R.id.btn_lunes);
        btnMartes = findViewById(R.id.btn_martes);
        btnMiercoles = findViewById(R.id.btn_miercoles);
        btnJueves = findViewById(R.id.btn_jueves);
        btnViernes = findViewById(R.id.btn_viernes);
        btnSabado = findViewById(R.id.btn_sabado);
        btnDomingo = findViewById(R.id.btn_domingo);

        // Obtener la fecha actual
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // Obtener el día de la semana actual
        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);
        // Crea un arreglo con todos los botones
        Button[] botones = {btnLunes, btnMartes, btnMiercoles, btnJueves, btnViernes, btnSabado, btnDomingo};

        // Resaltar el botón correspondiente al día actual
        resaltarBoton(diaActual);
        for(int i=0;i<7-calendario.getListaRecetas().size();i++){
            botones[i].setBackgroundColor(Color.DKGRAY);
        }

        // Asigna un OnClickListener a cada botón utilizando un bucle
        int diaSeleccionado = 0;
        for (int i = 7-calendario.getListaRecetas().size(); i < botones.length; i++) {
            final int dia = diaSeleccionado;
            botones[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Crea un Intent para abrir la actividad RecetasDiaActivity
                    Intent intent = new Intent(CalendarioActivity.this, RecetasDiaActivity.class);
                    // Pasa DiaRecetas
                    intent.putExtra("diaRecetas", calendario.getListaRecetas().get(dia));
                    // Inicia la actividad RecetasDiaActivity
                    startActivity(intent);
                }
            });
            diaSeleccionado++;
        }
    }

    private String obtenerDiaSemana(int diaSemana) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, diaSemana);
        Date date = calendar.getTime();
        return dateFormat.format(date);
    }

    private void resaltarBoton(int diaSemana) {
        Button btnSeleccionado;

        switch (diaSemana) {
            case Calendar.MONDAY:
                btnSeleccionado = btnLunes;
                break;
            case Calendar.TUESDAY:
                btnSeleccionado = btnMartes;
                break;
            case Calendar.WEDNESDAY:
                btnSeleccionado = btnMiercoles;
                break;
            case Calendar.THURSDAY:
                btnSeleccionado = btnJueves;
                break;
            case Calendar.FRIDAY:
                btnSeleccionado = btnViernes;
                break;
            case Calendar.SATURDAY:
                btnSeleccionado = btnSabado;
                break;
            case Calendar.SUNDAY:
                btnSeleccionado = btnDomingo;
                break;
            default:
                return;
        }

        // Cambiar el color de fondo del botón seleccionado
        btnSeleccionado.setBackgroundColor(Color.rgb(0, 103, 75));
    }

    private Calendario cargarCalendario() {
        try {
            FileInputStream fis = openFileInput("calendario.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            Gson gson = new Gson();
            Calendario calendario = gson.fromJson(br, Calendario.class);

            br.close();
            isr.close();
            fis.close();

            return calendario;
        } catch (FileNotFoundException e) {
            // El archivo no existe, se crea un nuevo Calendario
            Calendario calendario = new Calendario(getApplicationContext());
            actualizarCalendario(calendario);
            return calendario;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean necesitaActualizar(Calendario calendario) {
        Calendar calendar = Calendar.getInstance();
        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);

        // Verificar si es lunes y el calendario no se ha actualizado hoy o si no existe
        if (diaActual == Calendar.MONDAY && (calendario.getUltimaActualizacion() < calendar.getTimeInMillis() || calendario.getUltimaActualizacion() == 0)) {
            return true;
        }

        return false;
    }

    private void actualizarCalendario(Calendario calendario) {
        // Realizar la lógica de actualización del Calendario
        // Establecer la última actualización con la fecha y hora actual
        calendario.setUltimaActualizacion(System.currentTimeMillis());

        // Guardar el Calendario en "calendario.json"
        try {
            FileWriter fileWriter = new FileWriter("calendario.json");
            Gson gson = new Gson();
            gson.toJson(calendario, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
