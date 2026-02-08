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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RecetasFragment extends Fragment implements RecetaExpandableListAdapter.EmptyListListener {
    private static final String TAG = "RecetasFragment";

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

    // Executor para tareas del fragment
    private ExecutorService fragmentExecutor;

    // Reutilizar adapters para reducir GC / recreaciones
    private ArrayAdapter<String> autoCompleteAdapter;
    private RecetaExpandableListAdapter expandableListAdapter;

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

        // Adapter reutilizable para AutoComplete
        autoCompleteAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteTextViewRecetas.setAdapter(autoCompleteAdapter);
    }

    private void setupHandlers() {
        mainHandler = new Handler(Looper.getMainLooper());
        debounceHandler = new Handler(Looper.getMainLooper());

        // Executor single thread reutilizable para el fragment
        fragmentExecutor = Executors.newSingleThreadExecutor();

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ImportExportActivity resultCode=" + result.getResultCode());
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        Log.d(TAG, "Import OK -> recargando recetas (forceServer)");
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        if (textViewEmpty != null) textViewEmpty.setVisibility(View.GONE);

                        // usar wrapper seguro
                        safeCargarListaRecetas(true, new RecetasSrv.RecetasCallback() {
                            @Override
                            public void onSuccess(List<Receta> recetas) {
                                Log.d(TAG, "Recetas recargadas tras importación: " + recetas.size());
                                actualizarUIConRecetas(recetas);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Error recargando recetas tras importación", e);
                                mainHandler.post(() -> {
                                    if (!isAdded()) return;
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    UtilsSrv.notificacion(getContext(), getString(R.string.error_cargar_recetas), Toast.LENGTH_SHORT).show();
                                });
                            }
                        });

                    } else {
                        Log.d(TAG, "Import cancelled / no changes");
                    }
                }
        );
    }

    private void setupListeners() {
        botonPostres.setOnCheckedChangeListener((buttonView, isChecked) ->
                filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString()));

        autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
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
                // no-op
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

        // wrapper seguro que maneja SecurityException y problemas con Play Services
        safeCargarListaRecetas(false, new RecetasSrv.RecetasCallback() {
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

        // Ordenar por nombre (sin modificar la original)
        recetas.sort((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre()));

        mainHandler.post(() -> {
            if (!isAdded()) return;

            // Rellenar AutoComplete reutilizando adapter
            Set<String> nombres = recetas.stream()
                    .map(Receta::getNombre)
                    .collect(Collectors.toSet());
            List<String> nombresList = nombres.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());

            autoCompleteAdapter.clear();
            autoCompleteAdapter.addAll(nombresList);
            autoCompleteAdapter.notifyDataSetChanged();

            // Actualizar o crear el expandableListAdapter
            if (expandableListAdapter == null) {
                expandableListAdapter = new RecetaExpandableListAdapter(requireContext(), recetas, expandableListView, this);
                expandableListView.setAdapter(expandableListAdapter);
            } else {
                expandableListAdapter.updateData(recetas);
            }

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
        if (expandableListView != null) expandableListView.setVisibility(View.GONE);

        final boolean onlyPostres = (botonPostres != null) && botonPostres.isChecked();
        final String query = (consulta == null) ? "" : consulta.trim().toLowerCase(Locale.ROOT);

        // Ejecutar en executor (reutilizable)
        fragmentExecutor.execute(() -> {
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
                    r -> r.getNombre() != null ? r.getNombre().toLowerCase(Locale.ROOT) : "",
                    String.CASE_INSENSITIVE_ORDER));

            if (!isAdded()) return;

            mainHandler.post(() -> {
                if (!isAdded()) return;

                if (expandableListAdapter == null) {
                    expandableListAdapter = new RecetaExpandableListAdapter(requireContext(), copyList, expandableListView, RecetasFragment.this);
                    expandableListView.setAdapter(expandableListAdapter);
                } else {
                    expandableListAdapter.updateData(copyList);
                }

                actualizarVisibilidadListaRecetas(copyList);
                actualizarContador(copyList);
                actualizarFabSegunScroll();

                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (expandableListView != null) expandableListView.setVisibility(View.VISIBLE);
            });
        });
    }

    private void actualizarVisibilidadListaRecetas(List<Receta> recetas) {
        if (textViewEmpty == null) return;
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

        if (textViewEmpty != null) textViewEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
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

        // shutdown del executor del fragment para evitar fugas
        if (fragmentExecutor != null && !fragmentExecutor.isShutdown()) {
            fragmentExecutor.shutdownNow();
            fragmentExecutor = null;
        }

        // limpiar referencias a vistas para evitar memory leaks
        rootView = null;
        expandableListView = null;
        autoCompleteTextViewRecetas = null;
        botonPostres = null;
        contadorTextView = null;
        fab = null;
        progressBar = null;
        autoCompleteAdapter = null;
        expandableListAdapter = null;
    }

    // ------------------ HELPERS SEGUROS PARA GOOGLE PLAY SERVICES ------------------

    /**
     * Llama a RecetasSrv.cargarListaRecetas de forma segura.
     * Si Play Services no está disponible o lanza SecurityException, intenta fallback a cache local.
     */
    private void safeCargarListaRecetas(boolean forceServer, RecetasSrv.RecetasCallback callback) {
        try {
            int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
            if (status != ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services no disponible (status=" + status + ") - usando caché si es posible");
                // intentamos cargar sin forzar servidor
                RecetasSrv.cargarListaRecetas(requireContext(), false, callback);
                return;
            }

            // todo bien: invocar normalmente
            RecetasSrv.cargarListaRecetas(requireContext(), forceServer, callback);
        } catch (SecurityException se) {
            // Capturamos SecurityException que provenga de Play Services binder
            Log.e(TAG, "SecurityException al acceder a Google Play Services", se);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                UtilsSrv.notificacion(getContext(), "Error: permisos de Play Services. Se usará caché local.", Toast.LENGTH_LONG).show();
            });

            try {
                // fallback: cargar desde cache/local sin forzar servidor
                RecetasSrv.cargarListaRecetas(requireContext(), false, callback);
            } catch (Exception ex) {
                Log.e(TAG, "Error fallback al cargar recetas tras SecurityException", ex);
                if (callback != null) callback.onFailure(ex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado al cargar recetas", e);
            if (callback != null) callback.onFailure(e);
        }
    }

    private void safeCargarListaRecetas(RecetasSrv.RecetasCallback callback) {
        safeCargarListaRecetas(false, callback);
    }

}