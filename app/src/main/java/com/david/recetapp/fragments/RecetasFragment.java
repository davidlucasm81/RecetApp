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
import androidx.fragment.app.Fragment;

import com.david.recetapp.R;
import com.david.recetapp.actividades.recetas.AddRecetaActivity;
import com.david.recetapp.actividades.ImportExportActivity;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.FiltroRecetas;
import com.google.android.material.chip.Chip;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
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
    private Chip chipBotonOrdenar;
    private Chip chipBotonFiltrar;
    private Handler mainHandler;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private View rootView;
    private FloatingActionButton fab;
    private FloatingActionButton fabIA;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> importLauncher;

    private FiltroRecetas filtroRecetas = new FiltroRecetas();

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

        fabIA = rootView.findViewById(R.id.fabAddRecetaIA);
        fabIA.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.david.recetapp.actividades.recetas.IAInputActivity.class);
            startActivity(intent);
        });

        expandableListView = rootView.findViewById(R.id.expandableListView);
        textViewEmpty = rootView.findViewById(R.id.textViewEmpty);
        autoCompleteTextViewRecetas = rootView.findViewById(R.id.autoCompleteTextViewRecetas);
        chipBotonOrdenar = rootView.findViewById(R.id.chipBotonOrdenar);
        chipBotonFiltrar = rootView.findViewById(R.id.chipBotonFiltrar);
        contadorTextView = rootView.findViewById(R.id.textViewContadorRecetas);
        progressBar = rootView.findViewById(R.id.progressBar);

        // Adapter reutilizable para AutoComplete
        autoCompleteAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteTextViewRecetas.setAdapter(autoCompleteAdapter);

        actualizarBotonesAccionUI();
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
        if (chipBotonOrdenar != null) {
            chipBotonOrdenar.setOnClickListener(v -> abrirDialogoOrden());
        }
        if (chipBotonFiltrar != null) {
            chipBotonFiltrar.setOnClickListener(v -> abrirDialogoFiltros());
        }

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

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
            return true;
        });
    }

    private void abrirDialogoOrden() {
        OrdenRecetaBottomSheetDialog dialog = OrdenRecetaBottomSheetDialog.newInstance(filtroRecetas.getCriterioOrden());
        dialog.setOnOrdenSeleccionadoListener(nuevoCriterio -> {
            filtroRecetas.setCriterioOrden(nuevoCriterio);
            actualizarBotonesAccionUI();
            filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString());
        });
        dialog.show(getChildFragmentManager(), "OrdenRecetaBottomSheetDialog");
    }

    private void abrirDialogoFiltros() {
        FiltrosRecetaBottomSheetDialog dialog = FiltrosRecetaBottomSheetDialog.newInstance(filtroRecetas);
        dialog.setOnFiltrosAplicadosListener(filtroActualizado -> {
            this.filtroRecetas = filtroActualizado;
            actualizarBotonesAccionUI();
            filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString());
        });
        dialog.show(getChildFragmentManager(), "FiltrosRecetaBottomSheetDialog");
    }

    private void actualizarBotonesAccionUI() {
        if (!isAdded()) return;

        // 1. Botón Ordenar
        if (chipBotonOrdenar != null) {
            FiltroRecetas.CriterioOrden co = filtroRecetas.getCriterioOrden();
            String ordenLabel = getString(R.string.ordenar);
            if (co != null) {
                ordenLabel = switch (co) {
                    case NOMBRE_DESC -> getString(R.string.orden_nombre_desc);
                    case ESTRELLAS_DESC -> getString(R.string.orden_estrellas_desc);
                    case ESTRELLAS_ASC -> getString(R.string.orden_estrellas_asc);
                    case SALUD_DESC -> getString(R.string.orden_salud_desc);
                    case SALUD_ASC -> getString(R.string.orden_salud_asc);
                    case TIEMPO_ASC -> getString(R.string.orden_tiempo_asc);
                    case FECHA_DESC -> getString(R.string.orden_fecha_desc);
                    default -> getString(R.string.orden_nombre_asc);
                };
            }
            chipBotonOrdenar.setText(ordenLabel);
            if (co != null && co != FiltroRecetas.CriterioOrden.NOMBRE_ASC) {
                chipBotonOrdenar.setChipBackgroundColorResource(R.color.colorPrimary);
                chipBotonOrdenar.setTextColor(requireContext().getColor(android.R.color.white));
                chipBotonOrdenar.setChipIconTintResource(android.R.color.white);
            } else {
                chipBotonOrdenar.setChipBackgroundColorResource(R.color.colorBackground);
                chipBotonOrdenar.setTextColor(requireContext().getColor(R.color.colorIcono));
                chipBotonOrdenar.setChipIconTintResource(R.color.colorIcono);
            }
        }

        // 2. Botón Filtrar
        if (chipBotonFiltrar != null) {
            int activeCount = filtroRecetas.getActiveFilterCountWithoutOrder();
            if (activeCount > 0) {
                chipBotonFiltrar.setText(String.format(Locale.getDefault(), "%s (%d)", getString(R.string.filtros), activeCount));
                chipBotonFiltrar.setChipBackgroundColorResource(R.color.colorPrimary);
                chipBotonFiltrar.setTextColor(requireContext().getColor(android.R.color.white));
                chipBotonFiltrar.setChipIconTintResource(android.R.color.white);
            } else {
                chipBotonFiltrar.setText(getString(R.string.filtros));
                chipBotonFiltrar.setChipBackgroundColorResource(R.color.colorBackground);
                chipBotonFiltrar.setTextColor(requireContext().getColor(R.color.colorIcono));
                chipBotonFiltrar.setChipIconTintResource(R.color.colorIcono);
            }
        }
    }

    private void loadRecetas() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

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

        mainHandler.post(() -> {
            if (!isAdded()) return;

            Set<String> nombres = recetas.stream()
                    .map(Receta::getNombre)
                    .collect(Collectors.toSet());
            List<String> nombresList = nombres.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());

            autoCompleteAdapter.clear();
            autoCompleteAdapter.addAll(nombresList);
            autoCompleteAdapter.notifyDataSetChanged();

            if (expandableListAdapter == null) {
                ViewGroup anchor = rootView.findViewById(R.id.youtube_anchor_container);
                expandableListAdapter = new RecetaExpandableListAdapter(requireContext(), recetas, expandableListView, anchor, this);
                expandableListAdapter.setOnNavigateToRecipeListener(recetaId -> {
                    // Limpiar filtros y orden
                    autoCompleteTextViewRecetas.setText("");
                    filtroRecetas.reset();
                    actualizarBotonesAccionUI();
                    filtrarYActualizarLista("");
                    
                    mainHandler.postDelayed(() -> {
                        if (expandableListAdapter != null) {
                            for (int i = 0; i < expandableListAdapter.getGroupCount(); i++) {
                                Receta r = (Receta) expandableListAdapter.getGroup(i);
                                if (r.getId().equals(recetaId)) {
                                    expandableListView.collapseGroup(i);
                                    expandableListView.expandGroup(i);
                                    expandableListView.setSelectedGroup(i);
                                    return;
                                }
                            }
                        }
                    }, 600);
                });
                expandableListView.setAdapter(expandableListAdapter);
            } else {
                expandableListAdapter.updateData(recetas);
            }

            filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString());
        });
    }

    private void filtrarYActualizarLista(String consulta) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (expandableListView != null) expandableListView.setVisibility(View.GONE);

        filtroRecetas.setQuery(consulta);

        fragmentExecutor.execute(() -> {
            List<Receta> copyList = RecetasSrv.getRecetas();

            // Filtrar usando el modelo FiltroRecetas
            copyList.removeIf(r -> !filtroRecetas.test(r));

            // Ordenar usando el comparador del modelo FiltroRecetas
            copyList.sort(filtroRecetas.getComparator());

            if (!isAdded()) return;

            mainHandler.post(() -> {
                if (!isAdded()) return;

                if (expandableListAdapter == null) {
                    ViewGroup anchor = rootView.findViewById(R.id.youtube_anchor_container);
                    expandableListAdapter = new RecetaExpandableListAdapter(requireContext(), copyList, expandableListView, anchor, RecetasFragment.this);
                    expandableListView.setAdapter(expandableListAdapter);
                } else {
                    expandableListAdapter.updateData(copyList);
                }

                actualizarVisibilidadListaRecetas(copyList);
                actualizarContador(copyList);
                actualizarFabSegunScroll();
                actualizarBotonesAccionUI();

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
        if (fab == null || fabIA == null || expandableListView == null) return;

        int total = expandableListView.getCount();
        if (total == 0) {
            fab.setAlpha(1f);
            fab.setEnabled(true);
            fab.setClickable(true);
            fabIA.setAlpha(1f);
            fabIA.setEnabled(true);
            fabIA.setClickable(true);
            return;
        }

        int firstVisible = expandableListView.getFirstVisiblePosition();
        int lastVisible = expandableListView.getLastVisiblePosition();
        boolean atBottom = lastVisible >= total - 1;
        boolean atTop = firstVisible == 0;

        if (atBottom && !atTop) {
            fab.setAlpha(0.35f);
            fab.setEnabled(false);
            fab.setClickable(false);
            fabIA.setAlpha(0.35f);
            fabIA.setEnabled(false);
            fabIA.setClickable(false);
        } else {
            fab.setAlpha(1f);
            fab.setEnabled(true);
            fab.setClickable(true);
            fabIA.setAlpha(1f);
            fabIA.setEnabled(true);
            fabIA.setClickable(true);
        }
    }

    private void actualizarContador(List<Receta> recetas) {
        int count = (recetas == null) ? 0 : recetas.size();
        actualizarContadorUI(count);
    }

    private void actualizarContadorUI(int count) {
        if (contadorTextView == null) return;
        contadorTextView.setText(String.format(Locale.getDefault(),
                "%d %s", count, (count == 1 ? getString(R.string.receta_singular) : getString(R.string.recetas_plural))));

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
        if (expandableListAdapter != null) {
            expandableListAdapter.release();
        }
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        if (fragmentExecutor != null && !fragmentExecutor.isShutdown()) {
            fragmentExecutor.shutdownNow();
            fragmentExecutor = null;
        }

        rootView = null;
        expandableListView = null;
        autoCompleteTextViewRecetas = null;
        chipBotonOrdenar = null;
        chipBotonFiltrar = null;
        contadorTextView = null;
        fab = null;
        fabIA = null;
        progressBar = null;
        autoCompleteAdapter = null;
        expandableListAdapter = null;
    }

    // ------------------ HELPERS SEGUROS PARA GOOGLE PLAY SERVICES ------------------

    private void safeCargarListaRecetas(boolean forceServer, RecetasSrv.RecetasCallback callback) {
        try {
            int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
            if (status != ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services no disponible (status=" + status + ") - usando caché si es posible");
                RecetasSrv.cargarListaRecetas(requireContext(), false, callback);
                return;
            }

            RecetasSrv.cargarListaRecetas(requireContext(), forceServer, callback);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException al acceder a Google Play Services", se);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                UtilsSrv.notificacion(getContext(), getString(R.string.error_play_services), Toast.LENGTH_LONG).show();
            });

            try {
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
}
