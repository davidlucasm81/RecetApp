package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private SwitchCompat botonPostres;
    private Handler handler;
    private Runnable runnable;

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
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

        ImageButton importar = findViewById(R.id.btnImportar);
        importar.setOnClickListener(view -> {
            Intent intent = new Intent(VerRecetasActivity.this, ImportExportActivity.class);
            startActivity(intent);
        });

        expandableListView = findViewById(R.id.expandableListView);
        ImageView imageViewClearSearch = findViewById(R.id.imageViewClearSearch);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Cargar todas las recetas al iniciar la actividad
        listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());

        autoCompleteTextViewRecetas = findViewById(R.id.autoCompleteTextViewRecetas);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(listaRecetas.stream().map(Receta::getNombre).collect(Collectors.toSet())));
        autoCompleteTextViewRecetas.setAdapter(adapter);

        autoCompleteTextViewRecetas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable); // Elimina la búsqueda pendiente si el usuario sigue escribiendo
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler = new Handler();
                runnable = () -> {
                    String recetaIngredienteSeleccionado = s.toString();
                    filtrarYActualizarLista(recetaIngredienteSeleccionado);
                };
                handler.postDelayed(runnable, 500); // Espera 500 milisegundos antes de realizar la búsqueda
            }
        });

        imageViewClearSearch.setOnClickListener(v -> {
            autoCompleteTextViewRecetas.setText("");
            filtrarYActualizarLista("");
        });

        botonPostres = findViewById(R.id.botonPostre);
        botonPostres.setOnCheckedChangeListener((v, isChecked) -> filtrarYActualizarLista(autoCompleteTextViewRecetas.getText().toString()));

        // Configurar la visibilidad de los elementos según la lista de recetas
        actualizarVisibilidadListaRecetas();

        // Cargar todas las recetas por defecto al iniciar la actividad
        filtrarYActualizarLista("");
    }

    private void filtrarYActualizarLista(String consulta) {
        // Muestra el indicador de carga
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        expandableListView.setVisibility(View.GONE);

        new Thread(() -> {
            listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().sorted((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())).collect(Collectors.toList());

            if (!consulta.trim().isEmpty()) {
                listaRecetas.removeIf(r -> !contieneReceta(r, consulta) && !contieneIngredientes(r, consulta));
            }

            if (botonPostres.isChecked()) {
                listaRecetas.removeIf(r -> !r.isPostre());
            }

            runOnUiThread(() -> {
                RecetaExpandableListAdapter expandableListAdapter = new RecetaExpandableListAdapter(this, listaRecetas, expandableListView, this);
                expandableListView.setAdapter(expandableListAdapter);
                actualizarVisibilidadListaRecetas(); // Actualizar visibilidad de elementos después de la búsqueda

                // Ocultar el indicador de carga
                progressBar.setVisibility(View.GONE);
                expandableListView.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private boolean contieneReceta(Receta receta, String consulta) {
        return receta.getNombre().toLowerCase().contains(consulta.toLowerCase());
    }

    private boolean contieneIngredientes(Receta receta, String consulta) {
        return receta.getIngredientes().stream().anyMatch(ingrediente -> ingrediente.getNombre().toLowerCase().contains(consulta.toLowerCase()));
    }

    private void actualizarVisibilidadListaRecetas() {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        if (listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            botonPostres.setVisibility(View.GONE);
            findViewById(R.id.textoPostre).setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            botonPostres.setVisibility(View.VISIBLE);
            findViewById(R.id.textoPostre).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE);
        findViewById(R.id.textoPostre).setVisibility(View.GONE);
    }
}