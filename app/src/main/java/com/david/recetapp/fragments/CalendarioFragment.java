package com.david.recetapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.CalendarioAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarioFragment extends Fragment {

    private GridView calendarGridView;
    private TextView monthYearTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendario, container, false);

        calendarGridView = rootView.findViewById(R.id.calendarGridView);
        monthYearTextView = rootView.findViewById(R.id.monthYearTextView);

        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);

        btnActualizar.setOnClickListener(v -> {
            if (isAdded()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(getString(R.string.confirmacion)).setMessage(getString(R.string.alerta_actualizar_calendario)).setPositiveButton(getString(R.string.aceptar), (dialog, which) -> CalendarioSrv.cargarRecetas(requireContext())).setNegativeButton(getString(R.string.cancelar), null).show();
            }
        });

        setupCalendar();
        return rootView;
    }

    private void setupCalendar() {
        if (isAdded()) {
            // Cargar el calendario
            List<Day> days = CalendarioSrv.obtenerCalendario(requireContext());

            // Configurar el mes y año en el TextView
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            monthYearTextView.setText(monthYearFormat.format(Calendar.getInstance().getTime()));

            CalendarioAdapter calendarAdapter = new CalendarioAdapter(requireContext(), days);
            calendarGridView.setAdapter(calendarAdapter);
        }
    }
}
