package com.david.recetapp.actividades;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;

import java.util.List;

public class DetalleDiaActivity extends AppCompatActivity implements RecetaExpandableListAdapter.EmptyListListener {

    private TextView textViewEmpty;
    private ExpandableListView expandableListView;

    private Button addReceta;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_dia);

        expandableListView = findViewById(R.id.expandableListView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        addReceta = findViewById(R.id.addReceta);
        textViewEmpty.setVisibility(View.GONE); // Oculta el TextView
        Day selectedDay = (Day) getIntent().getSerializableExtra("selectedDay");
        List<Receta> listaRecetas = selectedDay.getRecetas();
        if(listaRecetas.size()<2){
            addReceta.setVisibility(View.VISIBLE); // Se muestra el boton de añadir receta
        }
        if(listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Se muestra el TextView
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
        addReceta.setVisibility(View.VISIBLE); // Se muestra el boton de añadir receta
    }
}
