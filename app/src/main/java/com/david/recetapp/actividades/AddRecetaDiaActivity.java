package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;

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
        selectedDay = (Day) getIntent().getSerializableExtra("selectedDay");
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);

        List<Receta> listaRecetas = obtenerListaRecetas();
        TextView textViewEmpty = findViewById(R.id.textViewEmpty);
        if(listaRecetas == null || listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Se muestra el TextView
        }
        // Crear dinámicamente botones para cada receta
        for (Receta receta : listaRecetas) {
            Button recetaButton = new Button(this);
            recetaButton.setText(receta.getNombre());
            recetaButton.setOnClickListener(v -> {
                // Mostrar un diálogo de confirmación
                showConfirmationDialog(receta);
            });

            // Agregar el botón al contenedor
            buttonContainer.addView(recetaButton);
        }
    }

    private List<Receta> obtenerListaRecetas() {
        return RecetasSrv.cargarListaRecetasCalendario(this, selectedDay.getRecetas());
    }

    // Mostrar un diálogo de confirmación
    private void showConfirmationDialog(Receta receta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Activity activity = this;
        builder.setTitle(activity.getString(R.string.Confirmacion)).setMessage(activity.getString(R.string.quieres_anadir_receta) + " "+receta.getNombre() + "?").setPositiveButton(activity.getString(R.string.si), (dialog, which) -> {
            selectedDay.getRecetas().add(receta.getId());
            CalendarioSrv.actualizarDia(activity, selectedDay);
            volverADetalleDiaActivity();
        }).setNegativeButton("No", (dialog, which) -> {
            // No hacer nada o realizar otra acción según sea necesario
        }).show();
    }

    // Método para volver a DetalleDiaActivity
    private void volverADetalleDiaActivity() {
        Intent intent = new Intent(this, DetalleDiaActivity.class);
        intent.putExtra("selectedDay", selectedDay);
        startActivity(intent);
        finish(); // Esto cierra la actividad actual
    }
}

