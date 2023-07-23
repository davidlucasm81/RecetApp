package com.david.recetapp.actividades;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.ConfiguracionSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.io.File;

public class AjustesCalendarioActivity extends AppCompatActivity {
    private EditText editTextCantidad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes_calendario);

        Button btnEliminar = findViewById(R.id.btnEliminar);
        btnEliminar.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.confirmacion)).setMessage(this.getString(R.string.alerta_eliminar_calendario)).setPositiveButton(this.getString(R.string.aceptar), (dialog, which) -> {
                File file = new File(getFilesDir(), "calendario.json");
                if (file.exists()) {
                    if (file.delete()) {
                        Toast.makeText(AjustesCalendarioActivity.this, this.getString(R.string.calendario_eliminado), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AjustesCalendarioActivity.this, "DEV -> ERROR BORRANDO", Toast.LENGTH_SHORT).show();
                    }
                }
                // Refrescamos fechas de las recetas:
                RecetasSrv.refrescarFechasRecetas(this);

                Intent intent = new Intent(AjustesCalendarioActivity.this, MainActivity.class);
                intent.putExtra("aviso-calendario-eliminado", this.getString(R.string.calendario_eliminado));
                // Iniciar la actividad y pasar el Intent
                startActivity(intent);
            }).setNegativeButton(this.getString(R.string.cancelar), null).show();
        });

        editTextCantidad = findViewById(R.id.editTextCantidad);

        editTextCantidad.setText(String.valueOf(ConfiguracionSrv.getDiasRepeticionReceta(this)));

        Button btnAceptar = findViewById(R.id.btnAceptar);
        btnAceptar.setOnClickListener(view -> {
            ConfiguracionSrv.setDiasRepeticionReceta(this, Integer.parseInt(editTextCantidad.getText().toString()));
            Intent intent = new Intent(AjustesCalendarioActivity.this, CalendarioActivity.class);
            // Iniciar la actividad y pasar el Intent
            startActivity(intent);
        });
    }
}
