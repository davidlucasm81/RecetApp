package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private ActivityResultLauncher<Intent> addRecetaLauncher;


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
            Calendar now = Calendar.getInstance();
            java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache(now.get(Calendar.MONTH), now.get(Calendar.YEAR));
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
            CalendarioSrv.obtenerCalendario(this, now.get(Calendar.MONTH), now.get(Calendar.YEAR), new CalendarioSrv.CalendarioCallback() {
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
                Calendar now = Calendar.getInstance();
                java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache(now.get(Calendar.MONTH), now.get(Calendar.YEAR));
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
                    CalendarioSrv.obtenerCalendario(this, now.get(Calendar.MONTH), now.get(Calendar.YEAR), new CalendarioSrv.CalendarioCallback() {
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Volver al MainActivity simplemente finalizando esta actividad para preservar la pila
                // y evitar recreaciones costosas.
                // Pasar resultado opcional para que MainActivity/CalendarioFragment pueda actualizarse si es necesario.
                Intent result = new Intent();
                if (selectedDay != null) result.putExtra("selectedDayDayOfMonth", selectedDay.getDayOfMonth());
                setResult(RESULT_OK, result);
                finish();
            }
        });

        addRecetaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int dayOfMonth = result.getData().getIntExtra("selectedDayDayOfMonth", -1);
                        if (dayOfMonth > 0 && selectedDay != null) {
                            // Usar el mes y año del día seleccionado, no necesariamente el actual "real"
                            int mes = selectedDay.getMonth();
                            int anio = selectedDay.getYear();

                            java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache(mes, anio);
                            if (cached != null && !cached.isEmpty()) {
                                for (Day d : cached) {
                                    if (d.getDayOfMonth() == dayOfMonth) {
                                        selectedDay = d;
                                        refreshUI();
                                        return;
                                    }
                                }
                            }

                            CalendarioSrv.obtenerCalendario(this, mes, anio, new CalendarioSrv.CalendarioCallback() {
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
        );

        addReceta.setOnClickListener(v -> {
            Intent intent2 = new Intent(this, AddRecetaDiaActivity.class);
            intent2.putExtra("selectedDay", selectedDay);
            // Lanzar esperando resultado para que podamos refrescar cuando vuelva
            addRecetaLauncher.launch(intent2);
        });
    }


    private void refreshUI() {
        if (selectedDay == null) return;
        
        progressBar.setVisibility(View.VISIBLE);

        TextView titleTextView = findViewById(R.id.titleTextView);
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.set(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDayOfMonth());
        
        String nuevoTexto = getString(R.string.detalle_dia) + " "
                + selectedDay.getDayOfMonth() + " "
                + monthYearFormat.format(cal.getTime());
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