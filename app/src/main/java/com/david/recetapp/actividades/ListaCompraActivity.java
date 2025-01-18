package com.david.recetapp.actividades;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

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
            // Inflar el diseño del selector de rango de fechas
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_date_range_picker, null);

            // Obtener referencias a los componentes del diseño
            NumberPicker numberPickerInicio = dialogView.findViewById(R.id.numberPickerInicio);
            NumberPicker numberPickerFin = dialogView.findViewById(R.id.numberPickerFin);

            // Configurar los NumberPickers para mostrar los días del mes
            numberPickerInicio.setMinValue(1);
            numberPickerInicio.setMaxValue(31);
            numberPickerFin.setMinValue(1);
            numberPickerFin.setMaxValue(31);

            // Establecer listeners para ajustar dinámicamente el rango
            numberPickerInicio.setOnValueChangedListener((picker, oldVal, newVal) -> {
                // Asegurarse de que el día de fin sea siempre mayor al día de inicio
                numberPickerFin.setMinValue(newVal + 1); // El día de fin debe ser al menos un día después
            });

            numberPickerFin.setOnValueChangedListener((picker, oldVal, newVal) -> {
                // Asegurarse de que el día de inicio no exceda el día de fin
                numberPickerInicio.setMaxValue(newVal - 1); // El día de inicio debe ser al menos un día antes
            });

            // Crear el diálogo
            AlertDialog alert = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                    .setTitle(this.getString(R.string.seleccionar_dias))
                    .setView(dialogView)
                    .setPositiveButton(this.getString(R.string.aceptar), (dialog, which) -> {
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(() -> {
                            // Guardar automáticamente el texto ingresado y el día de la semana
                            String text = editText.getText() + "\n" + CalendarioSrv.obtenerListaCompraSemana(this, numberPickerInicio.getValue(), numberPickerFin.getValue());
                            // Guardar automáticamente el texto ingresado y el día de la semana
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putString(TEXT_KEY, text);
                            editor.putInt(DAY_KEY, getCurrentDayOfMonth());
                            editor.apply();
                            editText.setText(text);
                            UtilsSrv.notificacion(ListaCompraActivity.this, this.getString(R.string.lista_compra), Toast.LENGTH_SHORT).show();
                        }, GUARDAR_DELAY_MS);
                    })
                    .setNegativeButton(this.getString(R.string.cancelar), null)
                    .create();

            // Asegúrate de que los botones tengan el color adecuado
            alert.setOnShowListener(dialogInterface -> {
                Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);

                // Aquí asignamos manualmente el color del texto de los botones
                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            });
            // Mostrar el diálogo
            alert.show();
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
