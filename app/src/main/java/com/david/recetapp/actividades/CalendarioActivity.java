package com.david.recetapp.actividades;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.util.Calendar;
import java.util.Date;

public class CalendarioActivity extends AppCompatActivity {

    private CalendarioBean calendario;
    private Button btnLunes, btnMartes, btnMiercoles, btnJueves, btnViernes, btnSabado, btnDomingo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);
        // Cargar el Calendario
        this.calendario = CalendarioSrv.cargarCalendario(this);
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
        for (int i = 0; i < 7 - calendario.getListaRecetas().size(); i++) {
            botones[i].setBackgroundColor(Color.DKGRAY);
        }

        // Asigna un OnClickListener a cada botón utilizando un bucle
        int diaSeleccionado = 0;
        for (int i = 7 - calendario.getListaRecetas().size(); i < botones.length; i++) {
            final int dia = diaSeleccionado;
            botones[i].setOnClickListener(view -> {
                // Crea un Intent para abrir la actividad RecetasDiaActivity
                Intent intent = new Intent(CalendarioActivity.this, RecetasDiaActivity.class);
                // Pasa DiaRecetas
                intent.putExtra("diaRecetas", calendario.getListaRecetas().get(dia));
                // Inicia la actividad RecetasDiaActivity
                startActivity(intent);
            });
            diaSeleccionado++;
        }

        ImageButton btnAjustes = findViewById(R.id.btnAjustes);
        btnAjustes.setOnClickListener(view -> {
            Intent intent = new Intent(CalendarioActivity.this, AjustesCalendarioActivity.class);
            startActivity(intent);
        });
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
}
