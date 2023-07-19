package com.david.recetapp.actividades;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Calendario;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarioActivity extends AppCompatActivity {

    private Calendario calendario;
    private Button btnLunes, btnMartes, btnMiercoles, btnJueves, btnViernes, btnSabado, btnDomingo;

    private int diasLimite = 0;

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
        ImageButton btnEliminar = findViewById(R.id.btnEliminar);
        btnEliminar.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.confirmacion)).setMessage(this.getString(R.string.alerta_eliminar_calendario)).setPositiveButton(this.getString(R.string.aceptar), (dialog, which) -> {
                File file = new File(getFilesDir(), "calendario.json");
                if (file.exists()) {
                    if (file.delete()) {
                        Toast.makeText(CalendarioActivity.this, this.getString(R.string.calendario_eliminado), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CalendarioActivity.this, "DEV -> ERROR BORRANDO", Toast.LENGTH_SHORT).show();
                    }
                }
                // Refrescamos fechas de las recetas:
                refrescarFechasRecetas();

                Intent intent = new Intent(CalendarioActivity.this, MainActivity.class);
                intent.putExtra("aviso-calendario-eliminado", this.getString(R.string.calendario_eliminado));
                // Iniciar la actividad y pasar el Intent
                startActivity(intent);
            }).setNegativeButton(this.getString(R.string.cancelar), null).show();
        });
    }

    private void refrescarFechasRecetas() {
        List<Receta> listaRecetas = new ArrayList<>();

        try {
            // Cargar el archivo JSON desde el almacenamiento interno
            FileInputStream fis = openFileInput("lista_recetas.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }

            br.close();
            isr.close();
            fis.close();

            // Convertir el JSON a una lista de objetos Receta utilizando GSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Receta>>() {
            }.getType();
            listaRecetas = gson.fromJson(jsonBuilder.toString(), listType);
        } catch (FileNotFoundException e) {
            // Si el archivo no existe, se crea una nueva lista vacía
            listaRecetas = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
        listaRecetas.forEach(r -> r.setFechaCalendario(new Date(0)));
        try {
            // Convertir la lista de objetos Receta a JSON utilizando GSON
            Gson gson = new Gson();
            String json = gson.toJson(listaRecetas);

            // Guardar el JSON en el almacenamiento interno
            FileOutputStream fos = openFileOutput("lista_recetas.json", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Calendario calendario = new Calendario();
            CalendarioBean calendarioBean = gson.fromJson(br, CalendarioBean.class);
            if (calendarioBean == null) {
                throw new FileNotFoundException();
            }
            calendario.setCalendarioBean(calendarioBean);
            br.close();
            isr.close();
            fis.close();
            return calendario;
        } catch (FileNotFoundException | NullPointerException e) {
            // El archivo no existe, se crea un nuevo Calendario
            Calendario calendario = new Calendario(getApplicationContext(), diasLimite);
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
        return diaActual == Calendar.MONDAY && (calendario.getCalendarioBean().getUltimaActualizacion() < calendar.getTimeInMillis() || calendario.getCalendarioBean().getUltimaActualizacion() == 0);
    }

    private void actualizarCalendario(Calendario calendario) {
        // Realizar la lógica de actualización del Calendario
        // Establecer la última actualización con la fecha y hora actual
        Toast.makeText(CalendarioActivity.this, this.getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
        calendario.getCalendarioBean().setUltimaActualizacion(System.currentTimeMillis());

        // Guardar el Calendario en "calendario.json"
        try {
            // Convertir la lista de objetos Receta a JSON utilizando GSON
            Gson gson = new Gson();
            String json = gson.toJson(calendario.getCalendarioBean());

            // Guardar el JSON en el almacenamiento interno
            FileOutputStream fos = openFileOutput("calendario.json", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
