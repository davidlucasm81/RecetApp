package com.david.recetapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import android.os.Bundle;

import java.time.LocalDate;

public class ListaCompraFragment extends Fragment {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String TEXT_KEY = "savedText";
    private static final String DAY_KEY = "numeroDia";
    private static final long GUARDAR_DELAY_MS = 1000; // Retraso de 1 segundo

    private final Handler handler = new Handler(Looper.getMainLooper());
    private EditText editText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lista_compra, container, false);

        editText = rootView.findViewById(R.id.editText);

        // Cargar el texto guardado al iniciar
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedText = prefs.getString(TEXT_KEY, "");
        editText.setText(savedText);

        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(v -> mostrarDialogoRangoFechas());

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
                    if (isAdded()) {
                        guardarTexto(editable.toString());
                    }
                }, GUARDAR_DELAY_MS);
            }
        });

        return rootView;
    }

    private void mostrarDialogoRangoFechas() {
        LayoutInflater inflaterDialog = LayoutInflater.from(getContext());
        View dialogView = inflaterDialog.inflate(R.layout.dialog_date_range_picker, null);

        NumberPicker numberPickerInicio = dialogView.findViewById(R.id.numberPickerInicio);
        NumberPicker numberPickerFin = dialogView.findViewById(R.id.numberPickerFin);

        // Configurar los NumberPickers
        numberPickerInicio.setMinValue(1);
        numberPickerInicio.setMaxValue(31);
        numberPickerFin.setMinValue(1);
        numberPickerFin.setMaxValue(31);

        // Establecer listeners para ajustar dinámicamente el rango
        numberPickerInicio.setOnValueChangedListener((picker, oldVal, newVal) -> numberPickerFin.setMinValue(newVal + 1));

        numberPickerFin.setOnValueChangedListener((picker, oldVal, newVal) -> numberPickerInicio.setMaxValue(newVal - 1));

        AlertDialog alert = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.seleccionar_dias))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) ->
                        generarListaCompra(numberPickerInicio.getValue(), numberPickerFin.getValue()))
                .setNegativeButton(getString(R.string.cancelar), null)
                .create();

        alert.setOnShowListener(dialogInterface -> {
            Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (isAdded()) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            }
        });

        alert.show();
    }

    private void generarListaCompra(int diaInicio, int diaFin) {
        handler.removeCallbacksAndMessages(null);

        CalendarioSrv.getListaCompra(getContext(), diaInicio, diaFin, new CalendarioSrv.ListaCompraCallback() {
            @Override
            public void onSuccess(String listaCompra) {
                if (!isAdded()) return;

                handler.post(() -> {
                    if (!isAdded()) return;

                    String textoActual = editText.getText().toString();
                    String nuevoTexto = textoActual + "\n" + listaCompra;
                    editText.setText(nuevoTexto);
                    guardarTexto(nuevoTexto);

                    UtilsSrv.notificacion(getContext(),
                            getString(R.string.lista_compra),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                handler.post(() -> {
                    if (!isAdded()) return;

                    UtilsSrv.notificacion(getContext(),
                            getString(R.string.error_generar_lista_compra),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void guardarTexto(String texto) {
        SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(TEXT_KEY, texto);
        editor.putInt(DAY_KEY, getCurrentDayOfMonth());
        editor.apply();
    }

    private int getCurrentDayOfMonth() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.getDayOfMonth();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}