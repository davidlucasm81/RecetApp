package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VerRecetasActivity extends AppCompatActivity implements RecetaExpandableListAdapter.EmptyListListener {
    private TextView textViewEmpty;
    private ExpandableListView expandableListView;
    private List<Receta> listaRecetas;

    private AutoCompleteTextView autoCompleteTextViewRecetas;

    private ImageView imageViewClearSearch;

    private  SwitchCompat botonPostres;

    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(VerRecetasActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_recetas);

        expandableListView = findViewById(R.id.expandableListView);
        imageViewClearSearch = findViewById(R.id.imageViewClearSearch);

        listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());

        autoCompleteTextViewRecetas = findViewById(R.id.autoCompleteTextViewRecetas);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(listaRecetas.stream().map(Receta::getNombre).collect(Collectors.toSet())));
        autoCompleteTextViewRecetas.setAdapter(adapter);
        // Configurar el OnItemClickListener para el AutoCompleteTextView
        autoCompleteTextViewRecetas.setOnItemClickListener((parent, view, position, id) -> {
            String recetaIngredienteSeleccionado = (String) parent.getItemAtPosition(position);
            filtrarYActualizarLista(recetaIngredienteSeleccionado);
        });

        // Configurar el evento de cambio de texto del AutoCompleteTextView
        autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String recetaIngredienteSeleccionado = s.toString();
                filtrarYActualizarLista(recetaIngredienteSeleccionado);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // Configurar el click listener para el botón de borrar búsqueda
        imageViewClearSearch.setOnClickListener(v -> {
            // Limpiar el texto del AutoCompleteTextView al hacer clic en el botón
            autoCompleteTextViewRecetas.setText("");
            listaRecetas = RecetasSrv.cargarListaRecetas(v.getContext()).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());
            RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(this, listaRecetas, expandableListView, this);
            expandableListView.setAdapter(expandableListAdapter);

            expandableListView.setOnGroupClickListener((parent1, v1, groupPosition1, id1) -> {
                if (expandableListView.isGroupExpanded(groupPosition1)) {
                    expandableListView.collapseGroup(groupPosition1);
                } else {
                    expandableListView.expandGroup(groupPosition1);
                }
                return true;
            });
        });

        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
            autoCompleteTextViewRecetas.setVisibility(View.GONE);
            imageViewClearSearch.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE); // Oculta el TextView si la lista no está vacía
            autoCompleteTextViewRecetas.setVisibility(View.VISIBLE);
            imageViewClearSearch.setVisibility(View.VISIBLE);
        }

        RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(this, listaRecetas, expandableListView, this);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
            return true;
        });
        ImageButton importar = findViewById(R.id.btnImportar);
        importar.setOnClickListener(view -> {
            Intent intent = new Intent(VerRecetasActivity.this, ImportExportActivity.class);
            startActivity(intent);
        });
        botonPostres = findViewById(R.id.botonPostre);
        botonPostres.setOnCheckedChangeListener((v, isChecked) -> {
            filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString());
        });
    }

    // Definir una función para filtrar y actualizar la lista de recetas
    private void filtrarYActualizarLista(String recetaIngredienteSeleccionado) {
        listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());
        if (!recetaIngredienteSeleccionado.trim().isEmpty()) {
            listaRecetas.removeIf(r -> !r.getNombre().equals(recetaIngredienteSeleccionado) && r.getIngredientes().stream().noneMatch(i -> i.getNombre().toLowerCase().contains(recetaIngredienteSeleccionado.toLowerCase())));
        }
        if(botonPostres.isChecked()){
            listaRecetas.removeIf(r -> !r.isPostre());
        }

        RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(this, listaRecetas, expandableListView, this);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener((parent1, v1, groupPosition1, id1) -> {
            if (expandableListView.isGroupExpanded(groupPosition1)) {
                expandableListView.collapseGroup(groupPosition1);
            } else {
                expandableListView.expandGroup(groupPosition1);
            }
            return true;
        });
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        autoCompleteTextViewRecetas.setVisibility(View.GONE);
        imageViewClearSearch.setVisibility(View.GONE);
    }
}