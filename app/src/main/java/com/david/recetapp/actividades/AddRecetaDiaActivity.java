package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetasAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private AutoCompleteTextView searchView;
    private ImageView clearSearchButton;

    private List<Receta> fullRecipeList = new ArrayList<>();
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
        setupSearch();
        initializeDates();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isAddingRecipe) {
                    volverADetalleDiaActivity();
                }
            }
        });

        loadRecetas(false);
    }

    private void initializeViews() {
        selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);
        recyclerView = findViewById(R.id.recyclerViewRecetas);
        emptyView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        // 🚀 Intentar encontrar SwipeRefreshLayout (puede no existir en layout original)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        searchView = findViewById(R.id.autoCompleteTextViewRecetas);
        clearSearchButton = findViewById(R.id.imageViewClearSearch);
    }

    private void setupSearch() {
        if (searchView != null) {
            searchView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    filtrarRecetas(s.toString());
                }
            });
        }

        if (clearSearchButton != null) {
            clearSearchButton.setOnClickListener(v -> {
                if (searchView != null) {
                    searchView.setText("");
                }
            });
        }
    }

    private void filtrarRecetas(String query) {
        if (fullRecipeList == null) return;
        
        String cleanQuery = query.toLowerCase().trim();
        List<Receta> filteredList;
        
        if (cleanQuery.isEmpty()) {
            filteredList = new ArrayList<>(fullRecipeList);
        } else {
            filteredList = fullRecipeList.stream()
                    .filter(r -> r.getNombre().toLowerCase().contains(cleanQuery))
                    .collect(Collectors.toList());
        }
        
        updateUI(filteredList);
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

        RecetasSrv.cargarListaRecetasCalendario(this, selectedDay,
                new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(java.util.List<Receta> listaRecetas) {
                        mainHandler.post(() -> {
                            hideLoading();
                            fullRecipeList = listaRecetas != null ? listaRecetas : new ArrayList<>();
                            String currentQuery = searchView != null ? searchView.getText().toString() : "";
                            if (currentQuery.isEmpty()) {
                                updateUI(fullRecipeList);
                            } else {
                                filtrarRecetas(currentQuery);
                            }
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
                .setPositiveButton(getString(R.string.si), (d, which) -> checkSubstitutions(receta))
                .setNegativeButton("No", null)
                .create();

        currentDialog = new WeakReference<>(dialog);
        dialog.show();
    }

    private void checkSubstitutions(Receta receta) {
        // Agrupar ingredientes por su "principal"
        Map<String, List<Ingrediente>> grupos = new HashMap<>();
        for (Ingrediente ing : receta.getIngredientes()) {
            String key = (ing.getEsSustitutoDe() != null && !ing.getEsSustitutoDe().isEmpty())
                    ? ing.getEsSustitutoDe() : ing.getNombre();
            grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(ing);
        }

        // Filtrar solo grupos que tengan más de una opción
        List<Map.Entry<String, List<Ingrediente>>> gruposConSustitutos = grupos.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toList());

        if (gruposConSustitutos.isEmpty()) {
            showNumberTextDialog(receta, new HashMap<>());
        } else {
            showSubstitutionSelectionDialog(receta, gruposConSustitutos, 0, new HashMap<>());
        }
    }

    private void showSubstitutionSelectionDialog(Receta receta, List<Map.Entry<String, List<Ingrediente>>> grupos, int index, Map<String, String> elegidos) {
        if (index >= grupos.size()) {
            showNumberTextDialog(receta, elegidos);
            return;
        }

        Map.Entry<String, List<Ingrediente>> grupo = grupos.get(index);
        String principal = grupo.getKey();
        List<Ingrediente> opciones = grupo.getValue();
        String[] nombresOpciones = opciones.stream().map(Ingrediente::getNombre).toArray(String[]::new);

        // Preseleccionar el mejor por defecto
        int defaultChoice = 0;
        double maxScore = -100;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).getPuntuacion() > maxScore) {
                maxScore = opciones.get(i).getPuntuacion();
                defaultChoice = i;
            }
        }

        final int[] selected = {defaultChoice};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.seleccionar_sustituto) + ": " + principal)
                .setSingleChoiceItems(nombresOpciones, defaultChoice, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.aceptar, (d, which) -> {
                    elegidos.put(principal, nombresOpciones[selected[0]]);
                    showSubstitutionSelectionDialog(receta, grupos, index + 1, elegidos);
                })
                .setNegativeButton(R.string.cancelar, null)
                .create();

        currentDialog = new WeakReference<>(dialog);
        dialog.show();
    }

    private void showNumberTextDialog(Receta receta, Map<String, String> elegidos) {
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
                .setPositiveButton(R.string.aceptar, (d, which) -> procesarNumeroPersonas(editText, receta, elegidos))
                .setNegativeButton(R.string.cancelar, null)
                .create();

        currentDialog = new WeakReference<>(dialog);
        dialog.show();
    }

    /**
     * 🚀 Procesa la adición de receta con indicador de carga
     */
    private void procesarNumeroPersonas(EditText editText, Receta receta, Map<String, String> elegidos) {
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

                    // Añadir localmente y actualizar caché para que la UI vea el cambio inmediatamente
                    selectedDay.getRecetas().add(new RecetaDia(receta.getId(), numPersonas, elegidos));
                    CalendarioSrv.aplicarActualizacionLocalDia(selectedDay.getMonth(), selectedDay.getYear(), selectedDay);

                    // Lanzar sincronización en background sin bloquear la UI.
                    // Usar un batch para actualizar calendario + fecha de receta de una vez.
                    CalendarioSrv.guardarDiaYRecetaBatch(selectedDay.getMonth(), selectedDay.getYear(), selectedDay, receta.getId());

                    // Volver de forma inmediata (UX optimista)
                    mainHandler.post(() -> {
                        isAddingRecipe = false;
                        hideLoading();
                        volverADetalleDiaActivity();
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


    private void volverADetalleDiaActivity() {
        // Devolver resultado a la actividad que lanzó este Activity (DetalleDiaActivity)
        Intent result = new Intent();
        result.putExtra("selectedDayDayOfMonth", selectedDay.getDayOfMonth());
        setResult(RESULT_OK, result);
        // No lanzar nuevas actividades: finish para volver al anterior en la pila
        finish();
    }
}