package com.david.recetapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarioFragment extends Fragment {

    private TextView monthYearTextView;
    private ExecutorService executor;
    private CalendarioRecyclerAdapter adapter;

    private static final int SPAN_COUNT = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendario, container, false);

        RecyclerView calendarRecyclerView = rootView.findViewById(R.id.calendarRecyclerView);
        monthYearTextView = rootView.findViewById(R.id.monthYearTextView);
        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);

        executor = Executors.newSingleThreadExecutor();

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
                                executor.execute(() -> {
                                    CalendarioSrv.cargarRecetas(requireContext());
                                    loadCalendarDays();
                                }))
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
        executor.execute(() -> {
            final List<Day> days = CalendarioSrv.obtenerCalendario(requireContext());
            final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> adapter.submitDays(days, numeroEnBlanco));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) executor.shutdownNow();
    }
}
