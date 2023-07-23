package com.david.recetapp.actividades;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.util.List;
import java.util.stream.Collectors;

public class VerRecetasActivity extends AppCompatActivity implements RecetaExpandableListAdapter.EmptyListListener {
    private TextView textViewEmpty;
    private ExpandableListView expandableListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_recetas);

        expandableListView = findViewById(R.id.expandableListView);

        List<Receta> listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().sorted((r1, r2) ->
                String.CASE_INSENSITIVE_ORDER.compare(r1.getNombre(), r2.getNombre())
        ).collect(Collectors.toList());

        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        } else {
            textViewEmpty.setVisibility(View.GONE); // Oculta el TextView si la lista no está vacía
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

    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
    }
}