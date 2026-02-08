package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.beans.Temporada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 🚀 Servicio de recetas OPTIMIZADO con carga lazy y caché
 */
public class RecetasSrv {
    private static final String TAG = "RecetasSrv";

    private static final FirebaseManager firebaseManager = new FirebaseManager();

    // Cache en memoria
    private static final List<Receta> recetasCache = new ArrayList<>();

    // 🚀 Caché de mapas de ingredientes (se crean UNA SOLA VEZ)
    private static Map<String, Integer> ingredientMapCache = null;
    private static Map<String, Integer> unitImportanceMapCache = null;
    private static final Object cacheLock = new Object();

    private static final Pattern patternIngredient = Pattern.compile("^(.+)\\s(-?\\d+)$");

    // 🚀 ExecutorService para procesamiento en background
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    // ——— Interfaces de callbacks ———
    public interface RecetasCallback {
        void onSuccess(List<Receta> recetas);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ——— GETTER DE RECETAS ———
    public static List<Receta> getRecetas() {
        return new ArrayList<>(recetasCache);
    }

    // ——— CARGA DESDE FIREBASE OPTIMIZADA ———

    /**
     * 🚀 VERSIÓN RÁPIDA: No calcula puntuaciones hasta que sea necesario
     */
    public static void cargarListaRecetas(Context context, RecetasCallback callback) {
        if (!recetasCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(recetasCache));
            return;
        }

        // Inicializar mapas de caché en background mientras se cargan recetas
        inicializarCachesEnBackground(context);

