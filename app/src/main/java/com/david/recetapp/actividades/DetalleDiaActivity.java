package com.david.recetapp.actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
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
    private ProgressBar progressBar;
    private Day selectedDay;

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectedDay = intent.getSerializableExtra("selectedDay", Day.class);
        refreshUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_dia);

        expandableListView = findViewById(R.id.expandableListView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        addReceta = findViewById(R.id.addReceta);
        progressBar = findViewById(R.id.progressBarDetalle);

        selectedDay = getIntent().getSerializableExtra("selectedDay", Day.class);

        refreshUI();

        addReceta.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecetaDiaActivity.class);
            intent.putExtra("selectedDay", selectedDay);
            startActivity(intent);
        });
    }

    private void refreshUI() {
        progressBar.setVisibility(View.VISIBLE);

        TextView titleTextView = findViewById(R.id.titleTextView);
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        assert selectedDay != null;
        String nuevoTexto = getString(R.string.detalle_dia) + " "
                + selectedDay.getDayOfMonth() + " "
                + monthYearFormat.format(Calendar.getInstance().getTime());
        titleTextView.setText(nuevoTexto);

        List<RecetaDia> listaRecetas = selectedDay.getRecetas();
        textViewEmpty.setVisibility(
                (listaRecetas == null || listaRecetas.isEmpty()) ? View.VISIBLE : View.GONE
        );

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

        progressBar.setVisibility(View.GONE);
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