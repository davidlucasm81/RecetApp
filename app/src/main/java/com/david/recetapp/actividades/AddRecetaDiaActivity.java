package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetasAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class AddRecetaDiaActivity extends AppCompatActivity {
    private Day selectedDay;
    private RecyclerView recyclerView;
    private RecetasAdapter adapter;
    private TextView emptyView;
    private ProgressBar progressBar;
    private final Calendar calendarComparar = Calendar.getInstance();
    private final Calendar calendarioIntervaloPrevio = Calendar.getInstance();
    private Date fechaElegida;
    private Date fechaIntervaloPrevio;
    private WeakReference<AlertDialog> currentDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receta_dia);

        initializeViews();
        setupRecyclerView();
        initializeDates();
        loadRecetas();
    }

    private void initializeViews() {
        selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);
        recyclerView = findViewById(R.id.recyclerViewRecetas);
        emptyView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        adapter = new RecetasAdapter(new ArrayList<>(), this::showConfirmationDialog);
        recyclerView.setAdapter(adapter);
    }

    private void initializeDates() {
        int mesActual = calendarComparar.get(Calendar.MONTH);
        int anioActual = calendarComparar.get(Calendar.YEAR);
        calendarComparar.set(anioActual, mesActual, selectedDay.getDayOfMonth());
        fechaElegida = calendarComparar.getTime();

        calendarioIntervaloPrevio.setTime(fechaElegida);
        calendarioIntervaloPrevio.add(Calendar.MONTH, -1);
        fechaIntervaloPrevio = calendarioIntervaloPrevio.getTime();
    }

    private void loadRecetas() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RecetasSrv.cargarListaRecetasCalendario(this, selectedDay.getRecetas(), new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(java.util.List<Receta> listaRecetas) {
                mainHandler.post(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    updateUI(listaRecetas);
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    UtilsSrv.notificacion(AddRecetaDiaActivity.this,
                            getString(R.string.error_cargar_recetas),
                            Toast.LENGTH_SHORT).show();
                    updateUI(new ArrayList<>());
                });
            }
        });
    }

    private void updateUI(java.util.List<Receta> listaRecetas) {
        recyclerView.setVisibility(listaRecetas == null || listaRecetas.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(listaRecetas == null || listaRecetas.isEmpty() ? View.VISIBLE : View.GONE);

        if (listaRecetas != null && !listaRecetas.isEmpty()) {
            adapter.updateRecetas(listaRecetas, fechaElegida, fechaIntervaloPrevio);
        }
    }

    private void showConfirmationDialog(Receta receta) {
        dismissCurrentDialog();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.Confirmacion))
                .setMessage(getString(R.string.quieres_anadir_receta) + " " + receta.getNombre() + "?")
                .setPositiveButton(getString(R.string.si), (d, which) -> showNumberTextDialog(receta))
                .setNegativeButton("No", null)
                .create();

        currentDialog = new WeakReference<>(dialog);
        dialog.show();
    }

    private void showNumberTextDialog(Receta receta) {
        dismissCurrentDialog();

        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(getString(R.string.numero_personas));
        editText.setText(String.valueOf(receta.getNumPersonas()));
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.numero_personas)
                .setView(editText)
                .setPositiveButton(R.string.aceptar, (d, which) -> procesarNumeroPersonas(editText, receta))
                .setNegativeButton(R.string.cancelar, null)
                .create();

        currentDialog = new WeakReference<>(dialog);
        dialog.show();
    }

    private void procesarNumeroPersonas(EditText editText, Receta receta) {
        String input = editText.getText().toString();
        if (!input.isEmpty()) {
            try {
                int numPersonas = Integer.parseInt(input);
                if (numPersonas >= 1) {
                    selectedDay.getRecetas().add(new RecetaDia(receta.getId(), numPersonas));

                    CalendarioSrv.actualizarDia(this, selectedDay, new CalendarioSrv.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            mainHandler.post(() -> volverADetalleDiaActivity());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            mainHandler.post(() -> UtilsSrv.notificacion(AddRecetaDiaActivity.this,
                                    getString(R.string.error_actualizar_calendario),
                                    Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    mostrarError(R.string.numero_personas_incorrecto);
                }
            } catch (NumberFormatException e) {
                mostrarError(R.string.numero_personas_incorrecto);
            }
        }
    }

    private void mostrarError(int messageId) {
        UtilsSrv.notificacion(this, getString(messageId), Toast.LENGTH_SHORT).show();
    }

    private void dismissCurrentDialog() {
        if (currentDialog != null) {
            AlertDialog dialog = currentDialog.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            currentDialog.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissCurrentDialog();
        mainHandler.removeCallbacksAndMessages(null);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        volverADetalleDiaActivity();
    }

    private void volverADetalleDiaActivity() {
        Intent intent = new Intent(this, DetalleDiaActivity.class);
        intent.putExtra("selectedDay", selectedDay);
        startActivity(intent);
        finish();
    }
}