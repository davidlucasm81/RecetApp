package com.david.recetapp.actividades;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.time.LocalDate;

public class ListaCompraActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String TEXT_KEY = "savedText";
    private static final String DAY_KEY = "numeroDia";
    private static final long GUARDAR_DELAY_MS = 1000; // Retraso de 1 segundo
    // Declarar una variable de clase para el temporizador
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_compra);

        EditText editText = findViewById(R.id.editText);

        // Cargar el texto y el día de la semana guardados al iniciar la actividad
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedText = prefs.getString(TEXT_KEY, "");

        ImageButton btnActualizar = findViewById(R.id.btnActualizar);

        btnActualizar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.confirmacion)).setMessage(this.getString(R.string.alerta_actualizar_lista)).setPositiveButton(this.getString(R.string.aceptar), (dialog, which) -> {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> {
                    // Guardar automáticamente el texto ingresado y el día de la semana
                    String text = editText.getText() + "\n" + CalendarioSrv.obtenerListaCompraSemana(this);
                    // Guardar automáticamente el texto ingresado y el día de la semana
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(TEXT_KEY, text);
                    editor.putInt(DAY_KEY, getCurrentDayOfMonth());
                    editor.apply();
                    editText.setText(text);
                }, GUARDAR_DELAY_MS);

            }).setNegativeButton(this.getString(R.string.cancelar), null).show();
        });


        editText.setText(savedText);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Cancelar las operaciones pendientes y programar una nueva
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> {
                    // Guardar automáticamente el texto ingresado y el día de la semana
                    String inputText = editable.toString();
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(TEXT_KEY, inputText);
                    editor.putInt(DAY_KEY, getCurrentDayOfMonth());
                    editor.apply();
                }, GUARDAR_DELAY_MS);
            }
        });
    }

    private int getCurrentDayOfMonth() {
        // Obtener el número del día del mes actual
        LocalDate currentDate = LocalDate.now();
        return currentDate.getDayOfMonth();
    }
}
