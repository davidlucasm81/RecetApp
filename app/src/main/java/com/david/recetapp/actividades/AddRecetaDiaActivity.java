package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddRecetaDiaActivity extends AppCompatActivity {
    private Day selectedDay;

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        volverADetalleDiaActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receta_dia);
        selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);

        List<Receta> listaRecetas = obtenerListaRecetas();
        TextView textViewEmpty = findViewById(R.id.textViewEmpty);
        if (listaRecetas == null || listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Se muestra el TextView
        }
        // Crear dinámicamente botones para cada receta
        if (listaRecetas != null) {
            // Obtener el mes y año actuales
            Calendar calendarComparar = Calendar.getInstance();
            int mesActual = calendarComparar.get(Calendar.MONTH); // 0-11 (Enero es 0)
            int anioActual = calendarComparar.get(Calendar.YEAR);
            // Crear la fecha a comparar
            calendarComparar.set(anioActual, mesActual, selectedDay.getDayOfMonth());
            // Fecha actual (hoy)
            Date fechaElegida = calendarComparar.getTime();
            // Fecha hace 2 semanas
            Calendar calendarioIntervaloPrevio = (Calendar) calendarComparar.clone();
            calendarioIntervaloPrevio.add(Calendar.MONTH, -1);
            Date fechaIntervaloPrevio = calendarioIntervaloPrevio.getTime();

            for (Receta receta : listaRecetas) {
                Button recetaButton = obtenerBotonAdd(receta, fechaElegida, fechaIntervaloPrevio);
                // Agregar el botón al contenedor
                buttonContainer.addView(recetaButton);
            }
        }
    }

    private @NonNull Button obtenerBotonAdd(Receta receta, Date fechaElegida, Date fechaIntervaloPrevio) {
        Button recetaButton = new Button(this);
        recetaButton.setText(receta.getNombre());

        Date fechaComparar = receta.getFechaCalendario();
        // Color si es postre
        if (receta.isPostre()) {
            recetaButton.setBackgroundResource(R.drawable.dessert_background);
        } else {
            // Color por fechas
            if (fechaComparar.after(fechaIntervaloPrevio) && fechaComparar.before(fechaElegida)) {
                recetaButton.setBackgroundResource(R.drawable.previous_selected_background);
            } else {
                recetaButton.setBackgroundResource(R.drawable.edittext_background);
            }
        }

        recetaButton.setOnClickListener(v -> {
            // Mostrar un diálogo de confirmación
            showConfirmationDialog(receta);
        });
        return recetaButton;
    }

    private List<Receta> obtenerListaRecetas() {
        return RecetasSrv.cargarListaRecetasCalendario(this, selectedDay.getRecetas());
    }

    // Mostrar un diálogo de confirmación
    private void showConfirmationDialog(Receta receta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Activity activity = this;

        builder.setTitle(activity.getString(R.string.Confirmacion))
                .setMessage(activity.getString(R.string.quieres_anadir_receta) + " " + receta.getNombre() + "?")
                .setPositiveButton(activity.getString(R.string.si), (dialog, which) -> showNumberTextDialog(receta))
                .setNegativeButton("No", (dialog, which) -> {
                    // No se hace nada
                }).show();
    }

    private void showNumberTextDialog(Receta receta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER); // Permite ingresar solo números
        editText.setHint(getString(R.string.numero_personas)); // Puedes agregar un hint si lo deseas
        editText.setText(String.valueOf(receta.getNumPersonas())); // Valor por defecto
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        builder.setTitle(R.string.numero_personas)
                .setView(editText)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    String input = editText.getText().toString();
                    if (!input.isEmpty()) {
                        try {
                            int numPersonas = Integer.parseInt(input);
                            if (numPersonas >= 1) {
                                selectedDay.getRecetas().add(new RecetaDia(receta.getId(), numPersonas));
                                CalendarioSrv.actualizarDia(this, selectedDay);
                                volverADetalleDiaActivity();
                            } else {
                                UtilsSrv.notificacion(this, getString(R.string.numero_personas_incorrecto), Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            UtilsSrv.notificacion(this, getString(R.string.numero_personas_incorrecto), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancelar, (dialog, which) -> {
                    // No hacer nada si se cancela
                })
                .show();
    }

    // Volver a DetalleDiaActivity
    private void volverADetalleDiaActivity() {
        Intent intent = new Intent(this, DetalleDiaActivity.class);
        intent.putExtra("selectedDay", selectedDay);
        startActivity(intent);
        finish(); // Esto cierra la actividad actual
    }
}

