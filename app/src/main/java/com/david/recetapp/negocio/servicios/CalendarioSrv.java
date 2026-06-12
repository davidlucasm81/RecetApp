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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    public static void actualizarFechaCalendario(Activity activity, String idReceta) {
        if (notHasUserId()) {
            Log.e(TAG, "❌ actualizarFechaCalendario sin userId — operación ignorada");
            return;
        }

        Calendar now = Calendar.getInstance();
        obtenerCalendario(activity, now.get(Calendar.MONTH), now.get(Calendar.YEAR), new CalendarioCallback() {
            private boolean alreadyExecuted = false;

            @Override
            public void onSuccess(List<Day> dias) {
                if (alreadyExecuted) return;
                alreadyExecuted = true;

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

    /**
         * 🚀 Clase auxiliar para batch updates
         */
        private record ActualizacionFecha(String idReceta, int diaMes) {
    }

    private static void addReceta(Queue<Receta> cola,
                                  Set<Receta> recetasUtilizadasRecientemente,
                                  Day dia,
                                  List<ActualizacionFecha> actualizacionesPendientes,
                                  int numPersonas,
                                  int mes,
                                  int anio,
                                  MomentoReceta momentoRequerido,
                                  WeeklyStats weeklyStats,
                                  Map<String, Receta> mapRecetas) {
        final int MAX_TRIES = 12;
        int tries = 0;
        Receta receta;

        while (tries < MAX_TRIES && !cola.isEmpty()) {
            receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia, mes, anio, momentoRequerido, weeklyStats, mapRecetas);
            tries++;
            if (receta == null) {
                // Si pedimos un momento específico y no hay, intentamos sin filtro de momento
                if (momentoRequerido != null) {
                    receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia, mes, anio, null, weeklyStats, mapRecetas);
                }
                if (receta == null) break;
            }

            final String recetaId = receta.getId();
            boolean yaEnDia = dia.getRecetas().stream()
                    .anyMatch(rd -> rd.getIdReceta().equals(recetaId));
            if (yaEnDia) {
                cola.offer(receta);
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.set(anio, mes, dia.getDayOfMonth(), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date proposedDate = cal.getTime();

            if (receta.getFechaCalendario() == null || proposedDate.after(receta.getFechaCalendario())) {
                receta.setFechaCalendario(proposedDate);
                actualizacionesPendientes.add(new ActualizacionFecha(recetaId, dia.getDayOfMonth()));
            }

            recetasUtilizadasRecientemente.add(receta);
            updateWeeklyStats(weeklyStats, receta);
            cola.offer(receta);

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

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola,
                                                  Set<Receta> recetasUtilizadasRecientemente,
                                                  Day dia,
                                                  int mes,
                                                  int anio,
                                                  MomentoReceta momentoRequerido,
                                                  WeeklyStats weeklyStats,
                                                  Map<String, Receta> mapRecetas) {
        List<Receta> candidatos = new ArrayList<>();
        for (Receta receta : cola) {
            if (recetasUtilizadasRecientemente.contains(receta)) continue;
            if (recetaRepetidaEnProximosDias(receta, dia, mes, anio)) continue;

            // Dieta mediterránea: Límite estricto de carne roja (max 1/semana)
            if (weeklyStats != null && weeklyStats.carneRoja >= 1) {
                if (containsIngredientType(receta, TipoIngrediente.CARNE_ROJA, TipoIngrediente.CARNE_PROCESADA)) {
                    continue;
                }
            }

            // Filtrado por momento si se requiere
            if (momentoRequerido != null) {
                MomentoReceta mr = receta.getMomentoReceta();
                if (mr != null && mr != MomentoReceta.AMBOS && mr != momentoRequerido) {
                    continue;
                }
            }

            boolean similar = false;
            if (dia.getRecetas() != null && !dia.getRecetas().isEmpty()) {
                for (RecetaDia rd : dia.getRecetas()) {
                    Receta existente = mapRecetas.get(rd.getIdReceta());
                    if (existente != null && recetasSimilares(receta, existente)) {
                        similar = true;
                        break;
                    }
                }
            }
            if (similar) continue;

            candidatos.add(receta);
        }

        if (candidatos.isEmpty()) return null;

        if (candidatos.size() == 1) {
            Receta r = candidatos.get(0);
            cola.remove(r);
            return r;
        }

        double totalWeight = 0.0;
        double[] weights = new double[candidatos.size()];
        for (int i = 0; i < candidatos.size(); i++) {
            Receta r = candidatos.get(i);
            
            // 🚀 NUEVA LÓGICA DE PESOS: Salud (puntuacionDada) + Gusto (estrellas)
            // puntuacionDada (Salud): Factor dominante
            double healthScore = 0.0;
            try { healthScore = r.getPuntuacionDada(); } catch (Exception ignored) {}
            if (Double.isNaN(healthScore) || healthScore <= 0.0) healthScore = 10.0; // Valor base si no tiene

            // Estrellas (Gusto): Factor secundario
            float stars = r.getEstrellas();
            if (stars <= 0) stars = 1.0f;

            // El peso final combina ambos, dando prioridad a la salud (puntuacionDada)
            // Multiplicamos la salud por un factor para que sea el motor principal
            double score = (healthScore * 1.5) + (stars * 10.0);

            // Dieta mediterránea: Priorizar legumbres y pescado si no se han alcanzado mínimos
            if (weeklyStats != null) {
                if (weeklyStats.legumbres < 2 && containsIngredientType(r, TipoIngrediente.LEGUMBRE)) {
                    score *= 3.0;
                }
                if (weeklyStats.pescado < 2 && containsIngredientType(r, TipoIngrediente.PESCADO_BLANCO, TipoIngrediente.PESCADO_AZUL, TipoIngrediente.MARISCO)) {
                    score *= 3.0;
                }
                
                // Penalizar exceso de carne blanca si ya hay suficiente
                if (weeklyStats.carneBlanca >= 2 && containsIngredientType(r, TipoIngrediente.CARNE_BLANCA)) {
                    score *= 0.5;
                }
                
                // Penalizar exceso de huevos si ya hay suficiente
                if (weeklyStats.huevos >= 2 && containsIngredientType(r, TipoIngrediente.HUEVO)) {
                    score *= 0.5;
                }
            }

            double noise = 0.1 * RANDOM.nextDouble();
            double w = score + noise;
            weights[i] = w;
            totalWeight += w;
        }

        double r = RANDOM.nextDouble() * totalWeight;
        double acc = 0.0;
        int selectedIdx = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc) { selectedIdx = i; break; }
        }

        Receta selected = candidatos.get(selectedIdx);
        cola.remove(selected);
        return selected;
    }

    // --- NUEVOS MÉTODOS PARA DIETA MEDITERRÁNEA ---

    private static class WeeklyStats {
        int pescado = 0;
        int carneBlanca = 0;
        int carneRoja = 0;
        int huevos = 0;
        int legumbres = 0;
    }

    private static int getWeekOfYear(int day, int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    private static boolean containsIngredientType(Receta r, TipoIngrediente... tipos) {
        if (r == null || r.getIngredientes() == null) return false;
        for (Ingrediente ing : r.getIngredientes()) {
            if (ing == null || ing.getTipo() == null) continue;
            for (TipoIngrediente t : tipos) {
                if (ing.getTipo() == t) return true;
            }
        }
        return false;
    }

    private static void updateWeeklyStats(WeeklyStats stats, Receta r) {
        if (stats == null || r == null) return;
        if (containsIngredientType(r, TipoIngrediente.CARNE_ROJA, TipoIngrediente.CARNE_PROCESADA)) {
            stats.carneRoja += 1;
        }
        if (containsIngredientType(r, TipoIngrediente.CARNE_BLANCA)) {
            stats.carneBlanca += 1;
        }
        if (containsIngredientType(r, TipoIngrediente.PESCADO_BLANCO, TipoIngrediente.PESCADO_AZUL, TipoIngrediente.MARISCO)) {
            stats.pescado += 1;
        }
        if (containsIngredientType(r, TipoIngrediente.LEGUMBRE)) {
            stats.legumbres += 1;
        }
        if (containsIngredientType(r, TipoIngrediente.HUEVO)) {
            stats.huevos += 1;
        }
    }

    /**
     * Determina si dos recetas son muy parecidas basándose en la intersección de ingredientes.
     */
    private static boolean recetasSimilares(Receta a, Receta b) {
        if (a == null || b == null) return false;
        List<String> listaA = new ArrayList<>();
        List<String> listaB = new ArrayList<>();

        if (a.getIngredientes() != null) {
            for (com.david.recetapp.negocio.beans.Ingrediente ing : a.getIngredientes()) {
                if (ing == null || ing.isOpcional()) continue;
                try { if (ing.getPuntuacion() < 0) continue; } catch (Exception ignored) {}
                if (ing.getNombre() != null && !ing.getNombre().trim().isEmpty())
                    listaA.add(ing.getNombre().toLowerCase(java.util.Locale.getDefault()).trim());
            }
        }
        if (b.getIngredientes() != null) {
            for (com.david.recetapp.negocio.beans.Ingrediente ing : b.getIngredientes()) {
                if (ing == null || ing.isOpcional()) continue;
                try { if (ing.getPuntuacion() < 0) continue; } catch (Exception ignored) {}
                if (ing.getNombre() != null && !ing.getNombre().trim().isEmpty())
                    listaB.add(ing.getNombre().toLowerCase(java.util.Locale.getDefault()).trim());
            }
        }

        if (listaA.isEmpty() || listaB.isEmpty()) return false;

        java.util.Set<String> setA = new java.util.HashSet<>(listaA);
        java.util.Set<String> setB = new java.util.HashSet<>(listaB);

        int intersection = 0;
        for (String s : setA) if (setB.contains(s)) intersection++;

        if (intersection >= 2) return true;

        // 🚀 MEJORA: Evitar repetir grupos de alimentos pesados el mismo día (Pasta, Arroz, Legumbres)
        if (containsIngredientType(a, TipoIngrediente.PASTA) && containsIngredientType(b, TipoIngrediente.PASTA)) return true;
        if (containsIngredientType(a, TipoIngrediente.CEREAL) && containsIngredientType(b, TipoIngrediente.CEREAL)) return true;
        if (containsIngredientType(a, TipoIngrediente.LEGUMBRE) && containsIngredientType(b, TipoIngrediente.LEGUMBRE)) return true;
        
        // 🚀 MEJORA: Evitar repetir grupos de proteínas el mismo día (Pescado, Carne Blanca)
        if (containsIngredientType(a, TipoIngrediente.PESCADO_BLANCO, TipoIngrediente.PESCADO_AZUL, TipoIngrediente.MARISCO) && 
            containsIngredientType(b, TipoIngrediente.PESCADO_BLANCO, TipoIngrediente.PESCADO_AZUL, TipoIngrediente.MARISCO)) return true;
            
        if (containsIngredientType(a, TipoIngrediente.CARNE_BLANCA) && containsIngredientType(b, TipoIngrediente.CARNE_BLANCA)) return true;

        int minSize = Math.min(Math.max(1, setA.size()), Math.max(1, setB.size()));
        double ratio = (double) intersection / (double) minSize;
        return ratio >= 0.5;
    }

    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recetasUtilizadasRecientemente,
                                                              Day day,
                                                              int mes,
                                                              int anio) {
        Set<Receta> recetasAEliminar = new HashSet<>();

        for (Receta receta : recetasUtilizadasRecientemente) {
            int diasDesdeUltimaUtilizacion = obtenerDiasDesdeUltimaUtilizacion(receta, day, mes, anio);
            if (diasDesdeUltimaUtilizacion >= LIMITE_DIAS) {
                recetasAEliminar.add(receta);
            }
        }

        recetasUtilizadasRecientemente.removeAll(recetasAEliminar);
    }

    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia, int mes, int anio) {
        Date fechaReceta = receta.getFechaCalendario();
        if (fechaReceta == null) return false;

        Calendar calDia = Calendar.getInstance();
        calDia.set(anio, mes, dia.getDayOfMonth(), 0, 0, 0);
        calDia.set(Calendar.MILLISECOND, 0);

        Calendar calReceta = Calendar.getInstance();
        calReceta.setTime(fechaReceta);
        calReceta.set(Calendar.HOUR_OF_DAY, 0);
        calReceta.set(Calendar.MINUTE, 0);
        calReceta.set(Calendar.SECOND, 0);
        calReceta.set(Calendar.MILLISECOND, 0);

        long diffMillis = calDia.getTimeInMillis() - calReceta.getTimeInMillis();
        long diffDays = diffMillis / (24L * 60L * 60L * 1000L);

        return diffDays >= 0 && diffDays <= LIMITE_DIAS;
    }

    private static int obtenerDiasDesdeUltimaUtilizacion(Receta receta, Day day, int mes, int anio) {
        Date fechaUltimaUtilizacion = receta.getFechaCalendario();

        if (fechaUltimaUtilizacion == null) return Integer.MAX_VALUE;

        Calendar calReceta = Calendar.getInstance();
        calReceta.setTime(fechaUltimaUtilizacion);
        calReceta.set(Calendar.HOUR_OF_DAY, 0);
        calReceta.set(Calendar.MINUTE, 0);
        calReceta.set(Calendar.SECOND, 0);
        calReceta.set(Calendar.MILLISECOND, 0);

        Calendar calObjetivo = Calendar.getInstance();
        calObjetivo.set(anio, mes, day.getDayOfMonth(), 0, 0, 0);
        calObjetivo.set(Calendar.MILLISECOND, 0);

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
     * Rellena un rango de días añadiendo recetas (no borra recetas existentes).
     */
    public static void rellenarRangoDias(final Context context, final int mes, final int anio, final int diaInicio, final int diaFin,
                                         final boolean forzarPasados, final int numRecetas, final int numPersonas, final RellenarCallback callback) {
        if (!checkUserId(callback)) return;

        obtenerCalendario(context, mes, anio, new CalendarioCallback() {
            private boolean alreadyExecuted = false;

            @Override
            public void onSuccess(List<Day> calendario) {
                if (alreadyExecuted) return;
                alreadyExecuted = true;

                RecetasSrv.cargarListaRecetas(context, new RecetasSrv.RecetasCallback() {
                    @Override
                    public void onSuccess(List<Receta> recetasDisponibles) {
                        // 🚀 Ejecutar la generación en background para no bloquear el UI thread
                        backgroundExecutor.execute(() -> {
                            try {
                                // Mapa para búsquedas rápidas de recetas por ID
                                Map<String, Receta> mapRecetas = recetasDisponibles.stream()
                                        .collect(Collectors.toMap(Receta::getId, r -> r, (r1, r2) -> r1));

                                // Usar la temporada del mes que se está rellenando
                                com.david.recetapp.negocio.beans.Temporada temporadaObjetivo = UtilsSrv.getTemporadaFecha(java.time.LocalDate.of(anio, mes + 1, 1));
                                List<Receta> filtradas = recetasDisponibles.stream()
                                        .filter(r -> r.getTipoReceta() == TipoReceta.PRINCIPAL && r.getTemporadas().contains(temporadaObjetivo))
                                        .toList();

                                List<Receta> soloPrincipales = recetasDisponibles.stream()
                                        .filter(r -> r.getTipoReceta() == TipoReceta.PRINCIPAL)
                                        .toList();

                                Queue<Receta> cola = new LinkedList<>(!filtradas.isEmpty() ? filtradas : soloPrincipales);
                                Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();
                                List<ActualizacionFecha> actualizacionesPendientes = new ArrayList<>();

                                // Estadísticas para dieta mediterránea
                                Map<Integer, WeeklyStats> statsPorSemana = new HashMap<>();

                                for (Day dia : calendario) {
                                    if (dia.getDayOfMonth() >= diaInicio && dia.getDayOfMonth() <= diaFin) {
                                        // ForzarPasados solo aplica si es el mes actual
                                        Calendar now = Calendar.getInstance();
                                        boolean isCurrentMonth = (mes == now.get(Calendar.MONTH) && anio == now.get(Calendar.YEAR));
                                        if (isCurrentMonth && !forzarPasados && esDiaAnteriorAlActual(dia)) continue;

                                        if (dia.getRecetas() == null) dia.setRecetas(new ArrayList<>());

                                        // Obtener o crear estadísticas de la semana
                                        int week = getWeekOfYear(dia.getDayOfMonth(), mes, anio);
                                        WeeklyStats weeklyStats = statsPorSemana.computeIfAbsent(week, k -> new WeeklyStats());

                                        for (int i = 0; i < numRecetas; i++) {
                                            MomentoReceta momentoRequerido = null;
                                            if (numRecetas >= 2) {
                                                if (i == 0) momentoRequerido = MomentoReceta.COMIDA;
                                                else if (i == 1) momentoRequerido = MomentoReceta.CENA;
                                            }
                                            addReceta(cola, recetasUtilizadasRecientemente, dia, actualizacionesPendientes, numPersonas, mes, anio, momentoRequerido, weeklyStats, mapRecetas);
                                        }

                                        limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia, mes, anio);
                                    }
                                }

                                Map<String, Long> fechaMap = getStringLongMap(actualizacionesPendientes, mes, anio);

                                firebaseManager.guardarCalendarioConFechas(mes, anio, calendario, fechaMap, new FirebaseManager.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // La caché de recetas y calendario ya se actualiza dentro de FirebaseManager.guardarCalendarioConFechas
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
}