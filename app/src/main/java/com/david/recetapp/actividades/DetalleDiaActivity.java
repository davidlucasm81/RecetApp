package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListCalendarAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetalleDiaActivity extends AppCompatActivity implements RecetaExpandableListCalendarAdapter.EmptyListListener {

    private TextView textViewEmpty;
    private ExpandableListView expandableListView;

    private Button addReceta;

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(DetalleDiaActivity.this, CalendarioActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_dia);

        expandableListView = findViewById(R.id.expandableListView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        addReceta = findViewById(R.id.addReceta);
        textViewEmpty.setVisibility(View.GONE); // Oculta el TextView
        Day selectedDay = (Day) getIntent().getSerializableExtra("selectedDay");

        TextView titleTextView = findViewById(R.id.titleTextView);
        String textoActual = titleTextView.getText().toString();
        // Set the month and year in the TextView
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String nuevoTexto = textoActual + " "+selectedDay.getDayOfMonth()+" "+monthYearFormat.format(Calendar.getInstance().getTime());
        titleTextView.setText(nuevoTexto);

        List<String> listaRecetas = selectedDay.getRecetas();
        if(listaRecetas.size()<2){
            addReceta.setVisibility(View.VISIBLE); // Se muestra el boton de añadir receta
        }
        if(listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE); // Se muestra el TextView
        }
        RecetaExpandableListCalendarAdapter expandableListAdapter = new RecetaExpandableListCalendarAdapter(this, selectedDay, RecetasSrv.obtenerRecetasPorId(this,listaRecetas), expandableListView, this);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
            return true;
        });

        // Manejamos el evento onClick
        addReceta.setOnClickListener(v -> {
            // Empezamos la actividad del detalle dia
            Intent intent = new Intent(this, AddRecetaDiaActivity.class);
            intent.putExtra("selectedDay", selectedDay);
            this.startActivity(intent);
        });
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        addReceta.setVisibility(View.VISIBLE); // Se muestra el boton de añadir receta
    }

    @Override
    public void onListSize() {
        addReceta.setVisibility(View.VISIBLE); // Se muestra el boton de añadir receta
    }
}
