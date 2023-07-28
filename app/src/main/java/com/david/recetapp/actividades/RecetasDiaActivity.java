package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetasDiaExpandableListAdapter;
import com.david.recetapp.negocio.beans.DiaRecetas;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.util.List;
import java.util.stream.Collectors;

public class RecetasDiaActivity extends AppCompatActivity {
    private ExpandableListView expandableListView;

    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(RecetasDiaActivity.this, CalendarioActivity.class);
        startActivity(intent);
    }

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recetas_dia_activity);

        expandableListView = findViewById(R.id.expandableListView);

        // Obtiene índice del día seleccionado del Intent
        DiaRecetas diaRecetas = (DiaRecetas) getIntent().getSerializableExtra("diaRecetas");
        int dia = (int) getIntent().getSerializableExtra("dia");
        TextView textViewEmpty = findViewById(R.id.textViewEmpty);

        assert diaRecetas != null;
        if (diaRecetas.getRecetas().isEmpty() || diaRecetas.getRecetas().stream().allMatch(s -> s.equals("-1"))) {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        } else {
            textViewEmpty.setVisibility(View.GONE); // Oculta el TextView si la lista no está vacía
        }

        // Obten el TextView del título
        TextView tituloTextView = findViewById(R.id.tituloTextView);

        String diaSemana = UtilsSrv.obtenerDiaSemana(diaRecetas.getFecha());
        tituloTextView.setText(diaSemana);
        List<Receta> listaRecetas = RecetasSrv.cargarListaRecetas(this).stream().filter(r -> diaRecetas.getRecetas().contains(r.getId())).collect(Collectors.toList());
        if (listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        }
        RecetasDiaExpandableListAdapter expandableListAdapter = new RecetasDiaExpandableListAdapter(this, listaRecetas, expandableListView, dia);
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
}