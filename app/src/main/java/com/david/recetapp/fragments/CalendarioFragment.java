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
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.beans.Receta;

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
    private TextView seasonTextView;
    private CalendarioRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout; // Puede ser null
    private Handler mainHandler;
    private View emptyView; // Puede ser null
    private boolean isLoading = false;
    private int currentRequestId = 0;

    private Calendar calendarViewing;
    private Calendar calendarReal;

    private ImageButton btnBorrar;
    private ImageButton btnActualizar;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;

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

        calendarReal = Calendar.getInstance();
        calendarViewing = (Calendar) calendarReal.clone();

        setupCalendar();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Carga inicial forzada si no está cargando ya
        if (!isLoading) {
            loadCalendarDays(false);
        }
    }

    private void initializeViews(View rootView) {
        monthYearTextView = rootView.findViewById(R.id.monthYearTextView);
        seasonTextView = rootView.findViewById(R.id.seasonTextView);
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
        btnBorrar = rootView.findViewById(R.id.btnBorrar);
        if (btnBorrar != null) {
            btnBorrar.setOnClickListener(v -> {
                if (isAdded() && !isLoading) {
                    if (!isViewingCurrentMonth()) {
                        UtilsSrv.notificacion(requireContext(), getString(R.string.solo_mes_actual), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog alert = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.borrar_calendario))
                            .setMessage(getString(R.string.confirmar_borrar_calendario))
                            .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                                isLoading = true;
                                final int requestId = ++currentRequestId;
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
                                else if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

                                // Timeout de seguridad: 8 segundos (borrado puede ser lento)
                                mainHandler.postDelayed(() -> {
                                    if (requestId == currentRequestId && isLoading) {
                                        isLoading = false;
                                        hideLoading();
                                        if (isAdded()) {
                                            UtilsSrv.notificacion(requireContext(), getString(R.string.error_conexion_lenta), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }, 8000);

                                // Resetear fechaCalendario de todas las recetas a null (Date(0) tratado como null)
                                RecetasSrv.cargarListaRecetas(requireContext(), new RecetasSrv.RecetasCallback() {
                                    @Override
                                    public void onSuccess(List<Receta> recetas) {
                                        for (Receta r : recetas) {
                                            RecetasSrv.actualizarRecetaCalendarioDirect(r, 0, false);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // No bloquear el flujo si falla; se sigue con el borrado del calendario
                                    }
                                });

                                CalendarioSrv.borrarYRecrearCalendario(requireContext(), calendarViewing.get(Calendar.MONTH), calendarViewing.get(Calendar.YEAR), new CalendarioSrv.CalendarioCallback() {
                                    @Override
                                    public void onSuccess(List<Day> days) {
                                        mainHandler.post(() -> {
                                            if (requestId != currentRequestId) return;
                                            isLoading = false;
                                            hideLoading();
                                            if (isAdded()) {
                                                final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1);
                                                adapter.submitDays(days, numeroEnBlanco);
                                                UtilsSrv.notificacion(requireContext(), getString(R.string.calendario_recreado), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        mainHandler.post(() -> {
                                            if (requestId != currentRequestId) return;
                                            isLoading = false;
                                            hideLoading();
                                            if (isAdded()) {
                                                UtilsSrv.notificacion(requireContext(), getString(R.string.error_actualizar_calendario), Toast.LENGTH_LONG).show();
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

        btnActualizar = rootView.findViewById(R.id.btnActualizar);

        if (btnActualizar != null) {
            btnActualizar.setOnClickListener(v -> {
                if (isAdded() && !isLoading) {
                    if (!isViewingCurrentMonth()) {
                        UtilsSrv.notificacion(requireContext(), getString(R.string.solo_mes_actual), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LayoutInflater inflaterDialog = LayoutInflater.from(getContext());
                    View dialogView = inflaterDialog.inflate(R.layout.dialog_calendar_refill, null);

                    NumberPicker numberPickerInicio = dialogView.findViewById(R.id.numberPickerInicio);
                    NumberPicker numberPickerFin = dialogView.findViewById(R.id.numberPickerFin);
                    NumberPicker numberPickerRecetas = dialogView.findViewById(R.id.numberPickerRecetas);

                    // Configurar los NumberPickers (permitir seleccionar un único día)
                    int maxDay = calendarViewing.getActualMaximum(Calendar.DAY_OF_MONTH);
                    numberPickerInicio.setMinValue(1);
                    numberPickerInicio.setMaxValue(maxDay);
                    numberPickerFin.setMinValue(1);
                    numberPickerFin.setMaxValue(maxDay);

                    // Configurar NumberPicker de recetas (ej: de 1 a 5 recetas por día)
                    numberPickerRecetas.setMinValue(1);
                    numberPickerRecetas.setMaxValue(5);
                    numberPickerRecetas.setValue(2); // Valor por defecto anterior

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
                                    showPeopleDialog(numberPickerInicio.getValue(), numberPickerFin.getValue(), numberPickerRecetas.getValue()))
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

        btnPreviousMonth = rootView.findViewById(R.id.btnPreviousMonth);
        if (btnPreviousMonth != null) {
            btnPreviousMonth.setOnClickListener(v -> {
                if (!isLoading) {
                    calendarViewing.add(Calendar.MONTH, -1);
                    updateNavigationUI();
                    loadCalendarDays(false);
                }
            });
        }

        btnNextMonth = rootView.findViewById(R.id.btnNextMonth);
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                if (!isLoading) {
                    calendarViewing.add(Calendar.MONTH, 1);
                    updateNavigationUI();
                    loadCalendarDays(false);
                }
            });
        }
    }

    private void updateNavigationUI() {
        setupCalendar();

        Calendar prevLimit = (Calendar) calendarReal.clone();
        prevLimit.add(Calendar.MONTH, -1);
        Calendar nextLimit = (Calendar) calendarReal.clone();
        nextLimit.add(Calendar.MONTH, 1);

        if (btnPreviousMonth != null) {
            btnPreviousMonth.setVisibility(isSameMonth(calendarViewing, prevLimit) ? View.INVISIBLE : View.VISIBLE);
        }
        if (btnNextMonth != null) {
            btnNextMonth.setVisibility(isSameMonth(calendarViewing, nextLimit) ? View.INVISIBLE : View.VISIBLE);
        }

        updateManagementButtonsVisibility();
    }

    private void updateManagementButtonsVisibility() {
        boolean isCurrent = isViewingCurrentMonth();
        if (btnBorrar != null) btnBorrar.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
        if (btnActualizar != null) btnActualizar.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
    }

    private boolean isViewingCurrentMonth() {
        return isSameMonth(calendarViewing, calendarReal);
    }

    private boolean isSameMonth(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    private void setupCalendar() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (monthYearTextView != null) {
            monthYearTextView.setText(monthYearFormat.format(calendarViewing.getTime()));
        }

        if (seasonTextView != null) {
            int mes = calendarViewing.get(Calendar.MONTH);
            int anio = calendarViewing.get(Calendar.YEAR);
            java.time.LocalDate date = java.time.LocalDate.of(anio, mes + 1, 1);
            com.david.recetapp.negocio.beans.Temporada temporada = UtilsSrv.getTemporadaFecha(date);

            seasonTextView.setText(getString(temporada.getStringRes()));

            // Color basado en la temporada
            int colorRes = switch (temporada) {
                case VERANO -> R.color.colorVerano;
                case PRIMAVERA -> R.color.colorPrimavera;
                case OTONIO -> R.color.colorOtonio;
                case INVIERNO -> R.color.colorInvierno;
            };
            seasonTextView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), colorRes)));
        }
    }

    /**
     * 🚀 Carga optimizada del calendario
     */
    private void loadCalendarDays(boolean showRefreshing) {
        if (isLoading) return;

        isLoading = true;
        final int requestId = ++currentRequestId;

        // Limpiar el calendario actual para que no se vea mientras carga el nuevo
        if (adapter != null) {
            adapter.submitList(null);
        }

        if (showRefreshing && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Obtener días locales actuales para posible merge
        List<Day> localDays = (adapter != null) ? adapter.getCurrentList() : null;

        // Intento rápido: obtener calendario desde caché en memoria para evitar I/O
        int mes = calendarViewing.get(Calendar.MONTH);
        int anio = calendarViewing.get(Calendar.YEAR);
        java.util.List<Day> cached = CalendarioSrv.obtenerCalendarioCache(mes, anio);
        if (cached != null && !cached.isEmpty()) {
            isLoading = false;
            hideLoading();
            if (isAdded()) {
                final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1, mes, anio);
                adapter.submitDays(cached, numeroEnBlanco);
                if (emptyView != null) emptyView.setVisibility(cached.isEmpty() ? View.VISIBLE : View.GONE);
            }
            return;
        }

        // Lógica de Notificación por Conexión Lenta: 6 segundos
        mainHandler.postDelayed(() -> {
            if (requestId == currentRequestId && isLoading) {
                if (isAdded()) {
                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_conexion_lenta), Toast.LENGTH_LONG).show();
                }
            }
        }, 6000);

        CalendarioSrv.obtenerCalendario(requireContext(), mes, anio, localDays, new CalendarioSrv.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                mainHandler.post(() -> {
                    if (requestId != currentRequestId) return;
                    isLoading = false;
                    hideLoading();

                    if (!isAdded()) return;

                    final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1, mes, anio);
                    adapter.submitDays(days, numeroEnBlanco);

                    if (emptyView != null) {
                        emptyView.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    if (requestId != currentRequestId) return;
                    isLoading = false;
                    hideLoading();

                    if (!isAdded()) return;

                    UtilsSrv.notificacion(requireContext(),
                            getString(R.string.error_cargar_calendario),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showPeopleDialog(int diaInicio, int diaFin, int numRecetas) {
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
                                rellenarDias(diaInicio, diaFin, numRecetas, numPersonas);
                            } else {
                                UtilsSrv.notificacion(requireContext(), getString(R.string.numero_personas_incorrecto), Toast.LENGTH_LONG).show();
                            }
                        } catch (NumberFormatException e) {
                            UtilsSrv.notificacion(requireContext(), getString(R.string.numero_personas_incorrecto), Toast.LENGTH_LONG).show();
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
    private void rellenarDias(int diaInicio, int diaFin, int numRecetas, int numPersonas) {
        if (isLoading) return;
        isLoading = true;
        final int requestId = ++currentRequestId;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Timeout de seguridad: 8 segundos
        mainHandler.postDelayed(() -> {
            if (requestId == currentRequestId && isLoading) {
                isLoading = false;
                hideLoading();
                if (isAdded()) {
                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_conexion_lenta), Toast.LENGTH_LONG).show();
                }
            }
        }, 8000);

        int mes = calendarViewing.get(Calendar.MONTH);
        int anio = calendarViewing.get(Calendar.YEAR);

        CalendarioSrv.rellenarRangoDias(requireContext(), mes, anio, diaInicio, diaFin, true, numRecetas, numPersonas, new CalendarioSrv.RellenarCallback() {
            @Override
            public void onSuccess(List<Day> updatedCalendar) {
                mainHandler.post(() -> {
                    if (requestId != currentRequestId) return;
                    isLoading = false;
                    hideLoading();

                    if (!isAdded()) return;

                    final int numeroEnBlanco = UtilsSrv.obtenerColumnaCalendario(1, mes, anio);
                    if (adapter != null) adapter.submitDays(updatedCalendar, numeroEnBlanco);

                    UtilsSrv.notificacion(requireContext(), getString(R.string.calendario_actualizado), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    if (requestId != currentRequestId) return;
                    isLoading = false;
                    hideLoading();

                    if (!isAdded()) return;

                    UtilsSrv.notificacion(requireContext(), getString(R.string.error_actualizar_calendario), Toast.LENGTH_LONG).show();
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
        int updatedDayOfMonth = -1;
        try {
            android.content.Intent intent = requireActivity().getIntent();
            updatedDayOfMonth = intent.getIntExtra("selectedDayDayOfMonth", -1);
        } catch (Exception ignored) {}

        if (updatedDayOfMonth != -1) {
            // Recargar el día específico desde el servidor o caché para asegurarnos de tener los datos frescos
            CalendarioSrv.obtenerCalendario(requireContext(), calendarViewing.get(Calendar.MONTH), calendarViewing.get(Calendar.YEAR), new CalendarioSrv.CalendarioCallback() {
                @Override
                public void onSuccess(List<Day> days) {
                    mainHandler.post(() -> {
                        if (!isAdded() || adapter == null) return;
                        
                        // Actualizar la lista completa del adapter (DiffUtil se encargará del resto)
                        int mes = calendarViewing.get(Calendar.MONTH);
                        int anio = calendarViewing.get(Calendar.YEAR);
                        adapter.submitDays(days, UtilsSrv.obtenerColumnaCalendario(1, mes, anio));
                        
                        // Limpiar el extra
                        requireActivity().setIntent(new android.content.Intent());
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    // Fallback: recarga completa
                    loadCalendarDays(false);
                }
            });
            return;
        }

        // Si no hay día específico, recargar normalmente
        loadCalendarDays(false);
    }
}