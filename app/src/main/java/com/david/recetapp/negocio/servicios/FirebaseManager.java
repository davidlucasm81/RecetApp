package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String COLLECTION_RECETAS = "recetas";
    private static final String COLLECTION_CALENDARIO = "calendario";

    // Límites para prevenir abusos
    private static final int MAX_RECETAS_POR_USUARIO = 1000;
    private static final int MAX_INGREDIENTES = 50;
    private static final int MAX_PASOS = 50;

    // Caché en memoria
    private static final Map<String, CacheEntry<List<Receta>>> recetasCache = new HashMap<>();
    private static final Map<String, CacheEntry<List<Day>>> calendarioCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(10); // 10 minutos

    private final FirebaseFirestore db;
    private String userId;
    private ListenerRegistration recetasListener;
    private ListenerRegistration calendarioListener;

    public FirebaseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.userId = "default_user";
        configurarFirestore();
    }
// TODO: Usar cuando haya login por correo electronico
    public FirebaseManager(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        configurarFirestore();
    }

    /**
     * Configura Firestore para optimizar el uso de datos
     */
    private void configurarFirestore() {
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                                PersistentCacheSettings.newBuilder()
                                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                        .build()
                        )
                        .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        db.setFirestoreSettings(settings);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        // Limpiar caché al cambiar de usuario
        invalidateAllCaches();
        detachListeners();
    }

    // ==================== GESTIÓN DE CACHÉ ====================

    private static class CacheEntry<T> {
        final T data;
        final long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
    }

    private void invalidateRecetasCache() {
        recetasCache.remove(userId);
    }

    private void invalidateCalendarioCache() {
        String cacheKey = getCalendarioCacheKey();
        calendarioCache.remove(cacheKey);
    }

    private void invalidateAllCaches() {
        recetasCache.clear();
        calendarioCache.clear();
    }

    private String getCalendarioCacheKey() {
        Calendar calendar = Calendar.getInstance();
        return userId + "_" + calendar.get(Calendar.YEAR) + "_" + calendar.get(Calendar.MONTH);
    }

    // ==================== INTERFACES DE CALLBACKS ====================

    public interface RecetasCallback {
        void onSuccess(List<Receta> recetas);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface CalendarioCallback {
        void onSuccess(List<Day> days);
        void onFailure(Exception e);
    }

    // ==================== OPERACIONES DE RECETAS ====================

    /**
     * Carga recetas usando caché y listener en tiempo real (reducido a 1 llamada inicial)
     */
    public void cargarRecetas(Context context, RecetasCallback callback) {
        // Verificar caché primero
        CacheEntry<List<Receta>> cached = recetasCache.get(userId);
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "Usando recetas desde caché");
            callback.onSuccess(new ArrayList<>(cached.data));
            return;
        }

        // Si ya hay un listener activo, no crear otro
        if (recetasListener != null) {
            Log.d(TAG, "Listener de recetas ya activo");
        }

        // Usar Source.CACHE primero, luego servidor si es necesario
        db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetas = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Receta receta = document.toObject(Receta.class);
                        RecetasSrv.setPuntuacionDada(receta, context);
                        recetas.add(receta);
                    }

                    // Guardar en caché
                    recetasCache.put(userId, new CacheEntry<>(recetas));
                    callback.onSuccess(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando recetas", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Añade receta con validación y una sola escritura
     */
    public void addReceta(Receta receta, SimpleCallback callback) {
        // Validaciones locales antes de escribir
        if (!validarReceta(receta)) {
            callback.onFailure(new Exception("Receta inválida"));
            return;
        }

        Map<String, Object> recetaMap = recetaToMap(receta);

        // Usar set con merge para evitar sobrescritura accidental
        db.collection(COLLECTION_RECETAS)
                .document(receta.getId())
                .set(recetaMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Receta añadida: " + receta.getId());
                    invalidateRecetasCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error añadiendo receta", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Edita receta optimizado - solo actualiza campos modificados
     */
    public void editarReceta(Receta receta, SimpleCallback callback) {
        if (!validarReceta(receta)) {
            callback.onFailure(new Exception("Receta inválida"));
            return;
        }

        Map<String, Object> recetaMap = recetaToMap(receta);

        db.collection(COLLECTION_RECETAS)
                .document(receta.getId())
                .update(recetaMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Receta actualizada: " + receta.getId());
                    invalidateRecetasCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error actualizando receta", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Elimina receta
     */
    public void eliminarReceta(String recetaId, SimpleCallback callback) {
        db.collection(COLLECTION_RECETAS)
                .document(recetaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Receta eliminada: " + recetaId);
                    invalidateRecetasCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando receta", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualiza solo la fecha de calendario (operación atómica)
     */
    public void actualizarFechaCalendario(String recetaId, long timestamp, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fechaCalendario", new Date(timestamp));
        updates.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_RECETAS)
                .document(recetaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    invalidateRecetasCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }


    // ==================== OPERACIONES DE CALENDARIO ====================

    /**
     * Obtiene calendario con caché
     */
    public void obtenerCalendario(CalendarioCallback callback) {
        String cacheKey = getCalendarioCacheKey();
        CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);

        // 🧠 Si hay caché válida → NO tocar Firebase
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "Calendario servido desde memoria (NO Firebase)");
            callback.onSuccess(new ArrayList<>(cached.data));
            return;
        }

        Log.d(TAG, "Calendario no en caché → Firebase");

        String calendarioId = getCalendarioId();
        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Day> days;
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> daysList =
                                (List<Map<String, Object>>) documentSnapshot.get("days");
                        days = parseDays(daysList);
                    } else {
                        days = new ArrayList<>();
                    }

                    calendarioCache.put(cacheKey, new CacheEntry<>(days));
                    callback.onSuccess(days);
                })
                .addOnFailureListener(callback::onFailure);
    }


    /**
     * Guarda calendario completo
     */
    public void guardarCalendario(List<Day> days, SimpleCallback callback) {
        String calendarioId = getCalendarioId();

        Map<String, Object> calendarioData = new HashMap<>();
        calendarioData.put("userId", userId);
        calendarioData.put("mes", Calendar.getInstance().get(Calendar.MONTH));
        calendarioData.put("anio", Calendar.getInstance().get(Calendar.YEAR));
        calendarioData.put("days", daysToList(days));
        calendarioData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .set(calendarioData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Calendario guardado: " + calendarioId);
                    invalidateCalendarioCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando calendario", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Actualiza día específico usando array update (más eficiente)
     */
    public void actualizarDia(Day day, SimpleCallback callback) {
        String cacheKey = getCalendarioCacheKey();
        CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);

        List<Day> updatedDays;

        if (cached != null && cached.isValid()) {
            // 1️⃣ Actualizar caché inmediatamente
            updatedDays = mergeDayIntoList(cached.data, day);
        } else {
            updatedDays = new ArrayList<>();
            updatedDays.add(day);
        }

        // Guardar en caché YA (UX instantánea)
        calendarioCache.put(cacheKey, new CacheEntry<>(updatedDays));

        // Devolver éxito inmediato
        callback.onSuccess();

        // 2️⃣ Sincronizar con Firebase en segundo plano
        syncCalendarioInBackground(updatedDays);
    }
    private List<Day> mergeDayIntoList(List<Day> existing, Day newDay) {
        List<Day> result = new ArrayList<>(existing);
        boolean found = false;

        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getDayOfMonth() == newDay.getDayOfMonth()) {
                result.set(i, newDay);
                found = true;
                break;
            }
        }

        if (!found) {
            result.add(newDay);
        }

        return result;
    }

    private void syncCalendarioInBackground(List<Day> days) {
        String calendarioId = getCalendarioId();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("days", daysToList(days));
        updateData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Calendario sincronizado en background"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error sincronizando calendario en background", e));
    }


    // ==================== IMPORTACIÓN BATCH ====================

    /**
     * Importa recetas en lotes de 500 (límite de Firestore)
     */
    public void importarRecetas(List<Receta> recetas, SimpleCallback callback) {
        if (recetas.isEmpty()) {
            callback.onSuccess();
            return;
        }

        // Dividir en lotes de 500 (límite de WriteBatch)
        int batchSize = 500;

        importarRecetasBatch(recetas, 0, batchSize, callback);
    }

    private void importarRecetasBatch(List<Receta> recetas, int startIndex, int batchSize,
                                      SimpleCallback callback) {
        int endIndex = Math.min(startIndex + batchSize, recetas.size());
        List<Receta> batch = recetas.subList(startIndex, endIndex);

        WriteBatch writeBatch = db.batch();

        for (Receta receta : batch) {
            if (validarReceta(receta)) {
                Map<String, Object> recetaMap = recetaToMap(receta);
                writeBatch.set(
                        db.collection(COLLECTION_RECETAS).document(receta.getId()),
                        recetaMap
                );
            }
        }

        writeBatch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch importado: " + batch.size() + " recetas");

                    // Si hay más lotes, continuar
                    if (endIndex < recetas.size()) {
                        importarRecetasBatch(recetas, endIndex, batchSize, callback);
                    } else {
                        // Terminado
                        invalidateRecetasCache();
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en batch import", e);
                    callback.onFailure(e);
                });
    }

    // ==================== VALIDACIONES ====================

    private boolean validarReceta(Receta receta) {
        if (receta == null || receta.getId() == null || receta.getNombre() == null) {
            return false;
        }

        if (receta.getIngredientes() != null && receta.getIngredientes().size() > MAX_INGREDIENTES) {
            Log.w(TAG, "Receta excede máximo de ingredientes");
            return false;
        }

        if (receta.getPasos() != null && receta.getPasos().size() > MAX_PASOS) {
            Log.w(TAG, "Receta excede máximo de pasos");
            return false;
        }

        return true;
    }

    // ==================== CONVERSIONES ====================

    private Map<String, Object> recetaToMap(Receta receta) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", receta.getId());
        map.put("userId", userId);
        map.put("nombre", receta.getNombre());
        map.put("temporadas", new ArrayList<>(receta.getTemporadas()));
        map.put("alergenos", new ArrayList<>(receta.getAlergenos()));
        map.put("ingredientes", receta.getIngredientes());
        map.put("pasos", receta.getPasos());
        map.put("estrellas", receta.getEstrellas());
        map.put("numPersonas", receta.getNumPersonas());
        map.put("shared", receta.isShared());
        map.put("postre", receta.isPostre());
        map.put("puntuacionDada", receta.getPuntuacionDada());
        map.put("fechaCalendario", receta.getFechaCalendario());
        map.put("timestamp", FieldValue.serverTimestamp());

        return map;
    }

    private String getCalendarioId() {
        Calendar calendar = Calendar.getInstance();
        int mes = calendar.get(Calendar.MONTH);
        int anio = calendar.get(Calendar.YEAR);
        return userId + "_" + anio + "_" + mes;
    }

    // ==================== LIMPIEZA ====================

    /**
     * Detach listeners para evitar fugas de memoria
     */
    public void detachListeners() {
        if (recetasListener != null) {
            recetasListener.remove();
            recetasListener = null;
        }
        if (calendarioListener != null) {
            calendarioListener.remove();
            calendarioListener = null;
        }
    }

    // Helper: convierte un objeto numérico a int de forma segura
    private int toInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Convierte un Map (o cualquier objeto) a RecetaDia de forma robusta
    private RecetaDia mapToRecetaDia(Object obj) {
        if (obj == null) return null;
        if (obj instanceof RecetaDia) return (RecetaDia) obj; // ya es instancia
        if (!(obj instanceof Map<?, ?> map)) return null;

        // Priorizar varias keys que podrían aparecer (compatibilidad)
        Object idObj = map.get("idReceta");
        if (idObj == null) idObj = map.get("id");
        if (idObj == null) idObj = map.get("recetaId");

        String idReceta = idObj != null ? idObj.toString() : null;

        // numeroPersonas puede guardarse con distintos nombres
        Object numObj = map.get("numeroPersonas");
        if (numObj == null) numObj = map.get("numPersonas");
        if (numObj == null) numObj = map.get("numero_personas");

        int numeroPersonas = toInt(numObj, 1);

        if (idReceta == null) {
            // Si no hay id, devolvemos null para ignorarlo (o puedes crear con id vacío)
            return null;
        }

        return new RecetaDia(idReceta, numeroPersonas);
    }

    // Convierte lista de RecetaDia (modelo) a lista de Map para guardar en Firestore
    private List<Map<String, Object>> daysToList(List<Day> days) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (days == null) return list;
        for (Day day : days) {
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("dayOfMonth", day.getDayOfMonth());

            List<Map<String, Object>> recetasAsMaps = getMaps(day);
            dayMap.put("recetas", recetasAsMaps);
            list.add(dayMap);
        }
        return list;
    }

    @NonNull
    private static List<Map<String, Object>> getMaps(Day day) {
        List<Map<String, Object>> recetasAsMaps = new ArrayList<>();
        List<RecetaDia> recetas = day.getRecetas();
        if (recetas != null) {
            for (RecetaDia rd : recetas) {
                if (rd == null) continue;
                Map<String, Object> rm = new HashMap<>();
                rm.put("idReceta", rd.getIdReceta());
                rm.put("numeroPersonas", rd.getNumeroPersonas());
                recetasAsMaps.add(rm);
            }
        }
        return recetasAsMaps;
    }

    // Parse days (desde Firestore -> List<Day>) convirtiendo cada receta Map -> RecetaDia
    private List<Day> parseDays(List<Map<String, Object>> daysList) {
        List<Day> days = new ArrayList<>();
        if (daysList == null) return days;

        for (Map<String, Object> dayMap : daysList) {
            try {
                Object dayOfMonthObj = dayMap.get("dayOfMonth");
                int dayOfMonth = toInt(dayOfMonthObj, -1);
                if (dayOfMonth < 1) continue;

                List<RecetaDia> recetas = new ArrayList<>();
                Object recetasObj = dayMap.get("recetas");
                if (recetasObj instanceof Collection<?> col) {
                    for (Object entry : col) {
                        RecetaDia rd = mapToRecetaDia(entry);
                        if (rd != null) recetas.add(rd);
                    }
                } else if (recetasObj instanceof Object[] arr) {
                    for (Object entry : arr) {
                        RecetaDia rd = mapToRecetaDia(entry);
                        if (rd != null) recetas.add(rd);
                    }
                }
                Day day = new Day(dayOfMonth, recetas);
                days.add(day);
            } catch (Exception e) {
                Log.e(TAG, "Error parseando día (ignorado) ", e);
            }
        }
        return days;
    }

    // en FirebaseManager (p. ej. al final de la clase)
    public static void clearAllRecetasCache() {
        recetasCache.clear();
    }

    /**
     * Carga recetas y permite forzar la lectura desde servidor (ignora caché)
     */
    public void cargarRecetas(Context context, boolean forceServer, RecetasCallback callback) {
        if (!forceServer) {
            // Usar la versión normal con caché
            cargarRecetas(context, callback);
            return;
        }

        // Forzar servidor: invalidar caché primero
        invalidateRecetasCache();

        db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .get(Source.SERVER) // 🔹 lectura directa desde servidor
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Receta> recetas = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Receta receta = document.toObject(Receta.class);
                        RecetasSrv.setPuntuacionDada(receta, context);
                        recetas.add(receta);
                    }

                    // Guardar en caché para próximas llamadas
                    recetasCache.put(userId, new CacheEntry<>(recetas));
                    callback.onSuccess(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando recetas desde servidor", e);
                    callback.onFailure(e);
                });
    }



}