        firebaseManager.cargarRecetas(context, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                recetasCache.clear();
                recetasCache.addAll(recetas);

                Log.d(TAG, "✅ Recetas cargadas: " + recetas.size());

                // ✅ Devolver INMEDIATAMENTE sin calcular puntuaciones
                callback.onSuccess(new ArrayList<>(recetasCache));

                // 🚀 Calcular puntuaciones en BACKGROUND (no bloquea UI)
                calcularPuntuacionesEnBackground(context);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas desde Firebase", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * 🚀 Versión con opción de forzar servidor
     */
    public static void cargarListaRecetas(Context context, boolean forceServer, RecetasCallback callback) {
        if (!forceServer && !recetasCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(recetasCache));
            return;
        }

        inicializarCachesEnBackground(context);

        firebaseManager.cargarRecetas(context, forceServer, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                recetasCache.clear();
                recetasCache.addAll(recetas);

                Log.d(TAG, "✅ Recetas cargadas (forceServer=" + forceServer + "): " + recetas.size());

                callback.onSuccess(new ArrayList<>(recetasCache));

                calcularPuntuacionesEnBackground(context);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas (forceServer=" + forceServer + ")", e);
                callback.onFailure(e);
            }
        });
    }

    // ——— CÁLCULO LAZY DE PUNTUACIONES ———

    /**
     * 🚀 Inicializa caché de mapas en background (se hace UNA SOLA VEZ)
     */
    private static void inicializarCachesEnBackground(Context context) {
        synchronized (cacheLock) {
            if (ingredientMapCache != null && unitImportanceMapCache != null) {
                return; // Ya están inicializados
            }
        }

        backgroundExecutor.execute(() -> {
            try {
                inicializarMapas(context);
                Log.d(TAG, "🔄 Mapas de ingredientes inicializados");
            } catch (Exception e) {
                Log.e(TAG, "Error inicializando mapas", e);
            }
        });
    }

    /**
     * 🚀 Calcula puntuaciones de TODAS las recetas en background
     */
    private static void calcularPuntuacionesEnBackground(Context context) {
        backgroundExecutor.execute(() -> {
            try {
                // Esperar a que los mapas estén listos
                inicializarMapas(context);

                long inicio = System.currentTimeMillis();

                synchronized (recetasCache) {
                    for (Receta receta : recetasCache) {
                        calcularPuntuacion(receta);
                    }
                }

                long duracion = System.currentTimeMillis() - inicio;
                Log.d(TAG, "✅ Puntuaciones calculadas en background: " + duracion + "ms");

            } catch (Exception e) {
                Log.e(TAG, "Error calculando puntuaciones en background", e);
            }
        });
    }

    /**
     * 🚀 Inicializa mapas de ingredientes y unidades (SOLO UNA VEZ)
     */
    private static void inicializarMapas(Context context) {
        synchronized (cacheLock) {
            if (ingredientMapCache != null && unitImportanceMapCache != null) {
                return; // Ya inicializados
            }

            // Crear mapa de ingredientes
            ingredientMapCache = new HashMap<>();
            String[] ingredientList = context.getResources().getStringArray(R.array.ingredient_list);
            for (String s : ingredientList) {
                Matcher m = patternIngredient.matcher(s.trim());
                if (m.matches()) {
                    String nombre = Objects.requireNonNull(m.group(1)).toLowerCase(Locale.getDefault());
                    int puntuacion = Integer.parseInt(m.group(2));
                    ingredientMapCache.put(nombre, puntuacion);
                }
            }

            // Crear mapa de unidades
            unitImportanceMapCache = new HashMap<>();
            String[] units = context.getResources().getStringArray(R.array.quantity_units);
            int[] importanceValues = context.getResources().getIntArray(R.array.importance_values);
            for (int i = 0; i < units.length; i++) {
                unitImportanceMapCache.put(units[i], importanceValues[i]);
            }
        }
    }

    /**
     * 🚀 Calcula puntuación de UNA receta (versión optimizada)
     */
    private static void calcularPuntuacion(Receta receta) {
        if (ingredientMapCache == null || unitImportanceMapCache == null) {
            return; // Mapas no inicializados todavía
        }

        // Asignar puntuaciones a ingredientes
        for (Ingrediente ing : receta.getIngredientes()) {
            String nombre = ing.getNombre().toLowerCase(Locale.getDefault());
            ing.setPuntuacion(ingredientMapCache.getOrDefault(nombre, -2));
        }

        // Calcular cantidad total
        double cantidadTotal = 0;
        for (Ingrediente ing : receta.getIngredientes()) {
            if (ing.getPuntuacion() >= 0) {
                double cantidad = UtilsSrv.convertirNumero(ing.getCantidad());
                int importancia = unitImportanceMapCache.getOrDefault(ing.getTipoCantidad(), 1);
                cantidadTotal += cantidad * importancia;
            }
        }

        if (cantidadTotal == 0) {
            receta.setPuntuacionDada(0);
            return;
        }

        // Calcular puntuación ponderada
        double puntuacionTotal = 0;
        for (Ingrediente ing : receta.getIngredientes()) {
            if (ing.getPuntuacion() >= 0) {
                double cantidad = UtilsSrv.convertirNumero(ing.getCantidad());
                int importancia = unitImportanceMapCache.getOrDefault(ing.getTipoCantidad(), 1);
                double peso = (cantidad * importancia) / cantidadTotal;
                puntuacionTotal += ing.getPuntuacion() * peso;
            }
        }

        receta.setPuntuacionDada(puntuacionTotal);
    }

    /**
     * 🚀 Método público para calcular puntuación ON-DEMAND cuando se necesite
     */
    public static void setPuntuacionDada(Receta receta, Context context) {
        // Si mapas no listos, inicializar pero sin bloquear el UI: lanzamos en background y retornamos (o bloquear poco)
        if (ingredientMapCache == null || unitImportanceMapCache == null) {
            inicializarCachesEnBackground(context);
            // opcionalmente: wait por un corto periodo o dejar que la puntuación se calcule cuando los mapas estén listos
            return;
        }

        calcularPuntuacion(receta);
    }


    /**
     * 🚀 Asegura que las puntuaciones estén calculadas (para listas que las necesiten)
     */
    public static void asegurarPuntuacionesCalculadas(Context context, RecetasCallback callback) {
        if (ingredientMapCache == null || unitImportanceMapCache == null) {
            inicializarMapas(context);
        }

        backgroundExecutor.execute(() -> {
            synchronized (recetasCache) {
                for (Receta receta : recetasCache) {
                    if (receta.getPuntuacionDada() == 0) { // No calculada
                        calcularPuntuacion(receta);
                    }
                }
            }

            // Devolver en hilo principal
            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(new ArrayList<>(recetasCache)));
        });

    }

    // ——— OPERACIONES SOBRE RECETAS ———

    public static void addReceta(Receta receta, SimpleCallback callback) {
        firebaseManager.addReceta(receta, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                recetasCache.add(receta);
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error añadiendo receta", e);
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void editarReceta(Receta receta, SimpleCallback callback) {
        firebaseManager.editarReceta(receta, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                for (int i = 0; i < recetasCache.size(); i++) {
                    if (recetasCache.get(i).getId().equals(receta.getId())) {
                        recetasCache.set(i, receta);
                        break;
                    }
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error editando receta", e);
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void eliminarReceta(int position, List<Receta> listaParcial, SimpleCallback callback) {
        Receta receta = listaParcial.remove(position);

        firebaseManager.eliminarReceta(receta.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                recetasCache.removeIf(r -> r.getId().equals(receta.getId()));
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                listaParcial.add(position, receta);
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    // ——— RECETAS CALENDARIO ———

    public static void cargarListaRecetasCalendario(Context context, List<RecetaDia> idRecetas, RecetasCallback callback) {
        cargarListaRecetas(context, new RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> todas) {
                Temporada temporada = UtilsSrv.getTemporadaFecha(LocalDate.now());
                Set<String> seleccionadas = idRecetas.stream()
                        .map(RecetaDia::getIdReceta)
                        .collect(Collectors.toSet());

                List<Receta> filtradas = todas.stream()
                        .filter(r -> !seleccionadas.contains(r.getId()) && r.getTemporadas().contains(temporada))
                        .sorted(Comparator.comparing(Receta::getPuntuacionDada, Comparator.reverseOrder())
                                .thenComparing(Receta::getEstrellas, Comparator.reverseOrder()))
                        .toList();

                callback.onSuccess(new ArrayList<>(filtradas));
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public static void actualizarRecetaCalendario(Context context, String idReceta, int diaMes, boolean add) {
        cargarListaRecetas(context, new RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                Optional<Receta> opt = recetas.stream()
                        .filter(r -> r.getId().equals(idReceta))
                        .findAny();

                opt.ifPresent(r -> {
                    long timestamp = 0;
                    if (diaMes > 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.DAY_OF_MONTH, diaMes);
                        Date fecha = cal.getTime();
                        if (add && r.getFechaCalendario() != null && r.getFechaCalendario().after(fecha)) return;
                        timestamp = fecha.getTime();
                    }

                    firebaseManager.actualizarFechaCalendario(idReceta, timestamp, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Fecha calendario actualizada para: " + idReceta);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error actualizando fecha calendario", e);
                        }
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas para actualizar calendario", e);
            }
        });
    }

    @NonNull
    public static List<Receta> getRecetasAdaptadasCalendario(List<Receta> recetasTotales, Day selectedDay) {
        Set<String> ids = selectedDay.getRecetas().stream()
                .map(RecetaDia::getIdReceta)
                .collect(Collectors.toSet());

        List<Receta> lista = recetasTotales.stream()
                .filter(r -> ids.contains(r.getId()))
                .collect(Collectors.toList());

        Map<String, Integer> personasMap = selectedDay.getRecetas().stream()
                .collect(Collectors.toMap(RecetaDia::getIdReceta, RecetaDia::getNumeroPersonas));

        for (Receta r : lista) {
            double orig = r.getNumPersonas();
            double obj = personasMap.getOrDefault(r.getId(), (int) orig);
            r.setNumPersonas((int) obj);
            double factor = obj / orig;

            for (Ingrediente ing : r.getIngredientes()) {
                double base = UtilsSrv.convertirNumero(ing.getCantidad());
                double escalada = base * factor;
                BigDecimal bd = BigDecimal.valueOf(escalada).setScale(2, RoundingMode.HALF_UP);
                ing.setCantidad(bd.stripTrailingZeros().toPlainString());
            }
        }

        return lista;
    }

    // ——— MÉTODOS AUXILIARES ———

    public static void setUserId(String userId) {
        firebaseManager.setUserId(userId);
    }

    /**
     * 🚀 Limpia cachés (útil para logout/login)
     */
    public static void limpiarCaches() {
        synchronized (recetasCache) {
            recetasCache.clear();
        }
        synchronized (cacheLock) {
            ingredientMapCache = null;
            unitImportanceMapCache = null;
        }
    }

    /**
     * 🚀 Apaga el executor al cerrar la app
     */
    public static void shutdown() {
        backgroundExecutor.shutdown();
    }
}