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

    // Nuevo callback que devuelve el calendario modificado para UI optimista
    public interface RellenarCallback {
        void onSuccess(List<Day> updatedCalendar);
        void onFailure(Exception e);
    }

    public static void borrarYRecrearCalendario(Context context, CalendarioCallback callback) {
        firebaseManager.eliminarCalendarioActual(new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Trigger recreation by calling obtenerCalendario
                obtenerCalendario(context, callback);
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public static void obtenerCalendario(Context context, CalendarioCallback callback) {
        obtenerCalendario(context, null, callback);
    }

    public static void obtenerCalendario(Context context, List<Day> localDays, CalendarioCallback callback) {
        firebaseManager.obtenerCalendario(com.google.firebase.firestore.Source.SERVER, new FirebaseManager.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (days.isEmpty()) {
                    // 🗑️ Mes nuevo o inexistente: borrar el calendario anterior antes de generar el nuevo
                    firebaseManager.eliminarCalendarioMesAnterior(new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Mes anterior limpiado, generando nuevo calendario");
                            List<Day> newDays = generateDays();
                            if (localDays != null && !localDays.isEmpty()) {
                                newDays = mergeCalendarios(localDays, newDays);
                            }
                            // 🚀 Auto-rellenar con recetas al crear
                            cargarRecetasAlCrear(context, newDays, callback);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.w(TAG, "No se pudo borrar mes anterior, continuando igualmente", e);
                            List<Day> newDays = generateDays();
                            if (localDays != null && !localDays.isEmpty()) {
                                newDays = mergeCalendarios(localDays, newDays);
                            }
                            cargarRecetasAlCrear(context, newDays, callback);
                        }
                    });
                } else {
                    List<Day> finalDays = days;
                    if (localDays != null && !localDays.isEmpty()) {
                        finalDays = mergeCalendarios(localDays, days);
                        // Si hubo cambios, sincronizar de vuelta a Firebase en background
                        if (!finalDays.equals(days)) {
                            Log.d(TAG, "Sincronizando calendario mergeado a Firebase");
                            guardarCalendario(context, finalDays, null);
                        }
                    }
                    callback.onSuccess(finalDays);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo calendario del servidor, intentando caché local", e);
                // Si falla servidor (ej: offline), intentar usar el de caché persistente de Firestore
                firebaseManager.obtenerCalendario(com.google.firebase.firestore.Source.CACHE, new FirebaseManager.CalendarioCallback() {
                    @Override
                    public void onSuccess(List<Day> cachedDays) {
                        List<Day> result = cachedDays;
                        if (result.isEmpty()) {
                            result = generateDays();
                        }
                        if (localDays != null && !localDays.isEmpty()) {
                            result = mergeCalendarios(localDays, result);
                        }
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        Log.e(TAG, "Error obteniendo calendario de caché", ex);
                        List<Day> result = generateDays();
                        if (localDays != null && !localDays.isEmpty()) {
                            result = mergeCalendarios(localDays, result);
                        }
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    /**
     * Mezcla dos listas de días, priorizando recetas pero evitando duplicados por ID.
     */
    private static List<Day> mergeCalendarios(List<Day> local, List<Day> server) {
        if (local == null || local.isEmpty()) return server;
        if (server == null || server.isEmpty()) return local;

        Map<Integer, Day> mergedMap = new HashMap<>();
        // Primero cargamos todo lo del servidor
        for (Day d : server) {
            mergedMap.put(d.getDayOfMonth(), new Day(d.getDayOfMonth(), new ArrayList<>(d.getRecetas())));
        }

        // Luego mergeamos lo local
        for (Day localDay : local) {
            Day existing = mergedMap.get(localDay.getDayOfMonth());
            if (existing == null) {
                mergedMap.put(localDay.getDayOfMonth(), localDay);
            } else {
                // Mezclar recetas evitando duplicados por ID
                Set<String> recipeIds = existing.getRecetas().stream()
                        .map(RecetaDia::getIdReceta)
                        .collect(Collectors.toSet());

                for (RecetaDia rd : localDay.getRecetas()) {
                    if (!recipeIds.contains(rd.getIdReceta())) {
                        existing.getRecetas().add(rd);
                        recipeIds.add(rd.getIdReceta());
                    }
                }
            }
        }

        return mergedMap.values().stream()
                .sorted((d1, d2) -> d1.getDayOfMonth() - d2.getDayOfMonth())
                .collect(Collectors.toList());
    }

    /**
     * Rellena el calendario con recetas inmediatamente después de crearlo
     */
    private static void cargarRecetasAlCrear(Context context, List<Day> newDays, CalendarioCallback callback) {
        RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                if (recetas.isEmpty()) {
                    guardarCalendario(context, newDays, new SimpleCallback() {
                        @Override
                        public void onSuccess() { callback.onSuccess(newDays); }
                        @Override
                        public void onFailure(Exception e) { callback.onFailure(e); }
                    });
                    return;
                }

                Queue<Receta> cola = new LinkedList<>(recetas);
                Set<Receta> recientes = new HashSet<>();
                List<ActualizacionFecha> updates = new ArrayList<>();

                for (Day d : newDays) {
                    if (!esDiaAnteriorAlActual(d)) {
                        addReceta(cola, recientes, d, updates, -1);
                        addReceta(cola, recientes, d, updates, -1);
                        limpiarRecetasUtilizadasRecientemente(recientes, d);
                    }
                }

                Map<String, Long> fechaMap = new HashMap<>();
                for (ActualizacionFecha af : updates) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, af.diaMes);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    fechaMap.put(af.idReceta, cal.getTimeInMillis());
                }

                firebaseManager.guardarCalendarioConFechas(newDays, fechaMap, new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        UtilsSrv.notificacion(context, context.getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
                        callback.onSuccess(newDays);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Si fallan las recetas, guardar el calendario vacío al menos
                guardarCalendario(context, newDays, new SimpleCallback() {
                    @Override
                    public void onSuccess() { callback.onSuccess(newDays); }
                    @Override
                    public void onFailure(Exception e) { callback.onFailure(e); }
                });
            }
        });
    }

    // Devuelve el calendario cacheado si está válido (sin I/O) — útil para UI rápida
    public static List<Day> obtenerCalendarioCache(Context context) {
        return firebaseManager.getCachedCalendarioIfValid();
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
        firebaseManager.actualizarDia(selectedDay, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                RecetasSrv.cargarListaRecetas(activity, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> listaRecetas) {
                        Set<String> idsRecetasDia = selectedDay.getRecetas().stream()
                                .map(RecetaDia::getIdReceta)
                                .collect(Collectors.toSet());

                        listaRecetas.stream()
                                .filter(r -> idsRecetasDia.contains(r.getId()))
                                .forEach(r -> RecetasSrv.actualizarRecetaCalendarioDirect(r, selectedDay.getDayOfMonth(), true));

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
    }

    // ==================== ACTUALIZAR FECHA CALENDARIO ====================

    // Aplicar actualización localmente en caché (sin I/O) para UI instantánea
    public static void aplicarActualizacionLocalDia(Day day) {
        firebaseManager.applyLocalDayUpdate(day);
    }

    // Actualizar día en Firebase en background sin bloquear la UI ni cargar recetas
    public static void actualizarDiaAsync(Day day) {
        firebaseManager.actualizarDia(day, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Actualización de día enviada a Firebase (async): " + day.getDayOfMonth());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error sincronizando día en background", e);
                // Error en background: registrar para diagnóstico (no bloquear UI)
                Log.w(TAG, "Error sincronizando día en background: " + e.getMessage());
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
            if (!esDiaAnteriorAlActual(dia)) {
                dia.setRecetas(new ArrayList<>());
                // Añadir 2 recetas al día
                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, -1);
                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, -1);

                // Limpiar recetas antiguas
                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia);
            }
        }

        // 🚀 Actualizar TODAS las fechas y guardar calendario en UN write-batch (menos round-trips)
        // Construir mapa recetaId -> timestamp
        Map<String, Long> fechaMap = new HashMap<>();
        for (ActualizacionFecha af : actualizacionesPendientes) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, af.diaMes);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            fechaMap.put(af.idReceta, cal.getTimeInMillis());
        }

        // Llamada en background: FirebaseManager hará el batch de calendario + recetas
        firebaseManager.guardarCalendarioConFechas(calendar, fechaMap, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Batch guardado: calendario y fechas de recetas");
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando batch calendario+fechas", e);
                if (callback != null) callback.onFailure(e);
            }
        });
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

        Log.d(TAG, "🔄 Actualizando " + actualizaciones.size() + " fechas en batch (optimizado)");

        // Agrupar por día para evitar actualizaciones duplicadas
        Map<String, Integer> actualizacionesUnicas = actualizaciones.stream()
                .collect(Collectors.toMap(
                        a -> a.idReceta,
                        a -> a.diaMes,
                        (d1, d2) -> d2 // Si hay duplicados, quedarse con el último
                ));

        // Actualizar usando la API pública de FirebaseManager para evitar cargas de recetas y acelerar el proceso
        for (Map.Entry<String, Integer> e : actualizacionesUnicas.entrySet()) {
            String idReceta = e.getKey();
            int diaMes = e.getValue();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, diaMes);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date fecha = cal.getTime();

            // Llamada asíncrona al FirebaseManager para actualizar la fecha
            firebaseManager.actualizarFechaCalendario(idReceta, fecha.getTime(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Fecha actualizada para receta: " + idReceta);
                }

                @Override
                public void onFailure(Exception ex) {
                    Log.e(TAG, "Error actualizando fecha para " + idReceta, ex);
                }
            });
        }
    }

    private static void addReceta(Queue<Receta> cola,
                                   Set<Receta> recetasUtilizadasRecientemente,
                                   Day dia,
                                   List<ActualizacionFecha> actualizacionesPendientes,
                                   int numPersonas) {
        // Intentar hasta N veces encontrar una receta válida que no esté ya en el día
        final int MAX_TRIES = 6;
        int tries = 0;
        Receta receta = null;

        while (tries < MAX_TRIES && !cola.isEmpty()) {
            receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia);
            tries++;
            if (receta == null) break;

            final String recetaId = receta.getId();
            boolean yaEnDia = dia.getRecetas().stream()
                    .anyMatch(rd -> rd.getIdReceta().equals(recetaId));
            if (yaEnDia) {
                // Ya existe en este día, intentar siguiente
                cola.offer(receta); // devolver al final
                receta = null;
                continue;
            }

            // Si pasa las comprobaciones, lo usamos
            receta.setFechaCalendario(new Date(0));
            recetasUtilizadasRecientemente.add(receta);
            cola.offer(receta);

            int personasToSet = (numPersonas > 0) ? numPersonas : receta.getNumPersonas();
            if (personasToSet <= 0) personasToSet = 2; // Fallback

            dia.getRecetas().add(new RecetaDia(recetaId, personasToSet));

            // Añadir a actualizaciones pendientes
            actualizacionesPendientes.add(new ActualizacionFecha(recetaId, dia.getDayOfMonth()));
            break;
        }
    }

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola,
                                                  Set<Receta> recetasUtilizadasRecientemente,
                                                  Day dia) {
        for (Receta receta : cola) { // iteración no destructiva
            if (!recetasUtilizadasRecientemente.contains(receta) &&
                    !recetaRepetidaEnProximosDias(receta, dia)) {
                cola.remove(receta); // elimina solo la seleccionada
                return receta;       // addReceta la re-añadirá al final
            }
        }
        return null; // no hay ninguna válida, cola intacta
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

    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia) {
        Date fechaReceta = receta.getFechaCalendario();
        if (fechaReceta == null) return false;

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

        long diffMillis = calDia.getTimeInMillis() - calReceta.getTimeInMillis(); // ← invertido
        long diffDays = diffMillis / (24L * 60L * 60L * 1000L);

        return diffDays >= 0 && diffDays <= LIMITE_DIAS;
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
                        Log.d(TAG, "🚀 Generando lista de compra para " + (diaFin - diaInicio + 1) + " días");
                        try {
                            RecetasSrv.inicializarMapas(context);
                            // Usamos ConcurrentHashMap en ambos niveles para seguridad en parallelStream
                            // La clave del primer mapa será el nombre normalizado (lowercase)
                            Map<String, ConcurrentHashMap<String, BigDecimal>> resultado = new ConcurrentHashMap<>();
                            // Mapa para mantener el nombre original a mostrar
                            Map<String, String> nombresDisplay = new ConcurrentHashMap<>();

                            calendario.parallelStream()
                                    .filter(dia -> dia.getDayOfMonth() >= diaInicio &&
                                            dia.getDayOfMonth() <= diaFin)
                                    .flatMap(d -> {
                                        List<Receta> adaptadas = RecetasSrv.getRecetasAdaptadasCalendario(listaRecetas, d);
                                        Log.d(TAG, "📅 Día " + d.getDayOfMonth() + ": " + adaptadas.size() + " recetas");
                                        return adaptadas.stream();
                                    })
                                    .flatMap(receta -> receta.getIngredientes().stream())
                                    .forEach(ingrediente -> {
                                        String nombreOriginal = ingrediente.getNombre();
                                        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) return;

                                        String nombreNormalizado = nombreOriginal.trim().toLowerCase(java.util.Locale.getDefault());
                                        nombresDisplay.putIfAbsent(nombreNormalizado, nombreOriginal.trim());

                                        String tipoCantidad = ingrediente.getTipoCantidad() != null ? ingrediente.getTipoCantidad() : "";
                                        BigDecimal cantidad = BigDecimal.valueOf(
                                                UtilsSrv.convertirNumero(ingrediente.getCantidad()));

                                        resultado.computeIfAbsent(nombreNormalizado, k -> new ConcurrentHashMap<>())
                                                .merge(tipoCantidad, cantidad, BigDecimal::add);
                                    });

                            Log.d(TAG, "📦 Consolidando unidades para " + resultado.size() + " ingredientes únicos");

                            // Consolidación de unidades a gramos si hay mezcla
                            resultado.forEach((nombreNorm, mapUnidades) -> {
                                if (mapUnidades.size() > 1) {
                                    BigDecimal totalGramos = BigDecimal.ZERO;
                                    boolean convertidaAlguna = false;

                                    for (Map.Entry<String, BigDecimal> entry : mapUnidades.entrySet()) {
                                        String unidad = entry.getKey();
                                        BigDecimal cant = entry.getValue();

                                        BigDecimal gramos = convertirAGramos(nombreNorm, unidad, cant);
                                        if (gramos != null) {
                                            totalGramos = totalGramos.add(gramos);
                                            convertidaAlguna = true;
                                        }
                                    }

                                    if (convertidaAlguna) {
                                        mapUnidades.clear();
                                        mapUnidades.put("g", totalGramos);
                                    }
                                }
                            });

                            // Construcción de la lista de compra
                            StringBuilder listaCompra = new StringBuilder();
                            resultado.keySet().stream()
                                    .sorted()
                                    .forEach(nombreNorm -> {
                                        String display = nombresDisplay.get(nombreNorm);
                                        if (display != null && !display.isEmpty()) {
                                            display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
                                        }

                                        ConcurrentHashMap<String, BigDecimal> unidades = resultado.get(nombreNorm);
                                        if (unidades != null) {
                                            String finalDisplay = display;
                                            unidades.forEach((tipoCantidad, cantidad) -> {
                                                String cantStr = cantidad.stripTrailingZeros().toPlainString();
                                                listaCompra.append(cantStr)
                                                        .append(" ")
                                                        .append(tipoCantidad)
                                                        .append(" ")
                                                        .append(finalDisplay)
                                                        .append("\n");
                                            });
                                        }
                                    });

                            Log.d(TAG, "✅ Lista de compra generada exitosamente");
                            callback.onSuccess(listaCompra.toString());

                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error fatal generando lista de compra", e);
                            callback.onFailure(e);
                        }
                    }

                    private BigDecimal convertirAGramos(String nombre, String unidad, BigDecimal cantidad) {
                        if ("g".equalsIgnoreCase(unidad)) return cantidad;
                        if ("kg".equalsIgnoreCase(unidad)) return cantidad.multiply(new BigDecimal("1000"));
                        if ("ml".equalsIgnoreCase(unidad)) return cantidad;
                        if ("l".equalsIgnoreCase(unidad)) return cantidad.multiply(new BigDecimal("1000"));

                        if ("unidad".equalsIgnoreCase(unidad)) {
                            Integer gPorUnidad = RecetasSrv.gramosMapCache.get(nombre.toLowerCase(java.util.Locale.getDefault()));
                            if (gPorUnidad != null && gPorUnidad > 0) {
                                return cantidad.multiply(new BigDecimal(gPorUnidad));
                            }
                        }

                        Integer importancia = RecetasSrv.unitImportanceMapCache.get(unidad);
                        if (importancia != null && importancia > 0) {
                            return cantidad.multiply(new BigDecimal(importancia));
                        }

                        return null;
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

    /** Rellena un rango de días añadiendo recetas (no borra recetas existentes).
     *  Si forzarPasados==true, también rellenará días anteriores al día actual.
     */
    public static void rellenarRangoDias(final Context context, final int diaInicio, final int diaFin,
                                        final boolean forzarPasados, final int numPersonas, final RellenarCallback callback) {
        obtenerCalendario(context, new CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> calendario) {
                // Cargar recetas disponibles desde caché/servicio
                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> recetasDisponibles) {
                        Queue<Receta> cola = new LinkedList<>(recetasDisponibles);
                        Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();
                        List<ActualizacionFecha> actualizacionesPendientes = new ArrayList<>();

                        for (Day dia : calendario) {
                            if (dia.getDayOfMonth() >= diaInicio && dia.getDayOfMonth() <= diaFin) {
                                if (!forzarPasados && esDiaAnteriorAlActual(dia)) continue;

                                if (dia.getRecetas() == null) dia.setRecetas(new ArrayList<>());

                                // Añadir hasta 2 recetas al día (sin eliminar las existentes)
                                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, numPersonas);
                                addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, numPersonas);

                                // Limpiar set de recientes según la lógica existente
                                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia);
                            }
                        }

                        // Construir mapa recetaId -> timestamp para el batch
                        Map<String, Long> fechaMap = new HashMap<>();
                        for (ActualizacionFecha af : actualizacionesPendientes) {
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.DAY_OF_MONTH, af.diaMes);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            fechaMap.put(af.idReceta, cal.getTimeInMillis());
                        }

                        // Guardar calendario + fechas en un único batch (background)
                        firebaseManager.guardarCalendarioConFechas(calendario, fechaMap, new FirebaseManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // Notificar al usuario en el hilo principal que el guardado finalizó
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                        UtilsSrv.notificacion(context, context.getString(R.string.calendario_actualizado), android.widget.Toast.LENGTH_SHORT)
                                );
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Loguear y notificar si hace falta
                                android.util.Log.e(TAG, "Error guardando calendario+fechas en background", e);
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                        UtilsSrv.notificacion(context, context.getString(R.string.calendario_no_actualizado), android.widget.Toast.LENGTH_SHORT)
                                );
                            }
                        });

                        // Devolver el calendario modificado de forma inmediata para UI optimista
                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(calendario));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (callback != null) callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    /** Establece el ID de usuario para Firebase */
    public static void setUserId(String userId) {
        firebaseManager.setUserId(userId);
    }

    // Merge helper local a este servicio para construir calendarios modificados rápidamente
    private static List<Day> mergeDayIntoList(List<Day> existing, Day newDay) {
        if (existing == null) existing = new ArrayList<>();
        List<Day> result = new ArrayList<>(existing);
        boolean found = false;
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getDayOfMonth() == newDay.getDayOfMonth()) {
                result.set(i, newDay);
                found = true;
                break;
            }
        }
        if (!found) result.add(newDay);
        return result;
    }

    /**
     * Guarda un único día y la fecha de una receta en un solo batch.
     * Si existe calendario en caché hace merge y ejecuta un WriteBatch que
     * actualiza el documento de calendario y la receta en un solo commit.
     */
    public static void guardarDiaYRecetaBatch(final Context context, final Day day, final String recetaId) {
        // Intentar usar caché para construir el calendario completo
        List<Day> cached = obtenerCalendarioCache(context);
        if (cached != null) {
            List<Day> updated = mergeDayIntoList(cached, day);

            Map<String, Long> fechaMap = new HashMap<>();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, day.getDayOfMonth());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            fechaMap.put(recetaId, cal.getTimeInMillis());

            firebaseManager.guardarCalendarioConFechas(updated, fechaMap, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "guardarDiaYRecetaBatch: éxito para receta " + recetaId);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "guardarDiaYRecetaBatch: fallo", e);
                }
            });
            return;
        }

        // Fallback: si no hay caché válida, usar la API existente para actualizar día y receta por separado
        firebaseManager.actualizarDia(day, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Actualizar fecha de receta por separado
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, day.getDayOfMonth());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                firebaseManager.actualizarFechaCalendario(recetaId, cal.getTimeInMillis(), new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Fallback: día y receta actualizados");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Fallback: error actualizando fecha receta", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Fallback: error actualizando día", e);
            }
        });
    }
}