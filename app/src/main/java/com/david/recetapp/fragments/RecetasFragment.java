package com.david.recetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.david.recetapp.R;
import com.david.recetapp.actividades.recetas.AddRecetaActivity;
import com.david.recetapp.actividades.ImportExportActivity;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RecetasFragment extends Fragment implements RecetaExpandableListAdapter.EmptyListListener {
    private TextView textViewEmpty;
    private TextView contadorTextView;
    private ExpandableListView expandableListView;
    private AutoCompleteTextView autoCompleteTextViewRecetas;
    private SwitchCompat botonPostres;
    private Handler mainHandler;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private View rootView;
    private FloatingActionButton fab;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recetas, container, false);

        initializeViews();
        setupHandlers();
        setupListeners();
        loadRecetas();

        return rootView;
    }

    private void initializeViews() {
        ImageButton importar = rootView.findViewById(R.id.btnImportar);
        importar.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ImportExportActivity.class);
            importLauncher.launch(intent);
        });

        fab = rootView.findViewById(R.id.fabAddReceta);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddRecetaActivity.class);
            startActivity(intent);
        });

        expandableListView = rootView.findViewById(R.id.expandableListView);
        textViewEmpty = rootView.findViewById(R.id.textViewEmpty);
        autoCompleteTextViewRecetas = rootView.findViewById(R.id.autoCompleteTextViewRecetas);
        botonPostres = rootView.findViewById(R.id.botonPostre);
        contadorTextView = rootView.findViewById(R.id.textViewContadorRecetas);
        progressBar = rootView.findViewById(R.id.progressBar);

        // Adapter vacío inicial para el AutoComplete
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteTextViewRecetas.setAdapter(adapter);
    }

    private void setupHandlers() {
        mainHandler = new Handler(Looper.getMainLooper());
        debounceHandler = new Handler(Looper.getMainLooper());
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("RecetasFragment", "ImportExportActivity resultCode=" + result.getResultCode());
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        Log.d("RecetasFragment", "Import OK -> recargando recetas");
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        textViewEmpty.setVisibility(View.GONE);
                        RecetasSrv.cargarListaRecetas(requireContext(), true, new RecetasSrv.RecetasCallback() {
                            @Override
                            public void onSuccess(List<Receta> recetas) {
                                Log.d("RecetasFragment", "Recetas recargadas tras importación: " + recetas.size());
                                actualizarUIConRecetas(recetas); // tu método para actualizar RecyclerView/ListView
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("RecetasFragment", "Error recargando recetas tras importación", e);
                            }
                        });

                    } else {
                        Log.d("RecetasFragment", "Import cancelled / no changes");
                    }
                }
        );

    }

    private void setupListeners() {
        botonPostres.setOnCheckedChangeListener((buttonView, isChecked) -> filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString()));

        autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                debounceRunnable = () -> filtrarYActualizarLista(s.toString());
                debounceHandler.postDelayed(debounceRunnable, 400);
            }
        });

        rootView.findViewById(R.id.imageViewClearSearch).setOnClickListener(v -> {
            autoCompleteTextViewRecetas.setText("");
            filtrarYActualizarLista("");
        });

        expandableListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mainHandler.post(RecetasFragment.this::actualizarFabSegunScroll);
            }
        });

        expandableListView.setOnGroupExpandListener(groupPosition ->
                mainHandler.post(RecetasFragment.this::actualizarFabSegunScroll));
    }

    private void loadRecetas() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RecetasSrv.cargarListaRecetas(requireContext(), new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                actualizarUIConRecetas(recetas);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    UtilsSrv.notificacion(getContext(),
                            getString(R.string.error_cargar_recetas),
                            Toast.LENGTH_SHORT).show();

                    filtrarYActualizarLista("");
                });
            }
        });
    }

    private void actualizarUIConRecetas(List<Receta> recetas) {
        if (!isAdded()) return;

        // Ordenar por nombre
        recetas.sort((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre()));

        mainHandler.post(() -> {
            if (!isAdded()) return;

            // Rellenar AutoComplete
            Set<String> nombres = recetas.stream()
                    .map(Receta::getNombre)
                    .collect(Collectors.toSet());
            List<String> nombresList = nombres.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
            ArrayAdapter<String> newAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, nombresList);
            autoCompleteTextViewRecetas.setAdapter(newAdapter);

            filtrarYActualizarLista("");

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void filtrarYActualizarLista(String consulta) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        expandableListView.setVisibility(View.GONE);

        final boolean onlyPostres = (botonPostres != null) && botonPostres.isChecked();
        final String query = (consulta == null) ? "" : consulta.trim().toLowerCase(Locale.ROOT);

        // Ejecutar en thread de fondo
        new Thread(() -> {
            List<Receta> copyList = RecetasSrv.getRecetas();

            if (!query.isEmpty()) {
                copyList.removeIf(r -> {
                    boolean nameMatch = r.getNombre() != null &&
                            r.getNombre().toLowerCase(Locale.ROOT).contains(query);
                    boolean ingrMatch = r.getIngredientes() != null &&
                            r.getIngredientes().stream()
                                    .anyMatch(i -> i.getNombre() != null &&
                                            i.getNombre().toLowerCase(Locale.ROOT).contains(query));
                    return !(nameMatch || ingrMatch);
                });
            }

            if (onlyPostres) {
                copyList.removeIf(r -> !r.isPostre());
            }

            copyList.sort(Comparator.comparing(
                    r -> r.getNombre().toLowerCase(Locale.ROOT),
                    String.CASE_INSENSITIVE_ORDER));

            if (!isAdded()) return;

            mainHandler.post(() -> {
                if (!isAdded()) return;

                RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(
                        getContext(), copyList, expandableListView, this);
                expandableListView.setAdapter(expandableListAdapter);

                actualizarVisibilidadListaRecetas(copyList);
                actualizarContador(copyList);
                actualizarFabSegunScroll();

                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                expandableListView.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void actualizarVisibilidadListaRecetas(List<Receta> recetas) {
        if (recetas == null || recetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    private void actualizarFabSegunScroll() {
        if (fab == null || expandableListView == null) return;

        int total = expandableListView.getCount();
        if (total == 0) {
            fab.setAlpha(1f);
            fab.setEnabled(true);
            fab.setClickable(true);
            return;
        }

        int lastVisible = expandableListView.getLastVisiblePosition();
        boolean atBottom = lastVisible >= total - 1;

        if (atBottom) {
            fab.setAlpha(0.35f);
            fab.setEnabled(false);
            fab.setClickable(false);
        } else {
            fab.setAlpha(1f);
            fab.setEnabled(true);
            fab.setClickable(true);
        }
    }

    private void actualizarContador(List<Receta> recetas) {
        int count = (recetas == null) ? 0 : recetas.size();
        actualizarContadorUI(count);
    }

    private void actualizarContadorUI(int count) {
        if (contadorTextView == null) return;
        contadorTextView.setText(String.format(Locale.getDefault(),
                "%d %s", count, (count == 1 ? "receta" : "recetas")));

        textViewEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void reloadList(int count) {
        if (mainHandler != null) {
            mainHandler.post(() -> actualizarContadorUI(count));
        } else {
            actualizarContadorUI(count);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}