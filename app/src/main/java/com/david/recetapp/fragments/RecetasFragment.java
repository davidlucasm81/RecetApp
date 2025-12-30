// Java
package com.david.recetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private TextView textViewEmpty;
    private ExpandableListView expandableListView;
    private List<Receta> listaRecetas = new ArrayList<>();     // lista filtrada que se muestra
    private final List<Receta> allRecetas = new ArrayList<>();       // cache: todas las recetas cargadas
    private AutoCompleteTextView autoCompleteTextViewRecetas;
    private SwitchCompat botonPostres;
    private Handler mainHandler;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private View rootView;
    private ExecutorService executor;
    private FloatingActionButton fab; // ahora campo para poder modificar su estado

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recetas, container, false);

        // Inicializaciones UI básicas
        ImageButton importar = rootView.findViewById(R.id.btnImportar);
        importar.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ImportExportActivity.class);
            startActivity(intent);
        });

        fab = rootView.findViewById(R.id.fabAddReceta);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddRecetaActivity.class);
            startActivity(intent);
        });

        expandableListView = rootView.findViewById(R.id.expandableListView);
        ImageView imageViewClearSearch = rootView.findViewById(R.id.imageViewClearSearch);
        textViewEmpty = rootView.findViewById(R.id.textViewEmpty);
        autoCompleteTextViewRecetas = rootView.findViewById(R.id.autoCompleteTextViewRecetas);

        // Inicializamos el Switch **antes** de cualquier filtrado para evitar NPE
        botonPostres = rootView.findViewById(R.id.botonPostre);

        // Handler y executor
        mainHandler = new Handler(Looper.getMainLooper());
        debounceHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();

        // Adapter vacío inicial para el AutoComplete
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteTextViewRecetas.setAdapter(adapter);

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Listener del Switch: llama a filtrar leyendo el estado desde el hilo UI
        botonPostres.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // llamamos a filtrar pasándole la consulta actual
            filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString());
        });

        // TextWatcher con debounce en el MainLooper
        autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                debounceRunnable = () -> filtrarYActualizarLista(s.toString());
                debounceHandler.postDelayed(debounceRunnable, 400); // 400ms debounce
            }
        });

        imageViewClearSearch.setOnClickListener(v -> {
            autoCompleteTextViewRecetas.setText("");
            filtrarYActualizarLista("");
        });

        // Detectar scroll para esconder/mostrar el FAB cuando estemos al final
        expandableListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mainHandler.post(RecetasFragment.this::actualizarFabSegunScroll);
            }
        });

        // Al expandir un grupo también recalculamos (por si aparecen más items)
        expandableListView.setOnGroupExpandListener(groupPosition -> mainHandler.post(RecetasFragment.this::actualizarFabSegunScroll));

        // Carga inicial de recetas en background (una única vez)
        executor.submit(() -> {
            List<Receta> recetas = RecetasSrv.cargarListaRecetas(requireContext());
            // ordenar por nombre (case insensitive)
            recetas.sort((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre()));

            // guardar en cache
            synchronized (allRecetas) {
                allRecetas.clear();
                allRecetas.addAll(recetas);
            }

            if (!isAdded()) {
                // el fragment puede haber sido destruido mientras cargábamos
                return;
            }

            // actualizar UI con el listado y el AutoCompleteAdapter
            mainHandler.post(() -> {
                if (!isAdded()) return;

                // rellenar AutoComplete con nombres únicos, ordenados
                Set<String> nombres = recetas.stream().map(Receta::getNombre).collect(Collectors.toSet());
                List<String> nombresList = nombres.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresList);
                autoCompleteTextViewRecetas.setAdapter(newAdapter);

                // Hacemos el primer filtrado con la consulta vacía
                filtrarYActualizarLista("");
                progressBar.setVisibility(View.GONE);
            });
        });

        return rootView;
    }

    /**
     * Filtra la lista usando la cache allRecetas y actualiza la UI.
     * Llama a la carga/filtrado en background mediante executor.
     * IMPORTANTE: lee el estado del Switch en el hilo UI y lo pasa al background para evitar NPE.
     */
    private void filtrarYActualizarLista(String consulta) {
        // Mostrar progress en UI
        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        expandableListView.setVisibility(View.GONE);

        // Leer estado del Switch en UI thread (defensa contra null)
        final boolean onlyPostres = (botonPostres != null) && botonPostres.isChecked();
        final String query = (consulta == null) ? "" : consulta.trim().toLowerCase(Locale.ROOT);

        // Submit a background task al executor (reutilizable)
        executor.submit(() -> {
            List<Receta> copyList;
            synchronized (allRecetas) {
                copyList = new ArrayList<>(allRecetas); // trabajar sobre copia
            }

            if (!query.isEmpty()) {
                copyList.removeIf(r -> {
                    boolean nameMatch = r.getNombre() != null && r.getNombre().toLowerCase(Locale.ROOT).contains(query);
                    boolean ingrMatch = r.getIngredientes() != null && r.getIngredientes().stream()
                            .anyMatch(i -> i.getNombre() != null && i.getNombre().toLowerCase(Locale.ROOT).contains(query));
                    return !(nameMatch || ingrMatch);
                });
            }

            if (onlyPostres) {
                copyList.removeIf(r -> !r.isPostre());
            }

            // Ordenamos la lista resultante por nombre (asegura orden después del filtrado)
            copyList.sort(Comparator.comparing(r -> r.getNombre().toLowerCase(Locale.ROOT), String.CASE_INSENSITIVE_ORDER));

            // Ahora actualizamos la UI en el hilo principal
            if (!isAdded()) {
                return; // fragment ya no está -> no actualizar
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;

                listaRecetas = copyList;
                RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(getContext(), listaRecetas, expandableListView, this);
                expandableListView.setAdapter(expandableListAdapter);

                actualizarVisibilidadListaRecetas();

                // Recalcular visibilidad del FAB tras actualizar el adaptador
                actualizarFabSegunScroll();

                progressBar.setVisibility(View.GONE);
                expandableListView.setVisibility(View.VISIBLE);
            });
        });
    }

    private void actualizarVisibilidadListaRecetas() {
        if (listaRecetas == null || listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * Ajusta el FAB: si la lista está al final lo deshabilita y baja la opacidad,
     * en caso contrario lo restaura.
     */
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
            fab.setAlpha(0.35f); // semi-transparente
            fab.setEnabled(false);
            fab.setClickable(false);
        } else {
            fab.setAlpha(1f);
            fab.setEnabled(true);
            fab.setClickable(true);
        }
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE);
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.textoPostre).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // cancelar debounce callbacks
        if (debounceHandler != null && debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        // shutdown executor para evitar leaks si el fragment se destruye
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
