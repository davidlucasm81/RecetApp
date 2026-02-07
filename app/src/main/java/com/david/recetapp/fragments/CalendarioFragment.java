package com.david.recetapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.CalendarioRecyclerAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarioFragment extends Fragment {

    private TextView monthYearTextView;
    private CalendarioRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private Handler mainHandler;

    private static final int SPAN_COUNT = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendario, container, false);

        RecyclerView calendarRecyclerView = rootView.findViewById(R.id.calendarRecyclerView);
        monthYearTextView = rootView.findViewById(R.id.monthYearTextView);
        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);
        progressBar = rootView.findViewById(R.id.progressBar); // Asegúrate de tener este ProgressBar en tu layout

        mainHandler = new Handler(Looper.getMainLooper());

        GridLayoutManager glm = new GridLayoutManager(requireContext(), SPAN_COUNT);
        calendarRecyclerView.setLayoutManager(glm);
        calendarRecyclerView.setHasFixedSize(true);

        adapter = new CalendarioRecyclerAdapter(requireContext());
        calendarRecyclerView.setAdapter(adapter);

        int spacingPx = (int) getResources().getDimension(R.dimen.calendar_spacing);
        calendarRecyclerView.addItemDecoration(new CalendarioRecyclerAdapter.GridSpacingItemDecoration(
                SPAN_COUNT, spacingPx, true));

        btnActualizar.setOnClickListener(v -> {
            if (isAdded()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.confirmacion))
                        .setMessage(getString(R.string.alerta_actualizar_calendario))
                        .setPositiveButton(getString(R.string.aceptar), (dialog, which) ->
                                actualizarCalendario())
                        .setNegativeButton(getString(R.string.cancelar), null)
                        .show();
            }
        });

        setupCalendar();
        return rootView;
    }

    private void setupCalendar() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthYearTextView.setText(monthYearFormat.format(Calendar.getInstance().getTime()));
        loadCalendarDays();
    }

    private void loadCalendarDays() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        CalendarioSrv.obtenerCalendario(requireContext(), new CalendarioSrv.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (!isAdded()) return;

                final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    adapter.submitDays(days, numeroEnBlanco);

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_cargar_calendario),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void actualizarCalendario() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        CalendarioSrv.cargarRecetas(requireContext(), new CalendarioSrv.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                // Recargar el calendario después de actualizar las recetas
                loadCalendarDays();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_actualizar_calendario),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar callbacks pendientes
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}