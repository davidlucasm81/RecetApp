package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.MomentoReceta;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.beans.TipoReceta;

import com.david.recetapp.negocio.beans.TipoIngrediente;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import com.google.firebase.auth.FirebaseAuth;

/**
 * 🚀 Servicio de Calendario OPTIMIZADO
 * - Elimina llamadas redundantes a Firebase
 * - Reduce operaciones N+1
 * - Usa caché de manera eficiente
 */
public class CalendarioSrv {
    private static final String TAG = "CalendarioSrv";
    private static final int LIMITE_DIAS = 14;
    private static final java.util.Random RANDOM = new java.util.Random();
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    // Crear FirebaseManager pasando el userId actual (si existe) o "default_user" en caso contrario.
    private static final FirebaseManager firebaseManager = new FirebaseManager(
            FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "default_user"
    );

    // ← ID del usuario activo; toda operación con datos requiere que esté configurado
    private static volatile String currentUserId = null;

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
        void onFailure(@SuppressWarnings("unused") Exception e);
    }

    public interface RellenarCallback {
        void onSuccess(List<Day> updatedCalendar);
        void onFailure(@SuppressWarnings("unused") Exception e);
    }

    // ——— Helpers de validación de userId ———

    /**
     * Comprueba que no hay un userId activo antes de cualquier operación con datos.
     * Llama a {@code cb.onFailure} y devuelve {@code true} si no está configurado.
     */
    private static boolean checkNotUserId(CalendarioCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return true;
        }
        return false;
    }

    private static boolean checkUserId(SimpleCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return false;
        }
        return true;
    }

    private static boolean checkUserId(ListaCompraCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return false;
        }
        return true;
    }

    private static boolean checkUserId(RellenarCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return false;
        }
        return true;
    }

    /** Versión sin callback para métodos fire-and-forget. */
    private static boolean notHasUserId() {
        return currentUserId == null || currentUserId.isEmpty();
    }

    // ——— CALENDARIO ———

    public static void borrarYRecrearCalendario(Context context, int mes, int anio, CalendarioCallback callback) {
        if (checkNotUserId(callback)) return;

        firebaseManager.eliminarCalendario(mes, anio, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                obtenerCalendario(context, mes, anio, callback);
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public static void obtenerCalendario(Context context, int mes, int anio, CalendarioCallback callback) {
        if (checkNotUserId(callback)) return;
        obtenerCalendario(context, mes, anio, null, callback);
    }

    public static void obtenerCalendario(Context context, int mes, int anio, List<Day> localDays, CalendarioCallback callback) {
        if (checkNotUserId(callback)) return;

        firebaseManager.obtenerCalendario(mes, anio, null, new FirebaseManager.CalendarioCallback() {
            @Override
            public void onSuccess(List<Day> days) {
                if (days.isEmpty()) {
                    // Generar nuevo calendario sin borrar nada de otros meses
                    List<Day> newDays = generateDays(mes, anio);
                    if (localDays != null && !localDays.isEmpty()) {
                        newDays = mergeCalendarios(localDays, newDays);
                    }
                    final List<Day> finalNewDays = newDays;
                    guardarCalendario(context, mes, anio, newDays, new SimpleCallback() {
                        @Override
                        public void onSuccess() { callback.onSuccess(finalNewDays); }
                        @Override
                        public void onFailure(Exception e) { callback.onFailure(e); }
                    });
                } else {
                    List<Day> finalDays = days;
                    if (localDays != null && !localDays.isEmpty()) {
                        finalDays = mergeCalendarios(localDays, days);
                        if (!finalDays.equals(days)) {
                            Log.d(TAG, "Sincronizando calendario mergeado a Firebase");
                            guardarCalendario(context, mes, anio, finalDays, null);
                        }
                    }
                    callback.onSuccess(finalDays);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo calendario del servidor, intentando caché local", e);
                firebaseManager.obtenerCalendario(mes, anio, com.google.firebase.firestore.Source.CACHE, new FirebaseManager.CalendarioCallback() {
                    @Override
                    public void onSuccess(List<Day> cachedDays) {
                        List<Day> result = cachedDays;
                        if (result.isEmpty()) {
                            result = generateDays(mes, anio);
                        }
                        if (localDays != null && !localDays.isEmpty()) {
                            result = mergeCalendarios(localDays, result);
                        }
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        Log.e(TAG, "Error obteniendo calendario de caché", ex);
                        List<Day> result = generateDays(mes, anio);
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
        for (Day d : server) {
            mergedMap.put(d.getDayOfMonth(), new Day(d.getDayOfMonth(), new ArrayList<>(d.getRecetas())));
        }

        for (Day localDay : local) {
            Day existing = mergedMap.get(localDay.getDayOfMonth());
            if (existing == null) {
                mergedMap.put(localDay.getDayOfMonth(), localDay);
            } else {
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
                .sorted(Comparator.comparingInt(Day::getDayOfMonth))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el calendario cacheado si está válido (sin I/O).
     * Requiere userId activo; si no está configurado devuelve null.
     */
    public static List<Day> obtenerCalendarioCache(int mes, int anio) {
        if (notHasUserId()) {
            Log.w(TAG, "⚠️ obtenerCalendarioCache llamado sin userId — devuelve null");
            return null;
        }
        return firebaseManager.getCachedCalendarioIfValid(mes, anio);
    }

    private static List<Day> generateDays(int mes, int anio) {
        List<Day> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new Day(i, mes, anio, new ArrayList<>()));
        }

        return days;
    }

    private static void guardarCalendario(Context context, int mes, int anio, List<Day> days, SimpleCallback callback) {
        firebaseManager.guardarCalendario(mes, anio, days, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_actualizado),
                        Toast.LENGTH_LONG).show();
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando calendario", e);
                UtilsSrv.notificacion(context, context.getString(R.string.calendario_no_actualizado),
                        Toast.LENGTH_LONG).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    // ==================== ACTUALIZAR DÍA ====================

    public static void actualizarDia(Activity activity, int mes, int anio, Day selectedDay, SimpleCallback callback) {
        if (!checkUserId(callback)) return;

        firebaseManager.actualizarDia(mes, anio, selectedDay, new FirebaseManager.SimpleCallback() {
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
                                .forEach(r -> {
                                    // Para la actualización de fecha, usamos una fecha real del mes/año correspondiente
                                    Calendar cal = Calendar.getInstance();
                                    cal.set(anio, mes, selectedDay.getDayOfMonth(), 0, 0, 0);
                                    cal.set(Calendar.MILLISECOND, 0);
                                    RecetasSrv.actualizarRecetaCalendarioDirect(r, cal.getTime().getTime(), true);
                                });

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
                        Toast.LENGTH_LONG).show();
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    // ==================== ACTUALIZAR FECHA CALENDARIO ====================

    /** Aplica actualización localmente en caché (sin I/O) para UI instantánea. */
    public static void aplicarActualizacionLocalDia(int mes, int anio, Day day) {
        if (notHasUserId()) {
            Log.w(TAG, "⚠️ aplicarActualizacionLocalDia sin userId — operación ignorada");
            return;
        }
        firebaseManager.applyLocalDayUpdate(mes, anio, day);
    }

    public static void actualizarFechaCalendario(Activity activity, String idReceta, int mes, int anio) {
        if (notHasUserId()) {
            Log.e(TAG, "❌ actualizarFechaCalendario sin userId — operación ignorada");
            return;
        }

        obtenerCalendario(activity, mes, anio, new CalendarioCallback() {
            private boolean alreadyExecuted = false;

            @Override
            public void onSuccess(List<Day> dias) {
                if (alreadyExecuted) return;
                alreadyExecuted = true;

                Optional<Day> lastOccurrence = dias.stream()
                        .filter(d -> d.getRecetas().stream()
                                .map(RecetaDia::getIdReceta)
                                .anyMatch(dr -> dr.equals(idReceta)))
                        .max(Comparator.comparingInt(Day::getDayOfMonth));

                if (lastOccurrence.isPresent()) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(anio, mes, lastOccurrence.get().getDayOfMonth(), 0, 0, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    RecetasSrv.actualizarRecetaCalendario(activity, idReceta,
                            cal.getTimeInMillis(), false);
                } else {
                    RecetasSrv.actualizarRecetaCalendario(activity, idReceta, 0, false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error actualizando fecha calendario", e);
            }
        });
    }

    /**
         * 🚀 Clase auxiliar para batch updates
         */
        private record ActualizacionFecha(String idReceta, int diaMes) {
    }

    private static void addReceta(List<Receta> lista,
                                  Set<Receta> recetasUtilizadasRecientemente,
                                  Day dia,
                                  List<ActualizacionFecha> actualizacionesPendientes,
                                  int numPersonas,
                                  int mes,
                                  int anio,
                                  MomentoReceta momentoRequerido,
                                  Map<LocalDate, Receta> asignacionesPrincipales,
                                  Map<LocalDate, List<Receta>> asignacionesDiarias,
                                  Set<String> ingredientesActivosDia,
                                  Map<String, Date> fechasTemporales,
                                  Map<String, CachedRecetaData> cacheData,
                                  Map<LocalDate, Set<String>> ingredientesPorDia,
                                  WindowStats windowStats) {
        final int MAX_TRIES = 12;
        int tries = 0;
        Receta receta;
        LocalDate fechaActual = getLocalDate(dia.getDayOfMonth(), mes, anio);

        // M15: Fallback progresivo adaptativo según tamaño del catálogo
        int limiteBase = Math.min(LIMITE_DIAS, lista.size() / 2);
        int[] fallbackLimites = {limiteBase, limiteBase * 3 / 4, limiteBase / 2, 3, 0};

        // M18: Recalcular estadísticas antes del bucle de reintentos (Optimización)
        DailyStats dailyStats = getDailyStats(fechaActual, asignacionesDiarias);
        
        while (tries < MAX_TRIES && !lista.isEmpty()) {
            // M12: Pre-filtrado antes de ruleta para eficiencia
            List<Receta> candidatosPreFiltrados = lista.stream()
                    .filter(r -> !recetasUtilizadasRecientemente.contains(r))
                    .filter(r -> {
                        // Filtro de momento si se requiere
                        if (momentoRequerido != null) {
                            MomentoReceta mr = r.getMomentoReceta();
                            return mr == null || mr == MomentoReceta.AMBOS || mr == momentoRequerido;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            if (candidatosPreFiltrados.isEmpty() && momentoRequerido != null) {
                // Reintentar sin filtro de momento si no hay candidatos
                candidatosPreFiltrados = lista.stream()
                        .filter(r -> !recetasUtilizadasRecientemente.contains(r))
                        .collect(Collectors.toList());
            }
            
            if (candidatosPreFiltrados.isEmpty()) break;

            // M22: Optimización de búsqueda de candidatos (llamar a obtenerRecetaNoRepetida una sola vez con el mejor límite posible)
            receta = null;
            for (int limite : fallbackLimites) {
                receta = obtenerRecetaNoRepetida(candidatosPreFiltrados, dia, momentoRequerido,
                        windowStats, dailyStats, ingredientesActivosDia, limite, fechasTemporales, 
                        cacheData, ingredientesPorDia, fechaActual);
                if (receta != null) break;
            }
            
            tries++;
            if (receta == null) break; // Definitivamente no hay candidatos en esta lista

            final String recetaId = receta.getId();
            boolean yaEnDia = dia.getRecetas().stream()
                    .anyMatch(rd -> rd.getIdReceta().equals(recetaId));
            if (yaEnDia) {
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.set(anio, mes, dia.getDayOfMonth(), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date proposedDate = cal.getTime();

            // M8: Usar fechasTemporales
            Date fechaEf = fechasTemporales.getOrDefault(recetaId, receta.getFechaCalendario());
            if (fechaEf == null || proposedDate.after(fechaEf)) {
                fechasTemporales.put(recetaId, proposedDate);
                actualizacionesPendientes.add(new ActualizacionFecha(recetaId, dia.getDayOfMonth()));
            }

            // M4: Actualización inmediata
            recetasUtilizadasRecientemente.add(receta);
            if (momentoRequerido == MomentoReceta.COMIDA || momentoRequerido == MomentoReceta.CENA) {
                asignacionesPrincipales.put(fechaActual, receta);
                updateWindowStats(windowStats, receta, cacheData); // Actualización incremental
            }
            asignacionesDiarias.computeIfAbsent(fechaActual, k -> new ArrayList<>()).add(receta);
            
            // M6: Actualizar ingredientes activos del día
            CachedRecetaData cd = cacheData.get(recetaId);
            if (cd != null) {
                ingredientesActivosDia.addAll(cd.ingredientesSignificativos);
                ingredientesPorDia.computeIfAbsent(fechaActual, k -> new HashSet<>())
                        .addAll(cd.ingredientesSignificativos);
            }

            int personasToSet = (numPersonas > 0) ? numPersonas : receta.getNumPersonas();
            if (personasToSet <= 0) personasToSet = 2;

            // 🚀 Auto-seleccionar mejores sustitutos para rellenado automático
            Map<String, String> elegidos = new HashMap<>();
            Map<String, List<Ingrediente>> grupos = new HashMap<>();
            for (Ingrediente ing : receta.getIngredientes()) {
                String key = (ing.getEsSustitutoDe() != null && !ing.getEsSustitutoDe().isEmpty())
                        ? ing.getEsSustitutoDe() : ing.getNombre();
                grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(ing);
            }

            for (Map.Entry<String, List<Ingrediente>> entry : grupos.entrySet()) {
                entry.getValue().stream()
                        .max(Comparator.comparingDouble(Ingrediente::getPuntuacion)).ifPresent(mejor -> elegidos.put(entry.getKey(), mejor.getNombre()));
            }

            dia.getRecetas().add(new RecetaDia(recetaId, personasToSet, elegidos));
            break;
        }
    }

    private static boolean noEsBaseMediterranea(Ingrediente i) {
        if (i.getTipo() == TipoIngrediente.ACEITE || i.getTipo() == TipoIngrediente.ESPECIA) return false;
        String n = normalizarNombre(i.getNombre());
        return !n.contains("ajo") && !n.contains("cebolla") && !n.contains("tomate") && !n.contains("aceite") && !n.contains("sal") && !n.contains("pimienta");
    }

    private static String normalizarNombre(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static Receta obtenerRecetaNoRepetida(List<Receta> candidatosPreFiltrados,
                                                  Day dia,
                                                  MomentoReceta momentoRequerido,
                                                  WindowStats windowStats,
                                                  DailyStats dailyStats,
                                                  Set<String> ingredientesActivosDia,
                                                  int limiteDias,
                                                  Map<String, Date> fechasTemporales,
                                                  Map<String, CachedRecetaData> cacheData,
                                                  Map<LocalDate, Set<String>> ingredientesPorDia,
                                                  LocalDate fechaActual) {
        List<Receta> candidatos = new ArrayList<>();
        long epochDiaActual = fechaActual.toEpochDay();
        
        for (Receta receta : candidatosPreFiltrados) {
            CachedRecetaData cd = cacheData.get(receta.getId());
            if (cd == null) continue;

            // M8: Comprobar fecha con fechasTemporales (Optimizado con epochDay)
            Date fechaEf = fechasTemporales.getOrDefault(receta.getId(), receta.getFechaCalendario());
            if (fechaEf != null) {
                long epochFechaEf = fechaEf.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay();
                if (epochDiaActual - epochFechaEf < limiteDias) continue;
            }

            // M9: Ventana de frecuencia con Bitmasks (Velocidad O(1))
            if (windowStats != null) {
                if (windowStats.carneRoja >= 1 && (cd.tiposMask & WindowStats.MASK_CARNE_ROJA) != 0) continue;
                if (windowStats.legumbres >= 2 && cd.hasTipo(TipoIngrediente.LEGUMBRE)) continue;
                if (windowStats.pastaArroz >= 2 && (cd.tiposMask & WindowStats.MASK_PASTA_ARROZ) != 0) continue;
            }

            // M6: Similitud con ingredientes activos del día
            if (!ingredientesActivosDia.isEmpty()) {
                long coincidencias = cd.ingredientesSignificativos.stream()
                        .filter(ingredientesActivosDia::contains)
                        .count();
                if (coincidencias >= 2) continue;
            }

            // M21: Balance Nutricional Intradiario con Bitmasks
            if (cd.isPesada && !ingredientesActivosDia.isEmpty()) {
                boolean hayPesadoEnDia = dia.getRecetas().stream().anyMatch(rd -> {
                    CachedRecetaData ecd = cacheData.get(rd.getIdReceta());
                    return ecd != null && ecd.isPesada;
                });
                if (hayPesadoEnDia) continue;
            }
            
            // Regla específica: Pasta + Legumbre NO el mismo día (Bitmasks)
            if ((cd.hasAnyTipo(TipoIngrediente.PASTA.getMask() | TipoIngrediente.LEGUMBRE.getMask())) && !ingredientesActivosDia.isEmpty()) {
                boolean hayConflicto = dia.getRecetas().stream().anyMatch(rd -> {
                    CachedRecetaData ecd = cacheData.get(rd.getIdReceta());
                    return ecd != null && ecd.hasAnyTipo(TipoIngrediente.PASTA.getMask() | TipoIngrediente.LEGUMBRE.getMask());
                });
                if (hayConflicto) continue;
            }

            candidatos.add(receta);
        }

        if (candidatos.isEmpty()) return null;

        double totalWeight = 0.0;
        double[] weights = new double[candidatos.size()];
        for (int i = 0; i < candidatos.size(); i++) {
            Receta r = candidatos.get(i);
            CachedRecetaData cd = cacheData.get(r.getId());
            
            // Score base: Salud + Estrellas (Preponderado)
            assert cd != null;
            double score = cd.healthNorm;
            if (Math.abs(cd.healthNorm - 0.7) < 0.2) { 
                score += cd.starsNorm * 0.2;
            }

            // Modificadores de dieta mediterránea
            if (windowStats != null) {
                if (windowStats.legumbres < 2 && cd.hasTipo(TipoIngrediente.LEGUMBRE)) score *= 3.0;
                if (windowStats.pescado < 3 && (cd.tiposMask & WindowStats.MASK_PESCADO) != 0) score *= 3.0;
                if (windowStats.carneBlanca >= 3 && cd.hasTipo(TipoIngrediente.CARNE_BLANCA)) score *= 0.5;
                if (windowStats.huevos >= 2 && cd.hasTipo(TipoIngrediente.HUEVO)) score *= 0.5;
            }
            
            if (dailyStats != null && dailyStats.verduras < 1 && cd.hasTipo(TipoIngrediente.VERDURA)) {
                score *= 2.0;
            }

            // Ligereza de cena (Uso de pre-calculado isDensa)
            if (momentoRequerido == MomentoReceta.CENA) {
                if (cd.isDensa) score *= 0.75;
                else score *= 1.25;
            }
            
            // M19 Avanzado: Sinergia con decaimiento temporal (Aprovechar frescos recientes)
            for (int d = 1; d <= 7; d++) {
                Set<String> ingAnteriores = ingredientesPorDia.get(fechaActual.minusDays(d));
                if (ingAnteriores != null && !ingAnteriores.isEmpty()) {
                    long sinergias = cd.ingredientesSignificativos.stream()
                            .filter(ingAnteriores::contains)
                            .count();
                    if (sinergias > 0) {
                        // Decaimiento: 1.0 (ayer) -> 0.4 (hace una semana)
                        double factorTemporal = 1.0 - (d - 1) * 0.1;
                        double bonusBase = (cd.hasAnyTipo(TipoIngrediente.VERDURA.getMask() | TipoIngrediente.LACTEO.getMask())) ? 0.15 : 0.05;
                        score *= (1.0 + (Math.min(2, sinergias) * bonusBase * factorTemporal));
                    }
                }
            }

            double noise = 0.05 * RANDOM.nextDouble();
            double w = Math.max(0.1, score + noise);
            weights[i] = w;
            totalWeight += w;
        }

        double rnd = RANDOM.nextDouble() * totalWeight;
        double acc = 0.0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (rnd <= acc) return candidatos.get(i);
        }

        return candidatos.get(0);
    }

    // --- NUEVOS MÉTODOS PARA DIETA MEDITERRÁNEA ---

    private static class WindowStats {
        int pescado = 0;
        int carneBlanca = 0;
        int carneRoja = 0;
        int huevos = 0;
        int legumbres = 0;
        int pastaArroz = 0;

        // Máscaras de conveniencia
        static final long MASK_CARNE_ROJA = TipoIngrediente.CARNE_ROJA.getMask() | TipoIngrediente.CARNE_PROCESADA.getMask();
        static final long MASK_PESCADO = TipoIngrediente.PESCADO_BLANCO.getMask() | TipoIngrediente.PESCADO_AZUL.getMask() | TipoIngrediente.MARISCO.getMask();
        static final long MASK_PASTA_ARROZ = TipoIngrediente.PASTA.getMask() | TipoIngrediente.CEREAL.getMask();
    }

    private static class DailyStats {
        int verduras = 0;
    }

    private static java.time.LocalDate getLocalDate(int day, int month, int year) {
        return java.time.LocalDate.of(year, month + 1, day);
    }

    private static void subtractWindowStats(WindowStats stats, Receta r, Map<String, CachedRecetaData> cacheData) {
        if (stats == null || r == null) return;
        CachedRecetaData cd = cacheData.get(r.getId());
        if (cd == null) return;
        
        if ((cd.tiposMask & WindowStats.MASK_CARNE_ROJA) != 0) stats.carneRoja = Math.max(0, stats.carneRoja - 1);
        if (cd.hasTipo(TipoIngrediente.CARNE_BLANCA)) stats.carneBlanca = Math.max(0, stats.carneBlanca - 1);
        if ((cd.tiposMask & WindowStats.MASK_PESCADO) != 0) stats.pescado = Math.max(0, stats.pescado - 1);
        if (cd.hasTipo(TipoIngrediente.LEGUMBRE)) stats.legumbres = Math.max(0, stats.legumbres - 1);
        if (cd.hasTipo(TipoIngrediente.HUEVO)) stats.huevos = Math.max(0, stats.huevos - 1);
        if ((cd.tiposMask & WindowStats.MASK_PASTA_ARROZ) != 0) stats.pastaArroz = Math.max(0, stats.pastaArroz - 1);
    }

    private static WindowStats getWindowStats(java.time.LocalDate date, Map<java.time.LocalDate, Receta> asignaciones, Map<String, CachedRecetaData> cacheData) {
        WindowStats stats = new WindowStats();
        // Ventana de 7 días (el día actual y los 6 anteriores)
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate d = date.minusDays(i);
            Receta r = asignaciones.get(d);
            if (r != null) {
                updateWindowStats(stats, r, cacheData);
            }
        }
        return stats;
    }

    private static void updateWindowStats(WindowStats stats, Receta r, Map<String, CachedRecetaData> cacheData) {
        if (stats == null || r == null) return;
        CachedRecetaData cd = cacheData.get(r.getId());
        if (cd == null) return;

        if ((cd.tiposMask & WindowStats.MASK_CARNE_ROJA) != 0) stats.carneRoja += 1;
        if (cd.hasTipo(TipoIngrediente.CARNE_BLANCA)) stats.carneBlanca += 1;
        if ((cd.tiposMask & WindowStats.MASK_PESCADO) != 0) stats.pescado += 1;
        if (cd.hasTipo(TipoIngrediente.LEGUMBRE)) stats.legumbres += 1;
        if (cd.hasTipo(TipoIngrediente.HUEVO)) stats.huevos += 1;
        if ((cd.tiposMask & WindowStats.MASK_PASTA_ARROZ) != 0) stats.pastaArroz += 1;
    }

    private static DailyStats getDailyStats(java.time.LocalDate date, Map<java.time.LocalDate, List<Receta>> asignacionesDiarias) {
        DailyStats stats = new DailyStats();
        List<Receta> recetas = asignacionesDiarias.get(date);
        if (recetas != null) {
            for (Receta r : recetas) {
                if (containsIngredientType(r)) {
                    stats.verduras++;
                }
            }
        }
        return stats;
    }

    private static boolean containsIngredientType(Receta r) {
        if (r == null || r.getIngredientes() == null) return false;
        for (Ingrediente ing : r.getIngredientes()) {
            if (ing != null && ing.getTipo() == TipoIngrediente.VERDURA) return true;
        }
        return false;
    }


    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recientes, Day dia, int mes, int anio) {
        LocalDate hoy = getLocalDate(dia.getDayOfMonth(), mes, anio);
        recientes.removeIf(r -> {
            if (r.getFechaCalendario() == null) return false;
            LocalDate f = r.getFechaCalendario().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            return hoy.toEpochDay() - f.toEpochDay() >= LIMITE_DIAS;
        });
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
    public static void getListaCompra(Context context, int mes, int anio, int diaInicio, int diaFin,
                                      ListaCompraCallback callback) {
        if (!checkUserId(callback)) return;

        obtenerCalendario(context, mes, anio, new CalendarioCallback() {
            private boolean alreadyExecuted = false;

            @Override
            public void onSuccess(List<Day> calendario) {
                if (alreadyExecuted) return;
                alreadyExecuted = true;

                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> listaRecetas) {
                        Log.d(TAG, "🚀 Generando lista de compra para " + (diaFin - diaInicio + 1) + " días");
                        try {
                            RecetasSrv.inicializarMapas(context);
                            Map<String, ConcurrentHashMap<String, BigDecimal>> resultado = new ConcurrentHashMap<>();
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

    /**
     * Rellena un rango de días añadiendo recetas (no borra recetas existentes) generando un menu.
     */
    public static void addMenu(final Context context, final int mes, final int anio, final int diaInicio, final int diaFin,
                               final boolean forzarPasados, final int numRecetas, final int numPersonas, final RellenarCallback callback) {
        if (!checkUserId(callback)) return;

        CompletableFuture<List<Day>> calendarFuture = new CompletableFuture<>();
        obtenerCalendario(context, mes, anio, new CalendarioCallback() {
            @Override public void onSuccess(List<Day> result) { calendarFuture.complete(result); }
            @Override public void onFailure(Exception e) { calendarFuture.completeExceptionally(e); }
        });

        CompletableFuture<List<Receta>> recipesFuture = new CompletableFuture<>();
        RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
            @Override public void onSuccess(List<Receta> result) { recipesFuture.complete(result); }
            @Override public void onFailure(Exception e) { recipesFuture.completeExceptionally(e); }
        });

        // M16: Añadir timeout a las cargas
        CompletableFuture.allOf(calendarFuture, recipesFuture)
                .orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((v, ex) -> {
            if (ex != null) {
                Log.e(TAG, "Error o timeout en carga de datos para addMenu", ex);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onFailure((Exception) (ex.getCause() != null ? ex.getCause() : ex))
                    );
                }
                return;
            }

            try {
                List<Day> calendario = calendarFuture.get();
                List<Receta> recetasDisponibles = recipesFuture.get();

                // M14: Comprobar si el rango ya está completo
                boolean rangoCompleto = true;
                for (Day d : calendario) {
                    if (d.getDayOfMonth() >= diaInicio && d.getDayOfMonth() <= diaFin) {
                        if (d.getRecetas() == null || d.getRecetas().size() < numRecetas) {
                            rangoCompleto = false;
                            break;
                        }
                    }
                }
                if (rangoCompleto) {
                    Log.d(TAG, "Rango [" + diaInicio + "-" + diaFin + "] ya completo, omitiendo generación.");
                    if (callback != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(calendario));
                    }
                    return;
                }

                // 🚀 Ejecutar la generación en background para no bloquear el UI thread
                backgroundExecutor.execute(() -> {
                    try {
                        // Mapa para búsquedas rápidas de recetas por ID
                        Map<String, Receta> mapRecetas = recetasDisponibles.stream()
                                .collect(Collectors.toMap(Receta::getId, r -> r, (r1, r2) -> r1));

                        // M17: Obtener caché de pre-procesado desde RecetasSrv
                        Map<String, CachedRecetaData> cacheData = RecetasSrv.getRecipeProcessingCache();
                        
                        // Fallback si la caché está vacía por algún motivo
                        if (cacheData.isEmpty()) {
                            cacheData = recetasDisponibles.stream()
                                    .collect(Collectors.toMap(Receta::getId, CachedRecetaData::new));
                        }

                        // Usar la temporada del mes que se está rellenando
                        com.david.recetapp.negocio.beans.Temporada temporadaObjetivo = UtilsSrv.getTemporadaFecha(java.time.LocalDate.of(anio, mes + 1, 1));

                        // --- PREPARAR LISTAS DE RECETAS (M3: List en lugar de Queue) ---
                        
                        // Particionado por MomentoReceta para eficiencia
                        List<Receta> filtradasComida = new ArrayList<>();
                        List<Receta> filtradasCena = new ArrayList<>();
                        List<Receta> filtradasAmbos = new ArrayList<>();
                        List<Receta> filtradasSides = new ArrayList<>();

                        for (Receta r : recetasDisponibles) {
                            if (r.getTipoReceta() == TipoReceta.SIDE) {
                                if (r.getTemporadas().contains(temporadaObjetivo)) filtradasSides.add(r);
                                continue;
                            }
                            if (r.getTipoReceta() != TipoReceta.PRINCIPAL) continue;
                            if (!r.getTemporadas().contains(temporadaObjetivo)) continue;

                            MomentoReceta m = r.getMomentoReceta();
                            if (m == MomentoReceta.COMIDA) filtradasComida.add(r);
                            else if (m == MomentoReceta.CENA) filtradasCena.add(r);
                            else filtradasAmbos.add(r);
                        }

                        // Fallback si la temporada está vacía
                        if (filtradasComida.isEmpty() && filtradasCena.isEmpty() && filtradasAmbos.isEmpty()) {
                            for (Receta r : recetasDisponibles) {
                                if (r.getTipoReceta() == TipoReceta.SIDE) {
                                    filtradasSides.add(r);
                                    continue;
                                }
                                if (r.getTipoReceta() != TipoReceta.PRINCIPAL) continue;
                                MomentoReceta m = r.getMomentoReceta();
                                if (m == MomentoReceta.COMIDA) filtradasComida.add(r);
                                else if (m == MomentoReceta.CENA) filtradasCena.add(r);
                                else filtradasAmbos.add(r);
                            }
                        }

                        Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();
                        List<ActualizacionFecha> actualizacionesPendientes = new ArrayList<>();
                        Map<String, Date> fechasTemporales = new HashMap<>(); // M8: Proteger mutación

                        // Estructuras para seguimiento de estadísticas
                        Map<LocalDate, Receta> asignacionesPrincipales = new HashMap<>();
                        Map<LocalDate, List<Receta>> asignacionesDiarias = new HashMap<>();
                        
                        // M19: Rastreo de ingredientes en la ventana actual para sinergia
                        Map<LocalDate, Set<String>> ingredientesPorDia = new HashMap<>();

                        // M1: Pre-poblar estadísticas con recetas ya existentes en el calendario y ventana previa
                        LocalDate inicioRango = LocalDate.of(anio, mes + 1, diaInicio);
                        LocalDate limiteInferior = inicioRango.minusDays(LIMITE_DIAS);

                        // M13: Carga de ventana previa eficiente (pueden ser 1 o 2 meses)
                        List<Day> ventanaPrevia = getDiasEnRangoSync(limiteInferior, inicioRango.minusDays(1));
                        List<Day> todosLosDiasParaStats = new ArrayList<>(ventanaPrevia);
                        todosLosDiasParaStats.addAll(calendario);

                        for (Day diaParaStat : todosLosDiasParaStats) {
                            LocalDate fecha = getLocalDate(diaParaStat.getDayOfMonth(), diaParaStat.getMonth(), diaParaStat.getYear());
                            
                            // Si está en el rango de los últimos 14 días antes del inicio, poblar recientes
                            boolean enVentanaReciente = !fecha.isBefore(limiteInferior) && fecha.isBefore(inicioRango);

                            if (diaParaStat.getRecetas() != null && !diaParaStat.getRecetas().isEmpty()) {
                                List<Receta> recetasDelDia = new ArrayList<>();
                                for (int j = 0; j < diaParaStat.getRecetas().size(); j++) {
                                    Receta r = mapRecetas.get(diaParaStat.getRecetas().get(j).getIdReceta());
                                    if (r != null) {
                                        recetasDelDia.add(r);
                                        if (j == 0 || j == 1) { // COMIDA o CENA aproximado
                                            asignacionesPrincipales.put(fecha, r);
                                        }
                                        if (enVentanaReciente) {
                                            recetasUtilizadasRecientemente.add(r);
                                        }
                                        
                                        // Poblar ingredientes por día para sinergia
                                        CachedRecetaData cd = cacheData.get(r.getId());
                                        if (cd != null) {
                                            ingredientesPorDia.computeIfAbsent(fecha, k -> new HashSet<>())
                                                    .addAll(cd.ingredientesSignificativos);
                                        }
                                    }
                                }
                                asignacionesDiarias.put(fecha, recetasDelDia);
                            }
                        }

                        // M20: Estadísticas Incrementales (Ventana Deslizante)
                        // Inicializar ventana de estadísticas para el primer día del rango
                        WindowStats windowStats = getWindowStats(inicioRango, asignacionesPrincipales, cacheData);

                        for (Day dia : calendario) {
                            if (dia.getDayOfMonth() >= diaInicio && dia.getDayOfMonth() <= diaFin) {
                                // ForzarPasados solo aplica si es el mes actual
                                Calendar now = Calendar.getInstance();
                                boolean isCurrentMonth = (mes == now.get(Calendar.MONTH) && anio == now.get(Calendar.YEAR));
                                if (isCurrentMonth && !forzarPasados && esDiaAnteriorAlActual(dia)) continue;

                                // Actualizar ventana deslizante: restar el día que sale (hace 7 días) y sumar el que ya está (si hubiera)
                                LocalDate fechaActual = getLocalDate(dia.getDayOfMonth(), mes, anio);
                                subtractWindowStats(windowStats, asignacionesPrincipales.get(fechaActual.minusDays(7)), cacheData);

                                Set<String> ingredientesActivosDia = new HashSet<>(); // M6: Caché ingredientes

                                for (int i = 0; i < numRecetas; i++) {
                                    MomentoReceta momentoRequerido = null;
                                    List<Receta> listaAUsar;

                                    if (numRecetas >= 2) {
                                        if (i == 0) {
                                            momentoRequerido = MomentoReceta.COMIDA;
                                            listaAUsar = new ArrayList<>(filtradasComida);
                                            listaAUsar.addAll(filtradasAmbos);
                                        } else if (i == 1) {
                                            momentoRequerido = MomentoReceta.CENA;
                                            listaAUsar = new ArrayList<>(filtradasCena);
                                            listaAUsar.addAll(filtradasAmbos);
                                        } else {
                                            // A partir de la 3ª receta, acompañamientos
                                            listaAUsar = filtradasSides;
                                        }
                                    } else {
                                        // Caso genérico (un solo slot)
                                        listaAUsar = new ArrayList<>(filtradasComida);
                                        listaAUsar.addAll(filtradasCena);
                                        listaAUsar.addAll(filtradasAmbos);
                                    }
                                    
                                    addReceta(listaAUsar, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, 
                                            numPersonas, mes, anio, momentoRequerido, asignacionesPrincipales, 
                                            asignacionesDiarias, ingredientesActivosDia, fechasTemporales,
                                            cacheData, ingredientesPorDia, windowStats);
                                }

                                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia, mes, anio);
                            }
                        }

                        Map<String, Long> fechaMap = getStringLongMap(actualizacionesPendientes, mes, anio);

                        firebaseManager.guardarCalendarioConFechas(mes, anio, calendario, fechaMap, new FirebaseManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // M8: Solo ahora actualizamos los objetos en memoria
                                for (Map.Entry<String, Date> entry : fechasTemporales.entrySet()) {
                                    Receta r = mapRecetas.get(entry.getKey());
                                    if (r != null) r.setFechaCalendario(entry.getValue());
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Error guardando calendario+fechas en background", e);
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                        UtilsSrv.notificacion(context, context.getString(R.string.calendario_no_actualizado), android.widget.Toast.LENGTH_LONG).show()
                                );
                            }
                        });

                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(calendario));
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error en generación de menú mediterráneo", e);
                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure(e));
                        }
                    }
                });
            } catch (Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    @NonNull
    private static Map<String, Long> getStringLongMap(List<ActualizacionFecha> actualizacionesPendientes, int mes, int anio) {
        Map<String, Long> fechaMap = new HashMap<>();
        for (ActualizacionFecha af : actualizacionesPendientes) {
            Calendar cal = Calendar.getInstance();
            cal.set(anio, mes, af.diaMes, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            fechaMap.put(af.idReceta, cal.getTimeInMillis());
        }
        return fechaMap;
    }

    /**
     * Establece el userId activo. Si cambia de usuario, Firebase usará el nuevo filtro
     * automáticamente en todas las consultas siguientes.
     */
    public static void setUserId(String userId) {
        if (!Objects.equals(currentUserId, userId)) {
            Log.d(TAG, "🔑 Cambio de userId en CalendarioSrv detectado");
            currentUserId = userId;
        }
        firebaseManager.setUserId(userId);
    }

    // ——— HELPERS INTERNOS ———

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
     */
    public static void guardarDiaYRecetaBatch(int mes, int anio, final Day day, final String recetaId) {
        if (notHasUserId()) {
            Log.e(TAG, "❌ guardarDiaYRecetaBatch sin userId — operación ignorada");
            return;
        }

        List<Day> cached = obtenerCalendarioCache(mes, anio);
        if (cached != null) {
            List<Day> updated = mergeDayIntoList(cached, day);

            Map<String, Long> fechaMap = new HashMap<>();
            Calendar cal = Calendar.getInstance();
            cal.set(anio, mes, day.getDayOfMonth(), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date proposedDate = cal.getTime();
            Log.d(TAG, "guardarDiaYRecetaBatch: receta=" + recetaId + ", dia=" + day.getDayOfMonth() + ", proposedDate=" + proposedDate);

            Optional<Receta> opt = RecetasSrv.getRecetas().stream()
                    .filter(r -> r.getId().equals(recetaId))
                    .findAny();

            if (opt.isPresent()) {
                Receta r = opt.get();
                Date existingDate = r.getFechaCalendario();
                if (existingDate == null || proposedDate.after(existingDate)) {
                    Log.d(TAG, "guardarDiaYRecetaBatch: actualizando fecha. Existente=" + existingDate);
                    fechaMap.put(recetaId, proposedDate.getTime());
                } else {
                    Log.d(TAG, "guardarDiaYRecetaBatch: NO actualiza fecha. Existente=" + existingDate);
                }
            } else {
                Log.d(TAG, "guardarDiaYRecetaBatch: receta no en cache, forzando fecha");
                fechaMap.put(recetaId, proposedDate.getTime());
            }

            firebaseManager.guardarCalendarioConFechas(mes, anio, updated, fechaMap, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "guardarDiaYRecetaBatch: éxito para receta " + recetaId);
                    for (Map.Entry<String, Long> entry : fechaMap.entrySet()) {
                        Date d = (entry.getValue() == 0L) ? null : new Date(entry.getValue());
                        RecetasSrv.actualizarFechaRecetaEnCache(entry.getKey(), d);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "guardarDiaYRecetaBatch: fallo", e);
                }
            });
            return;
        }

        // Fallback: si no hay caché válida, actualizar día y receta por separado
        firebaseManager.actualizarDia(mes, anio, day, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Calendar cal = Calendar.getInstance();
                cal.set(anio, mes, day.getDayOfMonth(), 0, 0, 0);
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

    /**
     * Obtiene los días en un rango de fechas de forma síncrona (bloqueante).
     * Útil para llamadas desde hilos de fondo.
     */
    private static List<Day> getDiasEnRangoSync(LocalDate inicio, LocalDate fin) {
        List<Day> result = new ArrayList<>();
        if (inicio.isAfter(fin)) return result;

        // Agrupar por mes/año para minimizar llamadas
        Map<String, List<LocalDate>> porMes = new HashMap<>();
        LocalDate curr = inicio;
        while (!curr.isAfter(fin)) {
            String key = curr.getYear() + "-" + (curr.getMonthValue() - 1);
            porMes.computeIfAbsent(key, k -> new ArrayList<>()).add(curr);
            curr = curr.plusDays(1);
        }

        for (Map.Entry<String, List<LocalDate>> entry : porMes.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int anio = Integer.parseInt(parts[0]);
            int mes = Integer.parseInt(parts[1]);
            
            CompletableFuture<List<Day>> future = new CompletableFuture<>();
            firebaseManager.obtenerCalendario(mes, anio, null, new FirebaseManager.CalendarioCallback() {
                @Override public void onSuccess(List<Day> days) { future.complete(days); }
                @Override public void onFailure(Exception e) { future.complete(new ArrayList<>()); }
            });

            try {
                List<Day> days = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
                for (Day d : days) {
                    LocalDate dDate = getLocalDate(d.getDayOfMonth(), mes, anio);
                    if (!dDate.isBefore(inicio) && !dDate.isAfter(fin)) {
                        result.add(d);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo ventana previa para " + entry.getKey(), e);
            }
        }
        return result;
    }

    /**
     * M17: Estructura para optimizar el acceso a datos de recetas durante la generación.
     */
    public static class CachedRecetaData {
        final double puntuacionDada;
        final long tiposMask;
        final java.util.Set<String> ingredientesSignificativos = new java.util.HashSet<>();
        final double healthNorm;
        final double starsNorm;
        final boolean isDensa;
        final boolean isPesada;

        // Máscaras pre-calculadas para tipos frecuentes
        private static final long MASK_PESADA = 
                TipoIngrediente.CARNE_ROJA.getMask() | 
                TipoIngrediente.CARNE_PROCESADA.getMask() | 
                TipoIngrediente.PASTA.getMask() | 
                TipoIngrediente.LEGUMBRE.getMask();

        private static final long MASK_DENSA = 
                MASK_PESADA | 
                TipoIngrediente.CEREAL.getMask() | 
                TipoIngrediente.HARINA.getMask() | 
                TipoIngrediente.GRASA.getMask() | 
                TipoIngrediente.DULCE.getMask() | 
                TipoIngrediente.PROCESADO.getMask();

        CachedRecetaData(Receta r) {
            this.puntuacionDada = r.getPuntuacionDada();
            long mask = 0;
            if (r.getIngredientes() != null) {
                for (Ingrediente i : r.getIngredientes()) {
                    if (i.getTipo() != null) {
                        mask |= i.getTipo().getMask();
                    }
                    if (!i.isOpcional() && noEsBaseMediterranea(i)) {
                        ingredientesSignificativos.add(normalizarNombre(i.getNombre()));
                    }
                }
            }
            this.tiposMask = mask;
            
            double h = r.getPuntuacionDada();
            if (h > 10.0) h /= 100.0;
            else h /= 10.0;
            if (h <= 0) h = 0.5;
            this.healthNorm = h;
            
            double s = (r.getEstrellas() - 1.0) / 4.0;
            this.starsNorm = Math.max(0, s);

            // Pre-calcular flags de densidad
            this.isPesada = (mask & MASK_PESADA) != 0;
            
            boolean densaPorIngredientes = (mask & MASK_DENSA) != 0;
            if (h > 0.7) this.isDensa = false;
            else if (h > 0 && h < 0.4) this.isDensa = true;
            else this.isDensa = densaPorIngredientes;
        }

        public boolean hasTipo(TipoIngrediente t) {
            return (tiposMask & t.getMask()) != 0;
        }

        public boolean hasAnyTipo(long otherMask) {
            return (tiposMask & otherMask) != 0;
        }
    }
}
