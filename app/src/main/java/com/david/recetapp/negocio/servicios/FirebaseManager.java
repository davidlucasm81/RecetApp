package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import com.google.firebase.firestore.DocumentReference;

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
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(10);

    private final FirebaseFirestore db;
    private String userId;
    private ListenerRegistration recetasListener;
    private ListenerRegistration calendarioListener;

    // 🚀 Flag para saber si ya hicimos la primera carga desde servidor
    private boolean recetasCargadasDesdeServidor = false;

    public FirebaseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.userId = "default_user";
        configurarFirestore();
    }

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

        db.setFirestoreSettings(settings);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        invalidateAllCaches();
        detachListeners();
        recetasCargadasDesdeServidor = false;
    }

    private String getEffectiveUserId() {
        if (userId == null || userId.equals("default_user")) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
            }
        }
        return userId;
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
        recetasCache.remove(getEffectiveUserId());
    }

    private void invalidateCalendarioCache(int mes, int anio) {
        String cacheKey = getCalendarioCacheKey(mes, anio);
        calendarioCache.remove(cacheKey);
    }

    private void invalidateAllCaches() {
        recetasCache.clear();
        calendarioCache.clear();
    }

    private String getCalendarioCacheKey(int mes, int anio) {
        return getEffectiveUserId() + "_" + anio + "_" + mes;
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

    // ==================== OPERACIONES DE RECETAS OPTIMIZADAS ====================

    /**
     * 🚀 VERSIÓN ULTRA-RÁPIDA: Prioriza CACHE de Firestore, luego memoria, luego servidor
     */
    public void cargarRecetas(Context context, RecetasCallback callback) {
        // 1️⃣ Verificar caché en memoria primero (instantáneo)
        CacheEntry<List<Receta>> cached = recetasCache.get(getEffectiveUserId());
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "✅ Recetas desde caché memoria (0ms)");
            callback.onSuccess(new ArrayList<>(cached.data));
            return;
        }

        // 2️⃣ Intentar desde CACHE LOCAL de Firestore (muy rápido, ~50-100ms)
        cargarDesdeCache(context, callback);
    }

    /**
     * 🚀 Carga desde caché local de Firestore (OFFLINE FIRST)
     */
    private void cargarDesdeCache(Context context, RecetasCallback callback) {
        db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", getEffectiveUserId())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .get(Source.CACHE) // 🔥 CACHE PRIMERO
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "✅ Recetas desde caché Firestore (~50ms)");
                        List<Receta> recetas = procesarRecetas(queryDocumentSnapshots, context);
                        recetasCache.put(getEffectiveUserId(), new CacheEntry<>(recetas));

                        // 3️⃣ En background, sincronizar con servidor si nunca se ha hecho
                        if (!recetasCargadasDesdeServidor) {
                            Log.d(TAG, "🔄 Sincronizando con servidor para asegurar datos frescos...");
                            sincronizarConServidorEnBackground(context, callback);
                        } else {
                            // Si ya sincronizamos en esta sesión, podemos devolver caché sin miedo
                            callback.onSuccess(recetas);
                        }
                    } else {
                        // Caché vacía, ir directo a servidor
                        Log.d(TAG, "⚠️ Caché vacía, cargando desde servidor...");
                        cargarDesdeServidor(context, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla caché (primera vez), ir a servidor
                    Log.d(TAG, "⚠️ Error en caché, cargando desde servidor...");
                    cargarDesdeServidor(context, callback);
                });
    }

    /**
     * 🚀 Carga desde servidor (solo cuando es necesario)
     */
    private void cargarDesdeServidor(Context context, RecetasCallback callback) {
        db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", getEffectiveUserId())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .get(Source.SERVER) // Forzar servidor
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✅ Recetas desde servidor");
                    List<Receta> recetas = procesarRecetas(queryDocumentSnapshots, context);
                    recetasCache.put(getEffectiveUserId(), new CacheEntry<>(recetas));
                    recetasCargadasDesdeServidor = true;
                    callback.onSuccess(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando desde servidor", e);

                    // 🔥 Toast informativo al usuario
                    Toast.makeText(context,
                            "No se pudieron cargar las recetas. Revisa tu conexión.",
                            Toast.LENGTH_LONG
                    ).show();

                    callback.onFailure(e);
                });

    }

    /**
     * 🚀 Sincroniza con servidor en background
     */
    private void sincronizarConServidorEnBackground(Context context, RecetasCallback callback) {
        db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", getEffectiveUserId())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .get(Source.SERVER)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "🔄 Sincronización background completada");
                    List<Receta> recetas = procesarRecetas(queryDocumentSnapshots, context);
                    recetasCache.put(getEffectiveUserId(), new CacheEntry<>(recetas));
                    recetasCargadasDesdeServidor = true;
                    if (callback != null) callback.onSuccess(recetas);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Sincronización background falló", e);
                    if (callback != null) {
                        // Fallback a caché si el servidor falla durante la sincronización inicial
                        CacheEntry<List<Receta>> cached = recetasCache.get(getEffectiveUserId());
                        if (cached != null) {
                            Log.d(TAG, "⚠️ Sincronización falló, usando caché local como fallback");
                            callback.onSuccess(new ArrayList<>(cached.data));
                        } else {
                            callback.onFailure(e);
                        }
                    }
                });
    }

    private List<Receta> procesarRecetas(Iterable<QueryDocumentSnapshot> snapshots, Context context) {
        List<Receta> recetas = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshots) {
            try {
                Receta receta = document.toObject(Receta.class);
                receta.setPuntuacionDada(0); // ← fuerza recálculo siempre
                recetas.add(receta);
            } catch (Exception e) {
                Log.e(TAG, "Error parseando receta: " + document.getId(), e);
            }
        }
        return recetas;
    }

    /**
     * Carga recetas forzando servidor (para pull-to-refresh)
     */
    public void cargarRecetas(Context context, boolean forceServer, RecetasCallback callback) {
        if (!forceServer) {
            cargarRecetas(context, callback);
            return;
        }

        // Forzar servidor: invalidar caché primero
        invalidateRecetasCache();
        recetasCargadasDesdeServidor = false;
        cargarDesdeServidor(context, callback);
    }

    /**
     * 🚀 LISTENER EN TIEMPO REAL (opcional, para actualizaciones automáticas)
     */
    public void activarListenerRecetas(Context context, RecetasCallback callback) {
        if (recetasListener != null) {
            Log.d(TAG, "Listener ya activo");
            return;
        }

        recetasListener = db.collection(COLLECTION_RECETAS)
                .whereEqualTo("userId", getEffectiveUserId())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_RECETAS_POR_USUARIO)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error en listener", error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "🔄 Listener actualizó recetas");
                        List<Receta> recetas = procesarRecetas(snapshots, context);
                        recetasCache.put(getEffectiveUserId(), new CacheEntry<>(recetas));
                        callback.onSuccess(recetas);
                    }
                });
    }

    // ==================== RESTO DE MÉTODOS (sin cambios) ====================

    public void addReceta(Receta receta, SimpleCallback callback) {
        if (!validarReceta(receta)) {
            callback.onFailure(new Exception("Receta inválida"));
            return;
        }

        String effectiveUserId = getEffectiveUserId();
        receta.setUserId(effectiveUserId);
        Map<String, Object> recetaMap = recetaToMap(receta);

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

    public void editarReceta(Receta receta, SimpleCallback callback) {
        if (!validarReceta(receta)) {
            callback.onFailure(new Exception("Receta inválida"));
            return;
        }

        String effectiveUserId = getEffectiveUserId();
        receta.setUserId(effectiveUserId);
        Map<String, Object> recetaMap = recetaToMap(receta);

        db.collection(COLLECTION_RECETAS)
                .document(receta.getId())
                .set(recetaMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Receta editada: " + receta.getId());
                    invalidateRecetasCache();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error editando receta", e);
                    callback.onFailure(e);
                });
    }

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

    public void actualizarFechaCalendario(String recetaId, long timestamp, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        if (timestamp == 0) {
            updates.put("fechaCalendario", null);
        } else {
            updates.put("fechaCalendario", new Date(timestamp));
        }
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

    // Devuelve el calendario cacheado si está válido; null en caso contrario
    public List<Day> getCachedCalendarioIfValid(int mes, int anio) {
        String cacheKey = getCalendarioCacheKey(mes, anio);
        CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return new ArrayList<>(cached.data);
        }
        return null;
    }

    public void obtenerCalendario(int mes, int anio, Source source, CalendarioCallback callback) {
        String cacheKey = getCalendarioCacheKey(mes, anio);

        if (source == null) {
            // 1. Memoria
            CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                callback.onSuccess(new ArrayList<>(cached.data));
                refrescarCalendarioEnBackground(mes, anio, cacheKey, cached.data, callback);
                return;
            }

            // 2. Caché persistente de Firestore
            String calendarioId = getCalendarioId(mes, anio);
            DocumentReference docRef = db.collection(COLLECTION_CALENDARIO).document(calendarioId);

            docRef.get(Source.CACHE)
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            // Cache HIT: devolver inmediatamente y refrescar en background
                            List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get("days");
                            List<Day> daysFromCache = parseDays(mes, anio, list);
                            calendarioCache.put(cacheKey, new CacheEntry<>(daysFromCache));
                            callback.onSuccess(new ArrayList<>(daysFromCache));
                            refrescarCalendarioEnBackground(mes, anio, cacheKey, daysFromCache, callback);
                        } else {
                            // Cache MISS: ir al servidor, siempre llamar callback
                            cargarCalendarioDesdeServidor(mes, anio, docRef, cacheKey, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Error de caché: ir al servidor
                        String id = getCalendarioId(mes, anio);
                        cargarCalendarioDesdeServidor(mes, anio,
                                db.collection(COLLECTION_CALENDARIO).document(id), cacheKey, callback);
                    });
            return;
        }

        // Source explícito (CACHE o SERVER)
        String calendarioId = getCalendarioId(mes, anio);
        db.collection(COLLECTION_CALENDARIO).document(calendarioId)
                .get(source)
                .addOnSuccessListener(snapshot -> {
                    List<Day> days = new ArrayList<>();
                    if (snapshot.exists()) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get("days");
                        days = parseDays(mes, anio, list);
                        calendarioCache.put(cacheKey, new CacheEntry<>(days));
                    }
                    callback.onSuccess(days);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Siempre llama callback.onSuccess (o onFailure), sin condiciones. */
    private void cargarCalendarioDesdeServidor(int mes, int anio, DocumentReference docRef,
                                               String cacheKey,
                                               CalendarioCallback callback) {
        docRef.get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    List<Day> days = new ArrayList<>();
                    if (snapshot.exists()) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get("days");
                        days = parseDays(mes, anio, list);
                        calendarioCache.put(cacheKey, new CacheEntry<>(days));
                    }
                    callback.onSuccess(days); // ← siempre, aunque esté vacío
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Refresco silencioso desde servidor; solo notifica si los datos cambiaron. */
    private void refrescarCalendarioEnBackground(int mes, int anio, String cacheKey,
                                                 List<Day> daysActuales,
                                                 CalendarioCallback callback) {
        String calendarioId = getCalendarioId(mes, anio);
        db.collection(COLLECTION_CALENDARIO).document(calendarioId)
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get("days");
                        List<Day> serverDays = parseDays(mes, anio, list);
                        calendarioCache.put(cacheKey, new CacheEntry<>(serverDays));
                        if (!serverDays.equals(daysActuales)) {
                            callback.onSuccess(serverDays); // actualización real
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error refrescando calendario en background", e));
    }

    public void guardarCalendario(int mes, int anio, List<Day> days, SimpleCallback callback) {
        String calendarioId = getCalendarioId(mes, anio);
        String cacheKey = getCalendarioCacheKey(mes, anio);

        Map<String, Object> calendarioData = new HashMap<>();
        calendarioData.put("userId", getEffectiveUserId());
        calendarioData.put("mes", mes);
        calendarioData.put("anio", anio);
        calendarioData.put("days", daysToList(days));
        calendarioData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .set(calendarioData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Calendario guardado: " + calendarioId);
                    // Actualizar caché en memoria con el calendario guardado para evitar recargas innecesarias
                    calendarioCache.put(cacheKey, new CacheEntry<>(days));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando calendario", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Guardar el documento de calendario y, en la misma operación de WriteBatch,
     * actualizar la fechaCalendario de múltiples recetas. Esto reduce round-trips
     * y asegura mayor coherencia entre documentos.
     */
    public void guardarCalendarioConFechas(int mes, int anio, List<Day> days, Map<String, Long> recetaFechas, SimpleCallback callback) {
        String calendarioId = getCalendarioId(mes, anio);
        String cacheKey = getCalendarioCacheKey(mes, anio);

        WriteBatch batch = db.batch();

        DocumentReference calRef = db.collection(COLLECTION_CALENDARIO).document(calendarioId);
        Map<String, Object> calendarioData = new HashMap<>();
        calendarioData.put("userId", getEffectiveUserId());
        calendarioData.put("mes", mes);
        calendarioData.put("anio", anio);
        calendarioData.put("days", daysToList(days));
        calendarioData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(calRef, calendarioData, SetOptions.merge());

        if (recetaFechas != null) {
            for (Map.Entry<String, Long> entry : recetaFechas.entrySet()) {
                String recetaId = entry.getKey();
                Long ts = entry.getValue();
                DocumentReference recetaRef = db.collection(COLLECTION_RECETAS).document(recetaId);
                Map<String, Object> updates = new HashMap<>();
                if (ts == null || ts == 0L) {
                    updates.put("fechaCalendario", null);
                } else {
                    updates.put("fechaCalendario", new Date(ts));
                }
                updates.put("timestamp", FieldValue.serverTimestamp());
                batch.update(recetaRef, updates);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch: calendario y fechas de recetas guardadas");
                    // Actualizar caché del calendario
                    calendarioCache.put(cacheKey, new CacheEntry<>(days));

                    // Actualizar caché de recetas si está disponible
                    CacheEntry<List<Receta>> recetasCached = recetasCache.get(getEffectiveUserId());
                    if (recetasCached != null && recetasCached.isValid()) {
                        List<Receta> list = recetasCached.data;
                        if (recetaFechas != null) {
                            for (Receta r : list) {
                                if (recetaFechas.containsKey(r.getId())) {
                                    Long t = recetaFechas.get(r.getId());
                                    r.setFechaCalendario((t == null || t == 0L) ? null : new Date(t));
                                }
                            }
                            // Re-put cache entry to refresh timestamp
                            recetasCache.put(getEffectiveUserId(), new CacheEntry<>(list));
                        }
                    } else if (recetaFechas != null) {
                        // Si no hay caché válida, simplemente invalidar para forzar recarga
                        invalidateRecetasCache();
                    }

                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en batch calendario+fechas", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void actualizarDia(int mes, int anio, Day day, SimpleCallback callback) {
        String cacheKey = getCalendarioCacheKey(mes, anio);
        CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);

        if (cached != null && cached.isValid()) {
            List<Day> updatedDays = mergeDayIntoList(cached.data, day);
            guardarDiaYNotificar(mes, anio, updatedDays, cacheKey, callback);
        } else {
            // Sin caché: traer el calendario completo antes de actualizar
            String calendarioId = getCalendarioId(mes, anio);
            db.collection(COLLECTION_CALENDARIO)
                    .document(calendarioId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<Day> existingDays = new ArrayList<>();
                        if (documentSnapshot.exists()) {
                            List<Map<String, Object>> daysList =
                                    (List<Map<String, Object>>) documentSnapshot.get("days");
                            existingDays = parseDays(mes, anio, daysList);
                        }
                        List<Day> updatedDays = mergeDayIntoList(existingDays, day);
                        guardarDiaYNotificar(mes, anio, updatedDays, cacheKey, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error obteniendo calendario para actualizar día", e);
                        callback.onFailure(e);
                    });
        }
    }
    private void guardarDiaYNotificar(int mes, int anio, List<Day> days, String cacheKey, SimpleCallback callback) {
        String calendarioId = getCalendarioId(mes, anio);
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("days", daysToList(days));
        updateData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Caché actualizada DESPUÉS de confirmar Firebase
                    calendarioCache.put(cacheKey, new CacheEntry<>(days));
                    Log.d(TAG, "✅ Día guardado en Firebase y caché actualizada");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error guardando día", e);
                    callback.onFailure(e);
                });
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

    private void syncCalendarioInBackground(int mes, int anio, List<Day> days) {
        if (days == null || days.isEmpty()) return;

        String calendarioId = getCalendarioId(mes, anio);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("days", daysToList(days));
        updateData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CALENDARIO)
                .document(calendarioId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Calendario sincronizado en background"))
                .addOnFailureListener(e -> Log.e(TAG, "Error sincronizando calendario", e));
    }

    // Apply a Day update only in the in-memory cache so the UI can read the change immediately
    public void applyLocalDayUpdate(int mes, int anio, Day day) {
        String cacheKey = getCalendarioCacheKey(mes, anio);
        CacheEntry<List<Day>> cached = calendarioCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            List<Day> updated = mergeDayIntoList(cached.data, day);
            calendarioCache.put(cacheKey, new CacheEntry<>(updated));
            Log.d(TAG, "Aplicación local del día en caché: " + day.getDayOfMonth());
        }
    }

    // ==================== IMPORTACIÓN BATCH ====================

    public void importarRecetas(List<Receta> recetas, SimpleCallback callback) {
        if (recetas.isEmpty()) {
            callback.onSuccess();
            return;
        }

        importarRecetasBatch(recetas, 0, 500, callback);
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

                    if (endIndex < recetas.size()) {
                        importarRecetasBatch(recetas, endIndex, batchSize, callback);
                    } else {
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
        map.put("userId", getEffectiveUserId());
        map.put("nombre", receta.getNombre());
        map.put("temporadas", new ArrayList<>(receta.getTemporadas()));
        map.put("alergenos", new ArrayList<>(receta.getAlergenos()));
        map.put("ingredientes", receta.getIngredientes());
        map.put("pasos", receta.getPasos());
        map.put("estrellas", receta.getEstrellas());
        map.put("numPersonas", receta.getNumPersonas());
        map.put("shared", receta.isShared());
        map.put("tipoReceta", receta.getTipoReceta() != null ? receta.getTipoReceta().name() : TipoReceta.PRINCIPAL.name());
        // ← puntuacionDada eliminada: siempre se calcula localmente
        map.put("fechaCalendario", receta.getFechaCalendario());
        map.put("timestamp", FieldValue.serverTimestamp());

        return map;
    }

    public void eliminarCalendario(int mes, int anio, SimpleCallback callback) {
        String idActual = getCalendarioId(mes, anio);
        db.collection(COLLECTION_CALENDARIO).document(idActual)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Calendario eliminado: " + idActual);
                    String cacheKey = getCalendarioCacheKey(mes, anio);
                    calendarioCache.remove(cacheKey);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    private String getCalendarioId(int mes, int anio) {
        return getEffectiveUserId() + "_" + anio + "_" + mes;
    }

    // ==================== LIMPIEZA ====================

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

    private int toInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private RecetaDia mapToRecetaDia(Object obj) {
        if (obj == null) return null;
        if (obj instanceof RecetaDia) return (RecetaDia) obj;
        if (!(obj instanceof Map<?, ?> map)) return null;

        Object idObj = map.get("idReceta");
        if (idObj == null) idObj = map.get("id");
        if (idObj == null) idObj = map.get("recetaId");

        String idReceta = idObj != null ? idObj.toString() : null;

        Object numObj = map.get("numeroPersonas");
        if (numObj == null) numObj = map.get("numPersonas");
        if (numObj == null) numObj = map.get("numero_personas");

        int numeroPersonas = toInt(numObj, 1);

        if (idReceta == null) {
            return null;
        }

        return new RecetaDia(idReceta, numeroPersonas);
    }

    private List<Map<String, Object>> daysToList(List<Day> days) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (days == null) return list;
        for (Day day : days) {
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("dayOfMonth", day.getDayOfMonth());
            dayMap.put("month", day.getMonth());
            dayMap.put("year", day.getYear());

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

    private List<Day> parseDays(int mes, int anio, List<Map<String, Object>> daysList) {
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
                Day day = new Day(dayOfMonth, mes, anio, recetas);
                days.add(day);
            } catch (Exception e) {
                Log.e(TAG, "Error parseando día", e);
            }
        }
        return days;
    }

    public static void clearAllRecetasCache() {
        recetasCache.clear();
    }

    // Al lado de getCalendarioId()
    private String getCalendarioIdMesAnterior() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        int mes = calendar.get(Calendar.MONTH);
        int anio = calendar.get(Calendar.YEAR);
        return getEffectiveUserId() + "_" + anio + "_" + mes;
    }

    public void eliminarCalendarioMesAnterior(SimpleCallback callback) {
        String idAnterior = getCalendarioIdMesAnterior();
        db.collection(COLLECTION_CALENDARIO)
                .document(idAnterior)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "🗑️ Calendario mes anterior eliminado: " + idAnterior);
                    // Limpiar también su caché si existiera
                    calendarioCache.entrySet().removeIf(e -> e.getKey().contains(idAnterior));
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    // No es crítico — puede que simplemente no existiera
                    Log.w(TAG, "⚠️ No se encontró calendario anterior (normal si es primer uso)", e);
                    if (callback != null) callback.onSuccess();
                });
    }
}