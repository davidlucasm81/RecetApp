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

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetaExpandableListCalendarAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

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
        // Volver al MainActivity simplemente finalizando esta actividad para preservar la pila
        // y evitar recreaciones costosas.
        // Pasar resultado opcional para que MainActivity/CalendarioFragment pueda actualizarse si es necesario.
        Intent result = new Intent();
        if (selectedDay != null) result.putExtra("selectedDayDayOfMonth", selectedDay.getDayOfMonth());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Compatibilidad: puede venir "selectedDay" serializable (viejo comportamiento) o
        // solo el número de día "selectedDayDayOfMonth" (mejor performance).
        Day sd = intent.getSerializableExtra("selectedDay", Day.class);
        if (sd != null) {
            selectedDay = sd;
            refreshUI();
            return;
        }

        int dayOfMonth = intent.getIntExtra("selectedDayDayOfMonth", -1);
        if (dayOfMonth > 0) {
            // Intent: obtener el Day desde la caché para evitar I/O en el hilo principal
            java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache();
            if (cached != null && !cached.isEmpty()) {
                for (Day d : cached) {
                    if (d.getDayOfMonth() == dayOfMonth) {
                        selectedDay = d;
                        refreshUI();
                        return;
                    }
                }
            }

            // Si no está en caché, solicitarlo desde servidor (async)
            CalendarioSrv.obtenerCalendario(this, new CalendarioSrv.CalendarioCallback() {
                @Override
                public void onSuccess(java.util.List<Day> days) {
                    for (Day d : days) {
                        if (d.getDayOfMonth() == dayOfMonth) {
                            selectedDay = d;
                            runOnUiThread(() -> refreshUI());
                            return;
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // mostrar vacío o mantener estado
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_dia);

        expandableListView = findViewById(R.id.expandableListView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        addReceta = findViewById(R.id.addReceta);
        progressBar = findViewById(R.id.progressBarDetalle);

        // Igual que onNewIntent: soportar tanto Serializable como dayOfMonth
        Intent intent = getIntent();
        Day sd = intent.getSerializableExtra("selectedDay", Day.class);
        if (sd != null) {
            selectedDay = sd;
        } else {
            int dayOfMonth = intent.getIntExtra("selectedDayDayOfMonth", -1);
            if (dayOfMonth > 0) {
                java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache();
                if (cached != null && !cached.isEmpty()) {
                    for (Day d : cached) {
                        if (d.getDayOfMonth() == dayOfMonth) {
                            selectedDay = d;
                            break;
                        }
                    }
                }

                if (selectedDay == null) {
                    // pedir al servidor (async)
                    CalendarioSrv.obtenerCalendario(this, new CalendarioSrv.CalendarioCallback() {
                        @Override
                        public void onSuccess(java.util.List<Day> days) {
                            for (Day d : days) {
                                if (d.getDayOfMonth() == dayOfMonth) {
                                    selectedDay = d;
                                    runOnUiThread(() -> refreshUI());
                                    return;
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // no action
                        }
                    });
                }
            }
        }

        refreshUI();

        addReceta.setOnClickListener(v -> {
            Intent intent2 = new Intent(this, AddRecetaDiaActivity.class);
            intent2.putExtra("selectedDay", selectedDay);
            // Lanzar esperando resultado para que podamos refrescar cuando vuelva
            startActivityForResult(intent2, 1001);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            int dayOfMonth = data.getIntExtra("selectedDayDayOfMonth", -1);
            if (dayOfMonth > 0) {
                java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache();
                if (cached != null && !cached.isEmpty()) {
                    for (Day d : cached) {
                        if (d.getDayOfMonth() == dayOfMonth) {
                            selectedDay = d;
                            refreshUI();
                            return;
                        }
                    }
                }

                CalendarioSrv.obtenerCalendario(this, new CalendarioSrv.CalendarioCallback() {
                    @Override
                    public void onSuccess(java.util.List<Day> days) {
                        for (Day d : days) {
                            if (d.getDayOfMonth() == dayOfMonth) {
                                selectedDay = d;
                                runOnUiThread(() -> refreshUI());
                                return;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) { }
                });
            }
        }
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