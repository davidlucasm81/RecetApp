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
import android.widget.NumberPicker;
import android.widget.Button;
import androidx.core.content.ContextCompat;
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
    private int currentRequestId = 0;

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
                    loadCalendarDays(true); // <- Solo recarga datos, sin tocar recetas
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void setupButtons(View rootView) {
        ImageButton btnBorrar = rootView.findViewById(R.id.btnBorrar);
        if (btnBorrar != null) {
            btnBorrar.setOnClickListener(v -> {
                if (isAdded() && !isLoading) {
                    AlertDialog alert = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.borrar_calendario))
                            .setMessage(getString(R.string.confirmar_borrar_calendario))
                            .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                                isLoading = true;
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
                                else if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

                                CalendarioSrv.borrarYRecrearCalendario(requireContext(), new CalendarioSrv.CalendarioCallback() {
                                    @Override
                                    public void onSuccess(List<Day> days) {
                                        mainHandler.post(() -> {
                                            hideLoading();
                                            isLoading = false;
                                            if (isAdded()) {
                                                final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
                                                adapter.submitDays(days, numeroEnBlanco);
                                                UtilsSrv.notificacion(requireContext(), getString(R.string.calendario_recreado), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        mainHandler.post(() -> {
                                            hideLoading();
                                            isLoading = false;
                                            if (isAdded()) {
                                                UtilsSrv.notificacion(requireContext(), getString(R.string.error_actualizar_calendario), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });
                            })
                            .setNegativeButton(getString(R.string.cancelar), null)
                            .create();

                    alert.setOnShowListener(dialogInterface -> {
                        Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                        Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
                        if (isAdded()) {
                            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                        }
                    });

                    alert.show();
                }
            });
        }

        ImageButton btnActualizar = rootView.findViewById(R.id.btnActualizar);

        if (btnActualizar != null) {
            btnActualizar.setOnClickListener(v -> {
                if (isAdded() && !isLoading) {
                    LayoutInflater inflaterDialog = LayoutInflater.from(getContext());
                    View dialogView = inflaterDialog.inflate(R.layout.dialog_date_range_picker, null);

                    NumberPicker numberPickerInicio = dialogView.findViewById(R.id.numberPickerInicio);
                    NumberPicker numberPickerFin = dialogView.findViewById(R.id.numberPickerFin);

                    // Configurar los NumberPickers (permitir seleccionar un único día)
                    Calendar cal = Calendar.getInstance();
                    int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    numberPickerInicio.setMinValue(1);
                    numberPickerInicio.setMaxValue(maxDay);
                    numberPickerFin.setMinValue(1);
                    numberPickerFin.setMaxValue(maxDay);

                    // Ajustes dinámicos: permitir rango inclusive (inicio <= fin)
                    numberPickerInicio.setOnValueChangedListener((picker, oldVal, newVal) -> {
                        if (newVal > numberPickerFin.getValue()) numberPickerFin.setValue(newVal);
                        numberPickerFin.setMinValue(newVal);
                    });
                    numberPickerFin.setOnValueChangedListener((picker, oldVal, newVal) -> {
                        if (newVal < numberPickerInicio.getValue()) numberPickerInicio.setValue(newVal);
                        numberPickerInicio.setMaxValue(newVal);
                    });

                    AlertDialog alert = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.seleccionar_dias))
                            .setView(dialogView)
                            .setPositiveButton(getString(R.string.aceptar), (dialog, which) ->
                                    showPeopleDialog(numberPickerInicio.getValue(), numberPickerFin.getValue()))
                            .setNegativeButton(getString(R.string.cancelar), null)
                            .create();

                    alert.setOnShowListener(dialogInterface -> {
                        Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                        Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
                        if (isAdded()) {
                            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                        }
                    });

                    alert.show();
                }
            });
        }
    }

    private void setupCalendar() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (monthYearTextView != null) {
            monthYearTextView.setText(monthYearFormat.format(Calendar.getInstance().getTime()));
        }
        // ← Ya no llama a loadCalendarDays aquí
    }

    /**
     * 🚀 Carga optimizada del calendario
     */
    private void loadCalendarDays(boolean showRefreshing) {
        loadCalendarDays(showRefreshing, 0);
    }

    private void loadCalendarDays(boolean showRefreshing, final int attempt) {
        if (attempt == 0 && isLoading) return;

        isLoading = true;
        final int requestId = ++currentRequestId;

        if (showRefreshing && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Obtener días locales actuales para posible merge
        List<Day> localDays = (adapter != null) ? adapter.getCurrentList() : null;

        // Intento rápido: obtener calendario desde caché en memoria para evitar I/O (solo en el primer intento)
        if (attempt == 0) {
            java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache(requireContext());
            if (cached != null && !cached.isEmpty()) {
                // UI inmediata desde caché (solo si no está vacío para evitar el trap de creación)
                hideLoading();
                isLoading = false;
                if (isAdded()) {
                    final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
                    adapter.submitDays(cached, numeroEnBlanco);
                    if (emptyView != null) emptyView.setVisibility(cached.isEmpty() ? View.VISIBLE : View.GONE);
                }
                return;
            }
        }

        // Lógica de Timeout: 3 segundos
        mainHandler.postDelayed(() -> {
            if (requestId == currentRequestId && isLoading && isAdded()) {
                if (attempt == 0) {
                    // Primer timeout: Reintentar una vez
                    UtilsSrv.notificacion(requireContext(), getString(R.string.reintentando_carga), Toast.LENGTH_SHORT).show();
                    loadCalendarDays(showRefreshing, 1);
                } else {
                    // Segundo timeout: Notificar error final (duración larga)
                    hideLoading();
                    isLoading = false;
                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_conexion_lenta), Toast.LENGTH_LONG).show();
                }
            }
        }, 3000);

        CalendarioSrv.obtenerCalendario(requireContext(), localDays, new CalendarioSrv.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (requestId != currentRequestId) return;

                mainHandler.post(() -> {
                    // ✅ Siempre limpiar estado ANTES de comprobar isAdded
                    hideLoading();
                    isLoading = false;

                    if (!isAdded()) return;

                    final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
                    adapter.submitDays(days, numeroEnBlanco);

                    if (emptyView != null) {
                        emptyView.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (requestId != currentRequestId) return;

                mainHandler.post(() -> {
                    // ✅ Siempre limpiar estado ANTES de comprobar isAdded
                    hideLoading();
                    isLoading = false;

                    if (!isAdded()) return;

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_cargar_calendario),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showPeopleDialog(int diaInicio, int diaFin) {
        if (!isAdded()) return;

        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editText.setHint(getString(R.string.numero_personas));
        editText.setText("2"); // Valor por defecto común
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Añadir algo de margen para que no esté pegado a los bordes del diálogo
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        editText.setPadding(padding, padding, padding, padding);

        AlertDialog alert = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.numero_personas))
                .setView(editText)
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                    String input = editText.getText().toString();
                    if (!input.isEmpty()) {
                        try {
                            int numPersonas = Integer.parseInt(input);
                            if (numPersonas >= 1) {
                                rellenarDias(diaInicio, diaFin, numPersonas);
                            } else {
                                UtilsSrv.notificacion(requireContext(), getString(R.string.numero_personas_incorrecto), Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            UtilsSrv.notificacion(requireContext(), getString(R.string.numero_personas_incorrecto), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .create();

        alert.setOnShowListener(dialogInterface -> {
            Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (isAdded()) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            }
        });

        alert.show();
    }

    /**
     * 🚀 Oculta todos los indicadores de carga
     */
    private void rellenarDias(int diaInicio, int diaFin, int numPersonas) {
        if (isLoading) return;
        isLoading = true;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        CalendarioSrv.rellenarRangoDias(requireContext(), diaInicio, diaFin, true, numPersonas, new CalendarioSrv.RellenarCallback() {
            @Override
            public void onSuccess(List<Day> updatedCalendar) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    // Actualizar UI inmediatamente con el calendario modificado
                    hideLoading();
                    isLoading = false;

                    final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
                    if (adapter != null) adapter.submitDays(updatedCalendar, numeroEnBlanco);

                    UtilsSrv.notificacion(requireContext(), getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    hideLoading();
                    isLoading = false;
                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_actualizar_calendario), Toast.LENGTH_SHORT).show();
                    loadCalendarDays(true);
                });
            }
        });
    }

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

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null || isLoading) return;

        // Intent: si venimos de DetalleDiaActivity o AddRecetaDiaActivity, recibimos el Day actualizado
        Day updatedDay = null;
        try {
            updatedDay = (Day) requireActivity().getIntent().getSerializableExtra("selectedDay");
        } catch (Exception ignored) {}

        if (updatedDay != null) {
            // Actualizar solo el día modificado en la lista actual (optimista, evita recarga completa)
            List<Day> current = new java.util.ArrayList<>(adapter.getCurrentList());
            boolean replaced = false;
            for (int i = 0; i < current.size(); i++) {
                Day d = current.get(i);
                if (d != null && d.getDayOfMonth() == updatedDay.getDayOfMonth()) {
                    current.set(i, updatedDay);
                    replaced = true;
                    break;
                }
            }

            if (replaced) {
                // Evitar computar Diff completo: notificar solo el item cambiado para minimizar trabajo en UI
                int adapterPos = -1;
                List<Day> full = adapter.getCurrentList();
                for (int j = 0; j < full.size(); j++) {
                    Day d = full.get(j);
                    if (d != null && d.getDayOfMonth() == updatedDay.getDayOfMonth()) {
                        adapterPos = j;
                        break;
                    }
                }
                if (adapterPos >= 0) {
                    adapter.notifyItemChanged(adapterPos);
                } else {
                    // Fallback: si no se encontró, enviar la lista completa como antes
                    adapter.submitList(current);
                }

                // Limpiar el extra para evitar re-aplicarlo al siguiente onResume
                requireActivity().setIntent(new android.content.Intent());
                return;
            }
        }

        // Si no hay día específico, recargar normalmente
        loadCalendarDays(false);
    }
}