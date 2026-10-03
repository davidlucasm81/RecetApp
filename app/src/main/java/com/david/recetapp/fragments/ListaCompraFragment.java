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
import android.widget.Toast;

import java.util.Calendar;

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

        ImageButton btnCopiar = rootView.findViewById(R.id.btnCopiar);
        ImageButton btnCortar = rootView.findViewById(R.id.btnCortar);
        ImageButton btnCompartir = rootView.findViewById(R.id.btnCompartir);
        ImageButton btnBorrar = rootView.findViewById(R.id.btnBorrar);
        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);

        btnCopiar.setOnClickListener(v -> copiarLista());
        btnCortar.setOnClickListener(v -> cortarLista());
        btnCompartir.setOnClickListener(v -> compartirLista());
        btnBorrar.setOnClickListener(v -> mostrarDialogoBorrar());
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

        Button btnFechaInicio = dialogView.findViewById(R.id.btnFechaInicio);
        Button btnFechaFin = dialogView.findViewById(R.id.btnFechaFin);

        final java.time.LocalDate[] startDate = { java.time.LocalDate.now() };
        final java.time.LocalDate[] endDate = { startDate[0].plusDays(6) };

        Runnable updateButtonTexts = () -> {
            btnFechaInicio.setText(String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", startDate[0].getDayOfMonth(), startDate[0].getMonthValue(), startDate[0].getYear()));
            btnFechaFin.setText(String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", endDate[0].getDayOfMonth(), endDate[0].getMonthValue(), endDate[0].getYear()));
        };
        updateButtonTexts.run();

        btnFechaInicio.setOnClickListener(v -> {
            android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                startDate[0] = java.time.LocalDate.of(year, month + 1, dayOfMonth);
                if (startDate[0].isAfter(endDate[0])) {
                    endDate[0] = startDate[0];
                }
                updateButtonTexts.run();
            }, startDate[0].getYear(), startDate[0].getMonthValue() - 1, startDate[0].getDayOfMonth());
            dpd.show();
        });

        btnFechaFin.setOnClickListener(v -> {
            android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                endDate[0] = java.time.LocalDate.of(year, month + 1, dayOfMonth);
                if (endDate[0].isBefore(startDate[0])) {
                    startDate[0] = endDate[0];
                }
                updateButtonTexts.run();
            }, endDate[0].getYear(), endDate[0].getMonthValue() - 1, endDate[0].getDayOfMonth());
            dpd.show();
        });

        AlertDialog alert = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.seleccionar_dias))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.aceptar), null)
                .setNegativeButton(getString(R.string.cancelar), null)
                .create();

        alert.setOnShowListener(dialogInterface -> {
            Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (isAdded()) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            }
            positiveButton.setOnClickListener(v -> {
                if (startDate[0].isAfter(endDate[0])) {
                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_rango_fechas_invalido), Toast.LENGTH_LONG).show();
                    return;
                }
                alert.dismiss();
                generarListaCompra(startDate[0], endDate[0]);
            });
        });

        alert.show();
    }

    private void copiarLista() {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) {
            UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_vacia), Toast.LENGTH_SHORT).show();
            return;
        }
        UtilsSrv.copiarAlPortapapeles(requireContext(), getString(R.string.lista_compra), texto);
        UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_copiada), Toast.LENGTH_SHORT).show();
    }

    private void cortarLista() {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) {
            UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_vacia), Toast.LENGTH_SHORT).show();
            return;
        }
        UtilsSrv.copiarAlPortapapeles(requireContext(), getString(R.string.lista_compra), texto);
        editText.setText("");
        guardarTexto("");
        UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_cortada), Toast.LENGTH_SHORT).show();
    }

    private void compartirLista() {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) {
            UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_vacia), Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent sendIntent = new android.content.Intent();
        sendIntent.setAction(android.content.Intent.ACTION_SEND);
        sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, texto);
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.lista_compra));
        sendIntent.setType("text/plain");

        android.content.Intent shareIntent = android.content.Intent.createChooser(sendIntent, getString(R.string.compartir_lista_compra));
        startActivity(shareIntent);
    }

    private void mostrarDialogoBorrar() {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) {
            UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra_vacia), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog alert = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.confirmacion))
                .setMessage(getString(R.string.confirmar_borrar_lista_compra))
                .setPositiveButton(getString(R.string.si), (dialog, which) -> {
                    editText.setText("");
                    guardarTexto("");
                    UtilsSrv.notificacion(requireContext(), getString(R.string.lista_compra), Toast.LENGTH_SHORT).show();
                })
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

    private void generarListaCompra(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        handler.removeCallbacksAndMessages(null);

        CalendarioSrv.getListaCompra(getContext(), startDate, endDate, new CalendarioSrv.ListaCompraCallback() {
            @Override
            public void onSuccess(String listaCompra) {
                if (!isAdded()) return;

                handler.post(() -> {
                    if (!isAdded()) return;

                    String textoActual = editText.getText().toString().trim();
                    String listaGenerada = listaCompra != null ? listaCompra.trim() : "";
                    String nuevoTexto = textoActual.isEmpty() ? listaGenerada : textoActual + "\n" + listaGenerada;
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