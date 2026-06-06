package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.content.res.XmlResourceParser;
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

    // ← ID del usuario activo; toda operación con datos requiere que esté configurado
    private static volatile String currentUserId = null;

    // Cache en memoria
    private static final List<Receta> recetasCache = new ArrayList<>();

    // 🚀 Caché de mapas de ingredientes (se crean UNA SOLA VEZ por sesión)
    private static Map<String, Integer> ingredientMapCache = null;
    public static Map<String, Integer> unitImportanceMapCache = null;
    public static Map<String, Integer> gramosMapCache = null;
    private static Map<String, String> translationMapCache = null; // Key (any lang) -> Target name (current locale)
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

    // ——— Helpers de validación de userId ———

    /**
     * Comprueba que no hay un userId activo antes de cualquier operación con datos.
     * Llama a {@code cb.onFailure} y devuelve {@code true} si no está configurado.
     */
    private static boolean checkNotUserId(RecetasCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return true;
        }
        return false;
    }

    private static boolean checkNotUserId(SimpleCallback cb) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Operación rechazada: userId no configurado");
            if (cb != null) cb.onFailure(new IllegalStateException("UserId no configurado"));
            return true;
        }
        return false;
    }

    /** Versión sin callback para métodos fire-and-forget. */
    private static boolean notHasUserId() {
        return currentUserId == null || currentUserId.isEmpty();
    }

    // ——— GETTER DE RECETAS ———
    public static List<Receta> getRecetas() {
        return new ArrayList<>(recetasCache);
    }

    // ——— CARGA DESDE FIREBASE OPTIMIZADA ———

    public static void cargarListaRecetas(Context context, RecetasCallback callback) {
        cargarListaRecetas(context, false, callback);
    }

    public static void cargarListaRecetas(Context context, boolean forceServer, RecetasCallback callback) {
        if (checkNotUserId(callback)) return;

        if (!forceServer && !recetasCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(recetasCache));
            return;
        }

        firebaseManager.cargarRecetas(context, forceServer, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                // Procesamiento en background ANTES de notificar éxito
                backgroundExecutor.execute(() -> {
                    try {
                        inicializarMapas(context);

                        long inicio = System.currentTimeMillis();
                        for (Receta receta : recetas) {
                            calcularPuntuacion(receta);
                        }
                        long duracion = System.currentTimeMillis() - inicio;
                        Log.d(TAG, "✅ Puntuaciones calculadas: " + duracion + "ms");

                        synchronized (recetasCache) {
                            recetasCache.clear();
                            recetasCache.addAll(recetas);
                        }

                        // Notificar en el main thread
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            Log.d(TAG, "✅ Recetas listas (forceServer=" + forceServer + "): " + recetas.size());
                            callback.onSuccess(new ArrayList<>(recetasCache));
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando recetas en background", e);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure(e));
                    }
                });
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


    /**
     * 🚀 Inicializa mapas de ingredientes, unidades y gramos (SOLO UNA VEZ por sesión).
     * Parsea el XML consolidado para leer nombres (es/en), puntuacion y gramos.
     */
    public static void inicializarMapas(Context context) {
        synchronized (cacheLock) {
            if (ingredientMapCache != null && unitImportanceMapCache != null && gramosMapCache != null && translationMapCache != null) {
                return;
            }

            ingredientMapCache = new HashMap<>();
            gramosMapCache = new HashMap<>();
            translationMapCache = new HashMap<>();
            String targetLang = Locale.getDefault().getLanguage();

            try {
                XmlResourceParser parser = context.getResources().getXml(R.xml.ingredient_list);
                int event = parser.getEventType();

                int currentPuntuacion = -2;
                int currentGramos = -1;
                String currentTag = null;
                List<String> currentNombres = new ArrayList<>();
                String targetNombre = null;
                String currentLangAttr = null;

                while (event != XmlPullParser.END_DOCUMENT) {
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            currentTag = parser.getName();
                            if ("item".equals(currentTag)) {
                                String pStr = parser.getAttributeValue(null, "puntuacion");
                                String gStr = parser.getAttributeValue(null, "gramos");
                                try { currentPuntuacion = Integer.parseInt(pStr); } catch (Exception ignored) { currentPuntuacion = -2; }
                                try { currentGramos = Integer.parseInt(gStr); } catch (Exception ignored) { currentGramos = -1; }
                                currentNombres.clear();
                                targetNombre = null;
                            } else if ("nombre".equals(currentTag)) {
                                currentLangAttr = parser.getAttributeValue(null, "lang");
                            }
                            break;

                        case XmlPullParser.TEXT:
                            if ("nombre".equals(currentTag)) {
                                String nombre = parser.getText().trim();
                                if (!nombre.isEmpty()) {
                                    currentNombres.add(nombre);
                                    if (targetLang.equals(currentLangAttr)) {
                                        targetNombre = nombre;
                                    }
                                }
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            if ("item".equals(parser.getName())) {
                                // Si no encontramos el nombre en el idioma objetivo, usamos el primero disponible (fallback)
                                if (targetNombre == null && !currentNombres.isEmpty()) {
                                    targetNombre = currentNombres.get(0);
                                }

                                for (String n : currentNombres) {
                                    String key = n.toLowerCase(Locale.getDefault());
                                    ingredientMapCache.put(key, currentPuntuacion);
                                    gramosMapCache.put(key, currentGramos);
                                    if (targetNombre != null) {
                                        translationMapCache.put(key, targetNombre);
                                    }
                                }
                            }
                            currentTag = null;
                            currentLangAttr = null;
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
            Integer puntu = ingredientMapCache.get(nombre);
            ing.setPuntuacion(puntu != null ? puntu : -2);
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

    // ——— OPERACIONES SOBRE RECETAS ———

    public static void addReceta(Receta receta, SimpleCallback callback) {
        if (checkNotUserId(callback)) return;

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
        if (checkNotUserId(callback)) return;

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
        if (checkNotUserId(callback)) return;

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

    public static void cargarListaRecetasCalendario(Context context, Day targetDay, RecetasCallback callback) {
        if (checkNotUserId(callback)) return;

        cargarListaRecetas(context, new RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> todas) {
                // Usar la temporada del día destino, no la de hoy
                LocalDate date = LocalDate.of(targetDay.getYear(), targetDay.getMonth() + 1, targetDay.getDayOfMonth());
                Temporada temporada = UtilsSrv.getTemporadaFecha(date);
                
                Set<String> seleccionadas = targetDay.getRecetas().stream()
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
        if (notHasUserId()) {
            Log.e(TAG, "❌ actualizarRecetaCalendario sin userId");
            return;
        }

        if (!recetasCache.isEmpty()) {
            Optional<Receta> opt = recetasCache.stream()
                    .filter(r -> r.getId().equals(idReceta))
                    .findAny();
            if (opt.isPresent()) {
                actualizarRecetaCalendarioDirect(opt.get(), diaMes, add);
                return;
            }
        }

        cargarListaRecetas(context, new RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                Optional<Receta> opt = recetas.stream()
                        .filter(r -> r.getId().equals(idReceta))
                        .findAny();

                opt.ifPresent(r -> actualizarRecetaCalendarioDirect(r, diaMes, add));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas para actualizar calendario", e);
            }
        });
    }

    public static void actualizarRecetaCalendarioDirect(Receta receta, long timestamp, boolean add) {
        if (notHasUserId()) {
            Log.e(TAG, "❌ actualizarRecetaCalendarioDirect sin userId");
            return;
        }
        if (receta == null) return;

        if (timestamp > 0) {
            Date fecha = new Date(timestamp);
            if (add && receta.getFechaCalendario() != null && !fecha.after(receta.getFechaCalendario())) return;
        }

        firebaseManager.actualizarFechaCalendario(receta.getId(), timestamp, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Fecha calendario actualizada para: " + receta.getId());
                receta.setFechaCalendario(timestamp == 0 ? null : new Date(timestamp));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error actualizando fecha calendario", e);
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
            // Evitar NullPointerException por unboxing: el mapa puede contener valores nulos
            Integer personasMapVal = personasMap.get(r.getId());
            int objInt = (personasMapVal != null) ? personasMapVal : (int) orig;
            r.setNumPersonas(objInt);

            // Evitar división por cero si orig == 0
            double factor = (orig == 0) ? 1.0 : ((double) objInt / orig);

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

    /**
     * Establece el userId activo. Si cambia de usuario, limpia automáticamente toda la caché
     * para evitar que datos de una sesión anterior sean visibles en la nueva.
     */
    public static void setUserId(String userId) {
        if (!Objects.equals(currentUserId, userId)) {
            Log.d(TAG, "🔑 Cambio de userId detectado → limpiando caché de recetas");
            limpiarCaches();
            currentUserId = userId;
        }
        firebaseManager.setUserId(userId);
    }

    public static String getNombreTraducido(String nombre) {
        if (translationMapCache == null || nombre == null) return nombre;
        String translated = translationMapCache.get(nombre.toLowerCase(Locale.getDefault()));
        return translated != null ? translated : nombre;
    }

    public static void limpiarCaches() {
        synchronized (recetasCache) {
            recetasCache.clear();
        }
        synchronized (cacheLock) {
            ingredientMapCache = null;
            unitImportanceMapCache = null;
            gramosMapCache = null;
            translationMapCache = null;
        }
    }

    /**
     * Actualiza la fecha de una receta en la caché local de RecetasSrv.
     */
    public static void actualizarFechaRecetaEnCache(String recetaId, Date fecha) {
        synchronized (recetasCache) {
            for (Receta r : recetasCache) {
                if (r.getId().equals(recetaId)) {
                    r.setFechaCalendario(fecha);
                    break;
                }
            }
        }
    }

    /**
     * Devuelve la lista de ingredientes en formato "Nombre Puntuacion" para las actividades.
     */
    public static String[] getIngredientListStrings(Context context) {
        List<String> list = new ArrayList<>();
        String targetLang = Locale.getDefault().getLanguage(); // "es" o "en"

        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.ingredient_list);
            int event = parser.getEventType();
            String currentNombre = null;
            String currentPuntuacion = "-2";
            String currentTag = null;
            boolean isTargetLang = false;

            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        if ("item".equals(currentTag)) {
                            currentPuntuacion = parser.getAttributeValue(null, "puntuacion");
                        } else if ("nombre".equals(currentTag)) {
                            String lang = parser.getAttributeValue(null, "lang");
                            isTargetLang = targetLang.equals(lang);
                        }
                        break;

                    case XmlPullParser.TEXT:
                        if ("nombre".equals(currentTag) && isTargetLang) {
                            currentNombre = parser.getText().trim();
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("item".equals(parser.getName())) {
                            if (currentNombre != null) {
                                list.add(currentNombre + " " + currentPuntuacion);
                            }
                            currentNombre = null;
                        }
                        currentTag = null;
                        break;
                }
                event = parser.next();
            }
            parser.close();
        } catch (Exception e) {
            Log.e(TAG, "Error parseando ingredient_list para actividades", e);
        }
        return list.toArray(new String[0]);
    }

}