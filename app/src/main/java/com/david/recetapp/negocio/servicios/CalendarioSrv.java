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

public class CalendarioSrv {
    private static final String TAG = "CalendarioSrv";
    private static final int LIMITE_DIAS = 3;
    private static final FirebaseManager firebaseManager = new FirebaseManager();

    /**
     * Callback para operaciones de calendario
     */
    public interface CalendarioCallback {
        void onSuccess(List<Day> days);
        void onFailure(Exception e);
    }

    /**
     * Callback simple
     */
    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Callback para lista de compra
     */
    public interface ListaCompraCallback {
        void onSuccess(String listaCompra);
        void onFailure(Exception e);
    }

    public static void obtenerCalendario(Context context, CalendarioCallback callback) {
        firebaseManager.obtenerCalendario(new FirebaseManager.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (days.isEmpty()) {
                    // No existe calendario, generar uno nuevo
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
                // En caso de error, generar calendario local
                List<Day> days = generateDays();
                callback.onSuccess(days);
            }
        });
    }

    private static List<Day> generateDays() {
        List<Day> days = new ArrayList<>();

        // Obtenemos el mes y el año
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        // Ponemos el calendario al primer dia del mes
        calendar.set(currentYear, currentMonth, 1);

        // Obtenemos los dias del mes
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Generamos los Days para el mes actual sin recetas
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new Day(i, new ArrayList<>()));
        }

        return days;
    }

    private static void guardarCalendario(Context context, List<Day> days, SimpleCallback callback) {
        firebaseManager.guardarCalendario(days, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando calendario", e);
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void actualizarDia(Activity activity, Day selectedDay, SimpleCallback callback) {
        obtenerCalendario(activity, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> dias) {
                // Mapeamos los días por día del mes para búsqueda más eficiente
                Map<Integer, Day> diasMap = dias.stream().collect(Collectors.toMap(Day::getDayOfMonth, day -> day));

                Day dia = diasMap.get(selectedDay.getDayOfMonth());

                if (dia != null) {
                    dia.setRecetas(selectedDay.getRecetas());

                    // Actualizar en Firebase
                    firebaseManager.actualizarDia(dia, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            // Actualizar fechas de calendario de las recetas
                            RecetasSrv.cargarListaRecetas(activity, new RecetasSrv.RecetasCallback() {
                                @Override
                                public void onSuccess(List<Receta> listaRecetas) {
                                    listaRecetas.stream()
                                            .filter(r -> dia.getRecetas().stream()
                                                    .map(RecetaDia::getIdReceta)
                                                    .anyMatch(dr -> dr.equals(r.getId())))
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
                            UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onFailure(e);
                        }
                    });
                } else {
                    UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onFailure(new Exception("Día no encontrado"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

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
                    RecetasSrv.actualizarRecetaCalendario(activity, idReceta, dia.get().getDayOfMonth(), false);
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

    public static void cargarRecetas(Context context, SimpleCallback callback) {
        obtenerCalendario(context, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> calendar) {
                RecetasSrv.cargarListaRecetasCalendario(context, new ArrayList<>(), new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> recetas) {
                        // Convertir la lista a una cola (Queue)
                        Queue<Receta> cola = new LinkedList<>(recetas);

                        // Conjunto para realizar un seguimiento de las recetas utilizadas recientemente
                        Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();

                        for (Day dia : calendar) {
                            dia.setRecetas(new ArrayList<>());
                            if (!esDiaAnteriorAlActual(dia)) {
                                // Seleccionar recetas
                                addReceta(cola, recetasUtilizadasRecientemente, dia);
                                addReceta(cola, recetasUtilizadasRecientemente, dia);

                                // Actualizar las recetas del calendario
                                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                                    @Override
                                    public void onSuccess(List<Receta> listaRecetas) {
                                        listaRecetas.stream()
                                                .filter(r -> dia.getRecetas().stream()
                                                        .map(RecetaDia::getIdReceta)
                                                        .anyMatch(dr -> dr.equals(r.getId())))
                                                .forEach(r -> RecetasSrv.actualizarRecetaCalendario(
                                                        context, r.getId(), dia.getDayOfMonth(), true));
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Error cargando recetas", e);
                                    }
                                });

                                // Limpiar las recetas utilizadas recientemente después de 3 días
                                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia);
                            }
                        }

                        // Guardar el calendario
                        guardarCalendario(context, calendar, callback);
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

    private static void addReceta(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia) {
        Receta receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia);
        if (receta != null) {
            // Marcar la receta como utilizada recientemente
            recetasUtilizadasRecientemente.add(receta);

            // Poner la receta al final de la cola para su uso futuro
            cola.offer(receta);

            // Agregar el ID de la receta al día actual
            dia.getRecetas().add(new RecetaDia(receta.getId(), receta.getNumPersonas()));
        }
    }

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia) {
        // Copiar la cola original para mantener el estado original
        Queue<Receta> colaOriginal = new LinkedList<>(cola);

        // Obtener y eliminar recetas de la cola hasta encontrar una que no se haya utilizado recientemente
        while (!cola.isEmpty()) {
            Receta receta = cola.poll();
            if (!recetasUtilizadasRecientemente.contains(receta)) {
                assert receta != null;
                if (!recetaRepetidaEnProximosDias(receta, dia)) {
                    // Poner la receta al final de la cola original
                    colaOriginal.offer(receta);
                    return receta;
                }
            }
        }

        // Restaurar la cola original
        cola.addAll(colaOriginal);
        return null;
    }

    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recetasUtilizadasRecientemente, Day day) {
        // Limpiar las recetas utilizadas recientemente que tienen más de "limiteDias" días
        Set<Receta> recetasAEliminar = new HashSet<>();
        for (Receta receta : recetasUtilizadasRecientemente) {
            int diasDesdeUltimaUtilizacion = obtenerDiasDesdeUltimaUtilizacion(receta, day);

            if (diasDesdeUltimaUtilizacion >= CalendarioSrv.LIMITE_DIAS) {
                recetasAEliminar.add(receta);
            }
        }

        // Eliminar las recetas que han superado el límite de días
        recetasUtilizadasRecientemente.removeAll(recetasAEliminar);
    }

    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia) {
        Date fechaReceta = receta.getFechaCalendario();
        if (fechaReceta != null) {
            Calendar calReceta = Calendar.getInstance();
            calReceta.setTime(fechaReceta);

            for (int i = 1; i <= CalendarioSrv.LIMITE_DIAS; i++) {
                calReceta.add(Calendar.DAY_OF_MONTH, 1);
                int dayOfMonthFuturo = calReceta.get(Calendar.DAY_OF_MONTH);
                if (dia.getDayOfMonth() + i == dayOfMonthFuturo) {
                    // Verificar si la receta está programada en el día futuro
                    return dia.getRecetas().stream()
                            .map(RecetaDia::getIdReceta)
                            .anyMatch(dr -> dr.equals(receta.getId()));
                }
            }
        }
        return false;
    }

    private static int obtenerDiasDesdeUltimaUtilizacion(Receta receta, Day day) {
        Date fechaUltimaUtilizacion = receta.getFechaCalendario();
        if (fechaUltimaUtilizacion != null) {
            Calendar calUltimaUtilizacion = Calendar.getInstance();
            calUltimaUtilizacion.setTime(fechaUltimaUtilizacion);

            // Utilizar el día del objeto Day proporcionado
            calUltimaUtilizacion.set(Calendar.DAY_OF_MONTH, day.getDayOfMonth());

            Calendar calDay = Calendar.getInstance();
            calDay.setTime(receta.getFechaCalendario());

            // Calcular la diferencia en días sin contar las horas exactas
            long diferenciaMillis = calUltimaUtilizacion.getTimeInMillis() - calDay.getTimeInMillis();

            return (int) (diferenciaMillis / (24 * 60 * 60 * 1000));
        }
        return 0;
    }

    public static boolean esDiaAnteriorAlActual(Day day) {
        Calendar calHoy = Calendar.getInstance();
        int dayOfMonthHoy = calHoy.get(Calendar.DAY_OF_MONTH);

        return day.getDayOfMonth() < dayOfMonthHoy;
    }

    public static void getListaCompra(Context context, int diaInicio, int diaFin, ListaCompraCallback callback) {
        obtenerCalendario(context, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> calendario) {
                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> listaRecetas) {
                        // Usar ConcurrentHashMap para operaciones paralelas
                        Map<String, Map<String, BigDecimal>> resultado = new ConcurrentHashMap<>();

                        calendario.parallelStream()
                                .filter(dia -> dia.getDayOfMonth() >= diaInicio && dia.getDayOfMonth() <= diaFin)
                                .flatMap(d -> RecetasSrv.getRecetasAdaptadasCalendario(listaRecetas, d).stream())
                                .flatMap(receta -> receta.getIngredientes().stream())
                                .forEach(ingrediente -> {
                                    String nombreIngrediente = ingrediente.getNombre();
                                    String tipoCantidad = ingrediente.getTipoCantidad();
                                    BigDecimal cantidad = BigDecimal.valueOf(UtilsSrv.convertirNumero(ingrediente.getCantidad()));
                                    resultado.computeIfAbsent(nombreIngrediente, k -> new HashMap<>())
                                            .merge(tipoCantidad, cantidad, BigDecimal::add);
                                });

                        // Construcción de la lista de compra como texto
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