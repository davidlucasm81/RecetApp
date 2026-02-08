package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 🚀 Servicio de Calendario OPTIMIZADO
 * - Elimina llamadas redundantes a Firebase
 * - Reduce operaciones N+1
 * - Usa caché de manera eficiente
 */
public class CalendarioSrv {
    private static final String TAG = "CalendarioSrv";
    private static final int LIMITE_DIAS = 3;
    private static final FirebaseManager firebaseManager = new FirebaseManager();

    public interface CalendarioCallback {
        void onSuccess(List<Day> days);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface ListaCompraCallback {
        void onSuccess(String listaCompra);
        void onFailure(Exception e);
    }

    // ==================== OBTENER CALENDARIO ====================

    public static void obtenerCalendario(Context context, CalendarioCallback callback) {
        firebaseManager.obtenerCalendario(new FirebaseManager.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (days.isEmpty()) {
                    List<Day> newDays = generateDays();
                    guardarCalendario(context, newDays, new SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess(newDays);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error guardando calendario nuevo", e);
                            callback.onFailure(e);
                        }
                    });
                } else {
                    callback.onSuccess(days);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo calendario", e);
                List<Day> days = generateDays();
                callback.onSuccess(days);
            }
        });
    }

    private static List<Day> generateDays() {
        List<Day> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.set(currentYear, currentMonth, 1);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new Day(i, new ArrayList<>()));
        }

        return days;
    }

    private static void guardarCalendario(Context context, List<Day> days, SimpleCallback callback) {
        firebaseManager.guardarCalendario(days, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_actualizado),
                        Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando calendario", e);
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_no_actualizado),
                        Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    // ==================== ACTUALIZAR DÍA ====================

    public static void actualizarDia(Activity activity, Day selectedDay, SimpleCallback callback) {
        obtenerCalendario(activity, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> dias) {
                Map<Integer, Day> diasMap = dias.stream()
                        .collect(Collectors.toMap(Day::getDayOfMonth, day -> day));

                Day dia = diasMap.get(selectedDay.getDayOfMonth());

                if (dia != null) {
                    dia.setRecetas(selectedDay.getRecetas());

                    firebaseManager.actualizarDia(dia, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            // 🚀 OPTIMIZACIÓN: Cargar recetas UNA SOLA VEZ
                            RecetasSrv.cargarListaRecetas(activity, new RecetasSrv.RecetasCallback() {
                                @Override
                                public void onSuccess(List<Receta> listaRecetas) {
                                    // Actualizar fechas solo de las recetas del día
                                    Set<String> idsRecetasDia = dia.getRecetas().stream()
                                            .map(RecetaDia::getIdReceta)
                                            .collect(Collectors.toSet());

                                    listaRecetas.stream()
                                            .filter(r -> idsRecetasDia.contains(r.getId()))
                                            .forEach(r -> RecetasSrv.actualizarRecetaCalendario(
                                                    activity, r.getId(), dia.getDayOfMonth(), true));

                                    if (callback != null) callback.onSuccess();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Error cargando recetas para actualizar", e);
                                    if (callback != null) callback.onFailure(e);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error actualizando día", e);
                            UtilsSrv.notificacion(activity,
                                    activity.getString(R.string.calendario_no_actualizado),
                                    Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onFailure(e);
                        }
                    });
                } else {
                    UtilsSrv.notificacion(activity,
                            activity.getString(R.string.calendario_no_actualizado),
                            Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onFailure(new Exception("Día no encontrado"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                UtilsSrv.notificacion(activity,
                        activity.getString(R.string.calendario_no_actualizado),
                        Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    // ==================== ACTUALIZAR FECHA CALENDARIO ====================

    public static void actualizarFechaCalendario(Activity activity, String idReceta) {
        obtenerCalendario(activity, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> dias) {
                Optional<Day> dia = dias.stream()
                        .sorted((d1, d2) -> d2.getDayOfMonth() - d1.getDayOfMonth())
                        .filter(d -> d.getRecetas().stream()
                                .map(RecetaDia::getIdReceta)
                                .anyMatch(dr -> dr.equals(idReceta)))
                        .findFirst();

                if (dia.isPresent()) {
                    RecetasSrv.actualizarRecetaCalendario(activity, idReceta,
                            dia.get().getDayOfMonth(), false);
                } else {
                    RecetasSrv.actualizarRecetaCalendario(activity, idReceta, -1, false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error actualizando fecha calendario", e);
            }
        });
    }

    // ==================== CARGAR RECETAS (PROBLEMA CRÍTICO RESUELTO) ====================

    /**
     * 🚀 VERSIÓN OPTIMIZADA - Eliminado problema N+1
     *
     * ANTES: Cargaba recetas 30+ veces (una por cada día del mes)
     * AHORA: Carga recetas UNA SOLA VEZ y reutiliza
     */
    public static void cargarRecetas(Context context, SimpleCallback callback) {
        obtenerCalendario(context, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> calendar) {
                // 🚀 Cargar recetas UNA SOLA VEZ al inicio
                RecetasSrv.cargarListaRecetasCalendario(context, new ArrayList<>(),
                        new RecetasSrv.RecetasCallback() {
                            @Override
                            public void onSuccess(List<Receta> recetas) {
                                Log.d(TAG, "✅ Recetas cargadas: " + recetas.size());

                                // Procesar calendario con las recetas ya cargadas
                                procesarCalendarioConRecetas(context, calendar, recetas, callback);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Error cargando recetas para calendario", e);
                                if (callback != null) callback.onFailure(e);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo calendario para cargar recetas", e);
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    /**
     * 🚀 Procesa el calendario SIN llamar a Firebase repetidamente
     */
    private static void procesarCalendarioConRecetas(Context context, List<Day> calendar,
                                                     List<Receta> recetasDisponibles,
                                                     SimpleCallback callback) {
        Queue<Receta> cola = new LinkedList<>(recetasDisponibles);
        Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();

        // 🚀 Batch de actualizaciones de fechas (en lugar de una por una)
        List<ActualizacionFecha> actualizacionesPendientes = new ArrayList<>();

        for (Day dia : calendar) {
            dia.setRecetas(new ArrayList<>());

            if (!esDiaAnteriorAlActual(dia)) {
                // Añadir 2 recetas al día
                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes);
                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes);

                // Limpiar recetas antiguas
                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia);
            }
        }

        // 🚀 Actualizar TODAS las fechas en batch (más eficiente)
        actualizarFechasEnBatch(context, actualizacionesPendientes);

        // Guardar calendario una sola vez
        guardarCalendario(context, calendar, callback);
    }

    /**
     * 🚀 Clase auxiliar para batch updates
     */
    private static class ActualizacionFecha {
        String idReceta;
        int diaMes;

        ActualizacionFecha(String idReceta, int diaMes) {
            this.idReceta = idReceta;
            this.diaMes = diaMes;
        }
    }

    /**
     * 🚀 Actualiza todas las fechas en batch (sin llamadas redundantes)
     */
    private static void actualizarFechasEnBatch(Context context,
                                                List<ActualizacionFecha> actualizaciones) {
        if (actualizaciones.isEmpty()) return;

        Log.d(TAG, "🔄 Actualizando " + actualizaciones.size() + " fechas en batch");

        // Agrupar por día para evitar actualizaciones duplicadas
        Map<String, Integer> actualizacionesUnicas = actualizaciones.stream()
                .collect(Collectors.toMap(
                        a -> a.idReceta,
                        a -> a.diaMes,
                        (d1, d2) -> d2 // Si hay duplicados, quedarse con el último
                ));

        // Actualizar todas las fechas
        actualizacionesUnicas.forEach((idReceta, diaMes) ->
                RecetasSrv.actualizarRecetaCalendario(context, idReceta, diaMes, true));
    }

    private static void addReceta(Queue<Receta> cola,
                                  Set<Receta> recetasUtilizadasRecientemente,
                                  Day dia,
                                  List<ActualizacionFecha> actualizacionesPendientes) {
        Receta receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia);

        if (receta != null) {
            recetasUtilizadasRecientemente.add(receta);
            cola.offer(receta);
            dia.getRecetas().add(new RecetaDia(receta.getId(), receta.getNumPersonas()));

            // 🚀 En lugar de actualizar inmediatamente, añadir a la lista de pendientes
            actualizacionesPendientes.add(new ActualizacionFecha(receta.getId(), dia.getDayOfMonth()));
        }
    }

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola,
                                                  Set<Receta> recetasUtilizadasRecientemente,
                                                  Day dia) {
        Queue<Receta> colaOriginal = new LinkedList<>(cola);

        while (!cola.isEmpty()) {
            Receta receta = cola.poll();
            if (!recetasUtilizadasRecientemente.contains(receta)) {
                assert receta != null;
                if (!recetaRepetidaEnProximosDias(receta, dia)) {
                    colaOriginal.offer(receta);
                    return receta;
                }
            }
        }

        cola.addAll(colaOriginal);
        return null;
    }

    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recetasUtilizadasRecientemente,
                                                              Day day) {
        Set<Receta> recetasAEliminar = new HashSet<>();

        for (Receta receta : recetasUtilizadasRecientemente) {
            int diasDesdeUltimaUtilizacion = obtenerDiasDesdeUltimaUtilizacion(receta, day);
            if (diasDesdeUltimaUtilizacion >= LIMITE_DIAS) {
                recetasAEliminar.add(receta);
            }
        }

        recetasUtilizadasRecientemente.removeAll(recetasAEliminar);
    }

    /**
     * Comprueba si una receta ya está programada en los próximos LIMITE_DIAS días
     */
    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia) {
        Date fechaReceta = receta.getFechaCalendario();

        if (fechaReceta == null) return false;

        // Fecha objetivo: día actual del calendario
        Calendar calDia = Calendar.getInstance();
        calDia.set(Calendar.HOUR_OF_DAY, 0);
        calDia.set(Calendar.MINUTE, 0);
        calDia.set(Calendar.SECOND, 0);
        calDia.set(Calendar.MILLISECOND, 0);
        calDia.set(Calendar.DAY_OF_MONTH, dia.getDayOfMonth());

        Calendar calReceta = Calendar.getInstance();
        calReceta.setTime(fechaReceta);
        calReceta.set(Calendar.HOUR_OF_DAY, 0);
        calReceta.set(Calendar.MINUTE, 0);
        calReceta.set(Calendar.SECOND, 0);
        calReceta.set(Calendar.MILLISECOND, 0);

        long diffMillis = calReceta.getTimeInMillis() - calDia.getTimeInMillis();
        long diffDays = diffMillis / (24L * 60L * 60L * 1000L);

        return diffDays >= 1 && diffDays <= LIMITE_DIAS;
    }

    private static int obtenerDiasDesdeUltimaUtilizacion(Receta receta, Day day) {
        Date fechaUltimaUtilizacion = receta.getFechaCalendario();

        if (fechaUltimaUtilizacion == null) return Integer.MAX_VALUE; // no usada antes

        Calendar calReceta = Calendar.getInstance();
        calReceta.setTime(fechaUltimaUtilizacion);
        calReceta.set(Calendar.HOUR_OF_DAY, 0);
        calReceta.set(Calendar.MINUTE, 0);
        calReceta.set(Calendar.SECOND, 0);
        calReceta.set(Calendar.MILLISECOND, 0);

        Calendar calObjetivo = Calendar.getInstance();
        calObjetivo.set(Calendar.HOUR_OF_DAY, 0);
        calObjetivo.set(Calendar.MINUTE, 0);
        calObjetivo.set(Calendar.SECOND, 0);
        calObjetivo.set(Calendar.MILLISECOND, 0);
        calObjetivo.set(Calendar.DAY_OF_MONTH, day.getDayOfMonth());

        long diferenciaMillis = Math.abs(calObjetivo.getTimeInMillis() - calReceta.getTimeInMillis());
        return (int) (diferenciaMillis / (24L * 60L * 60L * 1000L));
    }

    public static boolean esDiaAnteriorAlActual(Day day) {
        Calendar calHoy = Calendar.getInstance();
        int dayOfMonthHoy = calHoy.get(Calendar.DAY_OF_MONTH);
        return day.getDayOfMonth() < dayOfMonthHoy;
    }

    // ==================== LISTA DE COMPRA OPTIMIZADA ====================

    /**
     * 🚀 VERSIÓN OPTIMIZADA - Usa caché de recetas
     */
    public static void getListaCompra(Context context, int diaInicio, int diaFin,
                                      ListaCompraCallback callback) {
        obtenerCalendario(context, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> calendario) {
                // 🚀 Usar caché de recetas (no forzar servidor)
                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> listaRecetas) {
                        Map<String, Map<String, BigDecimal>> resultado = new ConcurrentHashMap<>();

                        calendario.parallelStream()
                                .filter(dia -> dia.getDayOfMonth() >= diaInicio &&
                                        dia.getDayOfMonth() <= diaFin)
                                .flatMap(d -> RecetasSrv.getRecetasAdaptadasCalendario(listaRecetas, d).stream())
                                .flatMap(receta -> receta.getIngredientes().stream())
                                .forEach(ingrediente -> {
                                    String nombreIngrediente = ingrediente.getNombre();
                                    String tipoCantidad = ingrediente.getTipoCantidad();
                                    BigDecimal cantidad = BigDecimal.valueOf(
                                            UtilsSrv.convertirNumero(ingrediente.getCantidad()));

                                    resultado.computeIfAbsent(nombreIngrediente, k -> new HashMap<>())
                                            .merge(tipoCantidad, cantidad, BigDecimal::add);
                                });

                        // Construcción de la lista de compra
                        StringBuilder listaCompra = new StringBuilder();
                        resultado.forEach((nombreIngrediente, ingredientesMap) ->
                                ingredientesMap.forEach((tipoCantidad, cantidad) ->
                                        listaCompra.append(cantidad.stripTrailingZeros().toPlainString())
                                                .append(" ")
                                                .append(tipoCantidad)
                                                .append(" ")
                                                .append(nombreIngrediente)
                                                .append("\n")));

                        callback.onSuccess(listaCompra.toString());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /** Establece el ID de usuario para Firebase */
    public static void setUserId(String userId) {
        firebaseManager.setUserId(userId);
    }
}