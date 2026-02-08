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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

/**
 * 🚀 AddRecetaDiaActivity OPTIMIZADA Y COMPATIBLE
 * - Funciona con layout original Y optimizado
 * - Detecta automáticamente si tiene SwipeRefreshLayout
 * - Mejor manejo de estados
 */
public class AddRecetaDiaActivity extends AppCompatActivity {

    private Day selectedDay;
    private RecyclerView recyclerView;
    private RecetasAdapter adapter;
    private TextView emptyView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout; // Puede ser null

    private final Calendar calendarComparar = Calendar.getInstance();
    private final Calendar calendarioIntervaloPrevio = Calendar.getInstance();
    private Date fechaElegida;
    private Date fechaIntervaloPrevio;

    private WeakReference<AlertDialog> currentDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isLoading = false;
    private boolean isAddingRecipe = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receta_dia);

        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh(); // Funciona aunque no exista el SwipeRefresh
        initializeDates();
        loadRecetas(false);
    }

    private void initializeViews() {
        selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);
        recyclerView = findViewById(R.id.recyclerViewRecetas);
        emptyView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        // 🚀 Intentar encontrar SwipeRefreshLayout (puede no existir en layout original)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        adapter = new RecetasAdapter(new ArrayList<>(), this::showConfirmationDialog);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 🚀 Configuración de SwipeRefresh (solo si existe en el layout)
     */
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );

            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (!isLoading && !isAddingRecipe) {
                    loadRecetas(true);
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
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

    /**
     * 🚀 Carga optimizada con soporte para pull-to-refresh
     */
    private void loadRecetas(boolean fromSwipeRefresh) {
        if (isLoading) return;

        isLoading = true;

        if (fromSwipeRefresh && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RecetasSrv.cargarListaRecetasCalendario(this, selectedDay.getRecetas(),
                new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(java.util.List<Receta> listaRecetas) {
                        mainHandler.post(() -> {
                            hideLoading();
                            updateUI(listaRecetas);
                            isLoading = false;
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        mainHandler.post(() -> {
                            hideLoading();
                            isLoading = false;

                            UtilsSrv.notificacion(AddRecetaDiaActivity.this,
                                    getString(R.string.error_cargar_recetas),
                                    Toast.LENGTH_SHORT).show();

                            updateUI(new ArrayList<>());
                        });
                    }
                });
    }

    /**
     * 🚀 Oculta todos los indicadores de carga
     */
    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateUI(java.util.List<Receta> listaRecetas) {
        boolean isEmpty = listaRecetas == null || listaRecetas.isEmpty();

        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (!isEmpty) {
            adapter.updateRecetas(listaRecetas, fechaElegida, fechaIntervaloPrevio);
        }
    }

    private void showConfirmationDialog(Receta receta) {
        if (isAddingRecipe) return;

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
        if (isAddingRecipe) return;

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

    /**
     * 🚀 Procesa la adición de receta con indicador de carga
     */
    private void procesarNumeroPersonas(EditText editText, Receta receta) {
        String input = editText.getText().toString();

        if (!input.isEmpty()) {
            try {
                int numPersonas = Integer.parseInt(input);

                if (numPersonas >= 1) {
                    if (isAddingRecipe) return;

                    isAddingRecipe = true;

                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    selectedDay.getRecetas().add(new RecetaDia(receta.getId(), numPersonas));

                    CalendarioSrv.actualizarDia(this, selectedDay, new CalendarioSrv.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            mainHandler.post(() -> {
                                isAddingRecipe = false;
                                hideLoading();
                                volverADetalleDiaActivity();
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            mainHandler.post(() -> {
                                isAddingRecipe = false;
                                hideLoading();

                                // 🚀 Revertir cambio si falló
                                selectedDay.getRecetas().removeIf(rd ->
                                        rd.getIdReceta().equals(receta.getId()));

                                UtilsSrv.notificacion(AddRecetaDiaActivity.this,
                                        getString(R.string.error_actualizar_calendario),
                                        Toast.LENGTH_SHORT).show();
                            });
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

        adapter = null;
        isLoading = false;
        isAddingRecipe = false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (!isAddingRecipe) {
            volverADetalleDiaActivity();
        }
    }

    private void volverADetalleDiaActivity() {
        Intent intent = new Intent(this, DetalleDiaActivity.class);
        intent.putExtra("selectedDay", selectedDay);
        startActivity(intent);
        finish();
    }
}