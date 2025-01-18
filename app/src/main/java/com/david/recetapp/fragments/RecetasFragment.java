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
import com.david.recetapp.actividades.AddRecetaActivity;
import com.david.recetapp.actividades.ImportExportActivity;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecetasFragment extends Fragment implements RecetaExpandableListAdapter.EmptyListListener {
    private TextView textViewEmpty;
    private ExpandableListView expandableListView;
    private List<Receta> listaRecetas;
    private AutoCompleteTextView autoCompleteTextViewRecetas;
    private SwitchCompat botonPostres;
    private Handler handler;
    private Runnable runnable;
    private View rootView; // Variable para guardar la vista inflada

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recetas, container, false);

        // Configuración de la interfaz
        ImageButton importar = rootView.findViewById(R.id.btnImportar);
        importar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ImportExportActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fab = rootView.findViewById(R.id.fabAddReceta);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddRecetaActivity.class);
            startActivity(intent);
        });

        expandableListView = rootView.findViewById(R.id.expandableListView);
        ImageView imageViewClearSearch = rootView.findViewById(R.id.imageViewClearSearch);
        textViewEmpty = rootView.findViewById(R.id.textViewEmpty);

        // Inicializamos la lista de recetas vacía
        listaRecetas = new ArrayList<>();
        autoCompleteTextViewRecetas = rootView.findViewById(R.id.autoCompleteTextViewRecetas);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteTextViewRecetas.setAdapter(adapter);

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Cargamos la lista de recetas en un hilo en segundo plano
        new Thread(() -> {
            // Cargar las recetas de manera costosa en un hilo en segundo plano
            List<Receta> recetas = RecetasSrv.cargarListaRecetas(requireContext()).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());

            // Al finalizar la carga de recetas, actualizamos la interfaz en el hilo principal
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listaRecetas = recetas;

                    // Actualizamos el adapter del AutoCompleteTextView
                    ArrayAdapter<String> newAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(recetas.stream().map(Receta::getNombre).collect(Collectors.toSet())));
                    autoCompleteTextViewRecetas.setAdapter(newAdapter);

                    // Ahora filtramos la lista inicial
                    filtrarYActualizarLista("");

                    // Configurar la búsqueda con el TextWatcher
                    autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (handler != null && runnable != null) {
                                handler.removeCallbacks(runnable);
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
                            runnable = () -> filtrarYActualizarLista(s.toString());
                            handler.postDelayed(runnable, 500);
                        }
                    });

                    // Configurar el botón de borrar la búsqueda
                    imageViewClearSearch.setOnClickListener(v -> {
                        autoCompleteTextViewRecetas.setText("");
                        filtrarYActualizarLista("");
                    });

                    // Configuración del filtro por postres
                    botonPostres = rootView.findViewById(R.id.botonPostre);
                    botonPostres.setOnCheckedChangeListener((v, isChecked) -> filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString()));

                    actualizarVisibilidadListaRecetas();

                });
            }
        }).start(); // Inicia el hilo para cargar las recetas

        return rootView;
    }


    private void filtrarYActualizarLista(String consulta) {
        // Usar rootView para acceder a los elementos
        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        expandableListView.setVisibility(View.GONE);

        new Thread(() -> {
            listaRecetas = RecetasSrv.cargarListaRecetas(requireContext()).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());

            if (!consulta.trim().isEmpty()) {
                listaRecetas.removeIf(r -> !contieneReceta(r, consulta) && !contieneIngredientes(r, consulta));
            }

            if (botonPostres.isChecked()) {
                listaRecetas.removeIf(r -> !r.isPostre());
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {

                    RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(getContext(), listaRecetas, expandableListView, this);
                    expandableListView.setAdapter(expandableListAdapter);
                    actualizarVisibilidadListaRecetas();

                    // Ocultar el indicador de carga
                    progressBar.setVisibility(View.GONE);
                    expandableListView.setVisibility(View.VISIBLE);

                });
            }
        }).start();
    }

    private boolean contieneReceta(Receta receta, String consulta) {
        return receta.getNombre().toLowerCase().contains(consulta.toLowerCase());
    }

    private boolean contieneIngredientes(Receta receta, String consulta) {
        return receta.getIngredientes().stream().anyMatch(ingrediente -> ingrediente.getNombre().toLowerCase().contains(consulta.toLowerCase()));
    }

    private void actualizarVisibilidadListaRecetas() {
        if (listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE);
        requireView().findViewById(R.id.textoPostre).setVisibility(View.GONE);
    }
}
