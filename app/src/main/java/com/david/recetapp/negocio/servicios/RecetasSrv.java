package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.content.res.XmlResourceParser;
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

import org.xmlpull.v1.XmlPullParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static Map<String, Integer> gramosMapCache = null; // ← NUEVO
    private static final Object cacheLock = new Object();

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

    public static void cargarListaRecetas(Context context, RecetasCallback callback) {
        if (!recetasCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(recetasCache));
            return;
        }

        inicializarCachesEnBackground(context);

        firebaseManager.cargarRecetas(context, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                recetasCache.clear();
                recetasCache.addAll(recetas);
                Log.d(TAG, "✅ Recetas cargadas: " + recetas.size());
                callback.onSuccess(new ArrayList<>(recetasCache));
                calcularPuntuacionesEnBackground(context);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas desde Firebase", e);
                callback.onFailure(e);
            }
        });
    }

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

    private static void inicializarCachesEnBackground(Context context) {
        synchronized (cacheLock) {
            if (ingredientMapCache != null && unitImportanceMapCache != null && gramosMapCache != null) {
                return;
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

    private static void calcularPuntuacionesEnBackground(Context context) {
        backgroundExecutor.execute(() -> {
            try {
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
     * 🚀 Inicializa mapas de ingredientes, unidades y gramos (SOLO UNA VEZ)
     * Parsea el XML directamente para leer <nombre>, <puntuacion> y <gramos>
     */
    private static void inicializarMapas(Context context) {
        synchronized (cacheLock) {
            if (ingredientMapCache != null && unitImportanceMapCache != null && gramosMapCache != null) {
                return;
            }

            // ——— Mapa de ingredientes y gramos via XmlResourceParser ———
            ingredientMapCache = new HashMap<>();
            gramosMapCache = new HashMap<>();

            try {
                XmlResourceParser parser = context.getResources().getXml(R.xml.ingredient_list);
                int event = parser.getEventType();

                String nombre = null;
                int puntuacion = -2;
                int gramos = -1;
                String currentTag = null;

                while (event != XmlPullParser.END_DOCUMENT) {
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            currentTag = parser.getName();
                            if ("item".equals(currentTag)) {
                                nombre = null;
                                puntuacion = -2;
                                gramos = -1;
                            }
                            break;

                        case XmlPullParser.TEXT:
                            if (currentTag == null) break;
                            String text = parser.getText().trim();
                            switch (currentTag) {
                                case "nombre":
                                    nombre = text;
                                    break;
                                case "puntuacion":
                                    try { puntuacion = Integer.parseInt(text); } catch (NumberFormatException ignored) {}
                                    break;
                                case "gramos":
                                    try { gramos = Integer.parseInt(text); } catch (NumberFormatException ignored) {}
                                    break;
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            if ("item".equals(parser.getName()) && nombre != null) {
                                String key = nombre.toLowerCase(Locale.getDefault());
                                ingredientMapCache.put(key, puntuacion);
                                gramosMapCache.put(key, gramos);
                            }
                            currentTag = null;
                            break;
                    }
                    event = parser.next();
                }
                parser.close();

            } catch (Exception e) {
                Log.e(TAG, "Error parseando ingredient_list XML", e);
            }

            // ——— Mapa de unidades ———
            unitImportanceMapCache = new HashMap<>();
            String[] units = context.getResources().getStringArray(R.array.quantity_units);
            int[] importanceValues = context.getResources().getIntArray(R.array.importance_values);
            for (int i = 0; i < units.length; i++) {
                unitImportanceMapCache.put(units[i].trim(), importanceValues[i]);
            }
            // Debug temporal
            for (Map.Entry<String, Integer> e : unitImportanceMapCache.entrySet()) {
                Log.d(TAG, "UNIT: '" + e.getKey() + "' = " + e.getValue());
            }
        }
    }

    private static void calcularPuntuacion(Receta receta) {
        if (ingredientMapCache == null || unitImportanceMapCache == null || gramosMapCache == null) {
            Log.e(TAG, "❌ Mapas null, no se calcula");
            return;
        }

        Log.d(TAG, "🔢 Calculando: " + receta.getNombre());

        for (Ingrediente ing : receta.getIngredientes()) {
            String nombre = ing.getNombre().toLowerCase(Locale.getDefault());
            ing.setPuntuacion(ingredientMapCache.getOrDefault(nombre, -2));
        }

        double cantidadTotal = 0;
        for (Ingrediente ing : receta.getIngredientes()) {
            if (ing.getPuntuacion() >= 0) {
                double cantidad = UtilsSrv.convertirNumero(ing.getCantidad());
                cantidadTotal += cantidad * getImportancia(ing);
            }
        }

        if (cantidadTotal == 0) {
            receta.setPuntuacionDada(0);
            return;
        }

        double puntuacionTotal = 0;
        for (Ingrediente ing : receta.getIngredientes()) {
            if (ing.getPuntuacion() >= 0) {
                double cantidad = UtilsSrv.convertirNumero(ing.getCantidad());
                double peso = (cantidad * getImportancia(ing)) / cantidadTotal;
                puntuacionTotal += ing.getPuntuacion() * peso;
            }
        }
        receta.setPuntuacionDada(puntuacionTotal);
    }

    private static int getImportancia(Ingrediente ing) {
        String tipoCantidad = ing.getTipoCantidad();
        if ("unidad".equals(tipoCantidad) && gramosMapCache != null) {
            Integer gramos = gramosMapCache.get(ing.getNombre().toLowerCase(Locale.getDefault()));
            if (gramos != null && gramos > 0) return gramos;
        }
        Integer val = unitImportanceMapCache.get(tipoCantidad);
        if (val == null) {
            Log.w(TAG, "Unidad no encontrada en mapa: '" + tipoCantidad + "'");
        }
        return val != null ? val : 1;
    }

    public static void setPuntuacionDada(Receta receta, Context context) {
        if (ingredientMapCache == null || unitImportanceMapCache == null || gramosMapCache == null) {
            inicializarCachesEnBackground(context);
            return;
        }
        calcularPuntuacion(receta);
    }

    public static void asegurarPuntuacionesCalculadas(Context context, RecetasCallback callback) {
        if (ingredientMapCache == null || unitImportanceMapCache == null || gramosMapCache == null) {
            inicializarMapas(context);
        }

        backgroundExecutor.execute(() -> {
            synchronized (recetasCache) {
                for (Receta receta : recetasCache) {
                    if (receta.getPuntuacionDada() == 0) {
                        calcularPuntuacion(receta);
                    }
                }
            }
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

    public static void limpiarCaches() {
        synchronized (recetasCache) {
            recetasCache.clear();
        }
        synchronized (cacheLock) {
            ingredientMapCache = null;
            unitImportanceMapCache = null;
            gramosMapCache = null; // ← NUEVO
        }
    }

    /**
     * Devuelve la lista de ingredientes en formato "Nombre Puntuacion" para las actividades.
     */
    public static String[] getIngredientListStrings(Context context) {
        List<String> list = new ArrayList<>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.ingredient_list);
            int event = parser.getEventType();
            String nombre = null;
            String puntuacion = "-2";
            String currentTag = null;

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    currentTag = parser.getName();
                } else if (event == XmlPullParser.TEXT) {
                    if ("nombre".equals(currentTag)) nombre = parser.getText().trim();
                    else if ("puntuacion".equals(currentTag)) puntuacion = parser.getText().trim();
                } else if (event == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
                    if (nombre != null) list.add(nombre + " " + puntuacion);
                    nombre = null;
                    puntuacion = "-2";
                }
                event = parser.next();
            }
            parser.close();
        } catch (Exception e) {
            Log.e(TAG, "Error parseando ingredient_list para actividades", e);
        }
        return list.toArray(new String[0]);
    }

    public static void shutdown() {
        backgroundExecutor.shutdown();
    }
}