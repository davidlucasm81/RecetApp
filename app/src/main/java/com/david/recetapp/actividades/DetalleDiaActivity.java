package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListCalendarAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.RecetaDia;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetalleDiaActivity extends AppCompatActivity
        implements RecetaExpandableListCalendarAdapter.EmptyListListener {

    private TextView textViewEmpty;
    private ExpandableListView expandableListView;
    private Button addReceta;

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DetalleDiaActivity.this, MainActivity.class);
        intent.putExtra("FRAGMENT_TO_LOAD", "CalendarioFragment");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_dia);

        expandableListView = findViewById(R.id.expandableListView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        addReceta = findViewById(R.id.addReceta);
        View progressBar = findViewById(R.id.progressBarDetalle);

        // 👉 Mostrar loading mientras se monta el adapter
        progressBar.setVisibility(View.VISIBLE);

        Day selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);

        TextView titleTextView = findViewById(R.id.titleTextView);
        SimpleDateFormat monthYearFormat =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        assert selectedDay != null;
        String nuevoTexto = getString(R.string.detalle_dia) + " "
                + selectedDay.getDayOfMonth() + " "
                + monthYearFormat.format(Calendar.getInstance().getTime());

        titleTextView.setText(nuevoTexto);

        List<RecetaDia> listaRecetas = selectedDay.getRecetas();
        if (listaRecetas == null || listaRecetas.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }

        // ⚠️ IMPORTANTE: NO se pide nada a Firebase aquí
        RecetaExpandableListCalendarAdapter adapter =
                new RecetaExpandableListCalendarAdapter(
                        this,
                        selectedDay,
                        expandableListView,
                        this
                );

        expandableListView.setAdapter(adapter);

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
            return true;
        });

        // Ya está listo → ocultamos loading
        progressBar.setVisibility(View.GONE);

        addReceta.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecetaDiaActivity.class);
            intent.putExtra("selectedDay", selectedDay);
            startActivity(intent);
        });
    }

    @Override
    public void onListEmpty() {
        textViewEmpty.setVisibility(View.VISIBLE);
        addReceta.setVisibility(View.VISIBLE);
    }

    @Override
    public void onListSize() {
        addReceta.setVisibility(View.VISIBLE);
    }
}
