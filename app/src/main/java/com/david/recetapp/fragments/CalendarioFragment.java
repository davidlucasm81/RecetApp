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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.CalendarioRecyclerAdapter;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 🚀 CalendarioFragment OPTIMIZADO Y COMPATIBLE
 * - Funciona con layout original Y optimizado
 * - Detecta automáticamente si tiene SwipeRefreshLayout
 * - Mejor manejo de estados
 */
public class CalendarioFragment extends Fragment {

    private TextView monthYearTextView;
    private CalendarioRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout; // Puede ser null
    private Handler mainHandler;
    private View emptyView; // Puede ser null
    private boolean isLoading = false;

    private static final int SPAN_COUNT = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendario, container, false);

        initializeViews(rootView);
        setupRecyclerView(rootView);
        setupSwipeRefresh(); // Funciona aunque no exista el SwipeRefresh
        setupButtons(rootView);

        mainHandler = new Handler(Looper.getMainLooper());

        setupCalendar();
        return rootView;
    }

    private void initializeViews(View rootView) {
        monthYearTextView = rootView.findViewById(R.id.monthYearTextView);
        progressBar = rootView.findViewById(R.id.progressBar);

        // 🚀 Intentar encontrar SwipeRefreshLayout (puede no existir en layout original)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        // 🚀 Intentar encontrar emptyView (puede no existir en layout original)
        emptyView = rootView.findViewById(R.id.emptyView);
    }

    private void setupRecyclerView(View rootView) {
        RecyclerView calendarRecyclerView = rootView.findViewById(R.id.calendarRecyclerView);

        GridLayoutManager glm = new GridLayoutManager(requireContext(), SPAN_COUNT);
        calendarRecyclerView.setLayoutManager(glm);
        calendarRecyclerView.setHasFixedSize(true);

        adapter = new CalendarioRecyclerAdapter(requireContext());
        calendarRecyclerView.setAdapter(adapter);

        int spacingPx = (int) getResources().getDimension(R.dimen.calendar_spacing);
        calendarRecyclerView.addItemDecoration(new CalendarioRecyclerAdapter.GridSpacingItemDecoration(
                SPAN_COUNT, spacingPx, true));
    }

    /**
     * 🚀 Configuración de SwipeRefresh (solo si existe en el layout)
     */
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );

            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (!isLoading) {
                    actualizarCalendario();
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void setupButtons(View rootView) {
        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);

        if (btnActualizar != null) {
            btnActualizar.setOnClickListener(v -> {
                if (isAdded() && !isLoading) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.confirmacion))
                            .setMessage(getString(R.string.alerta_actualizar_calendario))
                            .setPositiveButton(getString(R.string.aceptar), (dialog, which) ->
                                    actualizarCalendario())
                            .setNegativeButton(getString(R.string.cancelar), null)
                            .show();
                }
            });
        }
    }

    private void setupCalendar() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (monthYearTextView != null) {
            monthYearTextView.setText(monthYearFormat.format(Calendar.getInstance().getTime()));
        }
        loadCalendarDays(false);
    }

    /**
     * 🚀 Carga optimizada del calendario
     */
    private void loadCalendarDays(boolean showRefreshing) {
        if (isLoading) return;

        isLoading = true;

        // Mostrar indicador apropiado
        if (showRefreshing && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
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
                    hideLoading();

                    // Mostrar/ocultar empty view (solo si existe)
                    if (emptyView != null) {
                        boolean isEmpty = days.isEmpty() || days.stream()
                                .allMatch(d -> d.getRecetas() == null || d.getRecetas().isEmpty());
                        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    }

                    isLoading = false;
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hideLoading();
                    isLoading = false;

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_cargar_calendario),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void actualizarCalendario() {
        if (isLoading) return;

        isLoading = true;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        CalendarioSrv.cargarRecetas(requireContext(), new CalendarioSrv.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                loadCalendarDays(true);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    hideLoading();
                    isLoading = false;

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_actualizar_calendario),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 🚀 Oculta todos los indicadores de carga
     */
    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        adapter = null;
        isLoading = false;
    }
}