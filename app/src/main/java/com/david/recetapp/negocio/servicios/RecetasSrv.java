package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.RecetaDia;
import com.david.recetapp.negocio.beans.Temporada;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** @noinspection DataFlowIssue*/
public class RecetasSrv {
    public static final String JSON = "lista_recetas.json";
    private static final String TAG = "RecetasSrv";

    // Cache: key → lista de recetas
    private static final LruCache<String, List<Receta>> recetasCache = new LruCache<>(4 * 1024 * 1024);
    // Timestamps para validación
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutos

    /** Carga todas las recetas, usa cache si está válida */
    public static List<Receta> cargarListaRecetas(Context context) {
        final String cacheKey = "todas_las_recetas";
        List<Receta> recetas = recetasCache.get(cacheKey);
        long lastUpdate = cacheTimestamps.getOrDefault(cacheKey, 0L);

        if (recetas != null && System.currentTimeMillis() - lastUpdate < CACHE_VALIDITY_MS) {
            return new ArrayList<>(recetas);
        }

        try (FileInputStream fis = context.openFileInput(JSON);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            Type listType = new TypeToken<List<Receta>>(){}.getType();
            recetas = new Gson().fromJson(sb.toString(), listType);
            recetas.forEach(r -> r.setPuntuacionDada(context));

            recetasCache.put(cacheKey, new ArrayList<>(recetas));
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            return new ArrayList<>(recetas);

        } catch (FileNotFoundException e) {
            // archivo no existe → lista vacía
        } catch (IOException e) {
            Log.e(TAG, "Error en 'cargarListaRecetas': " + e.getMessage());
        }

        return new ArrayList<>();
    }

    /** Añade una receta y limpia la cache */
    public static void addReceta(Context context, Receta receta) {
        List<Receta> lista = cargarListaRecetas(context);
        boolean existe = lista.stream().anyMatch(r -> r.getId().equals(receta.getId()));
        if (!existe) {
            lista.addFirst(receta);
            persistirRecetas(context, lista);
            invalidateCaches();
        }
    }

    /** Edita una receta existente y limpia la cache */
    public static void editarReceta(Context context, Receta receta) {
        List<Receta> lista = cargarListaRecetas(context);
        Optional<Receta> opt = lista.stream()
                .filter(r -> r.getId().equals(receta.getId()))
                .findFirst();

        if (opt.isPresent()) {
            lista.remove(opt.get());
            lista.addFirst(receta);
            persistirRecetas(context, lista);
            invalidateCaches();
        }
    }

    /** Elimina una receta (por posición dentro de una lista parcial) y limpia la cache */
    public static void eliminarReceta(Context context, int position, List<Receta> listaParcial) {
        Receta receta = listaParcial.remove(position);

        List<Receta> lista = cargarListaRecetas(context);
        lista.removeIf(r -> r.getId().equals(receta.getId()));
        persistirRecetas(context, lista);
        invalidateCaches();
    }

    /** Carga recetas para calendario, usa cache si válida */
    public static List<Receta> cargarListaRecetasCalendario(Context context, List<RecetaDia> idRecetas) {
        String cacheKey = "recetas_calendario_" + idRecetas.hashCode();
        List<Receta> recetas = recetasCache.get(cacheKey);
        long lastUpdate = cacheTimestamps.getOrDefault(cacheKey, 0L);

        if (recetas != null && System.currentTimeMillis() - lastUpdate < CACHE_VALIDITY_MS) {
            return recetas;
        }

        List<Receta> todas = cargarListaRecetas(context);
        Temporada temporada = UtilsSrv.getTemporadaFecha(LocalDate.now());
        Set<String> seleccionadas = idRecetas.stream()
                .map(RecetaDia::idReceta)
                .collect(Collectors.toSet());

        recetas = todas.stream()
                .filter(r -> !seleccionadas.contains(r.getId()) && r.getTemporadas().contains(temporada))
                .sorted(Comparator.comparing(Receta::getPuntuacionDada, Comparator.reverseOrder())
                        .thenComparing(Receta::getEstrellas, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        recetasCache.put(cacheKey, recetas);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return recetas;
    }

    /** Actualiza sólo la fecha de calendario de una receta */
    public static void actualizarRecetaCalendario(Context context, String idReceta, int diaMes, boolean add) {
        Optional<Receta> opt = cargarListaRecetas(context).stream()
                .filter(r -> r.getId().equals(idReceta))
                .findAny();

        if (opt.isPresent()) {
            Receta r = opt.get();
            Date fecha = new Date(0);
            if (diaMes > 0) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, diaMes);
                fecha = cal.getTime();
                if (add && r.getFechaCalendario().after(fecha)) {
                    return;
                }
            }
            r.setFechaCalendario(fecha);
            editarReceta(context, r);
        }
    }

    @NonNull
    public static List<Receta> getRecetasAdaptadasCalendario(List<Receta> recetasTotales, Day selectedDay) {
        Set<String> ids = selectedDay.getRecetas().stream()
                .map(RecetaDia::idReceta)
                .collect(Collectors.toSet());

        List<Receta> lista = recetasTotales.stream()
                .filter(r -> ids.contains(r.getId()))
                .collect(Collectors.toList());

        Map<String, Integer> personasMap = selectedDay.getRecetas().stream()
                .collect(Collectors.toMap(RecetaDia::idReceta, RecetaDia::numeroPersonas));

        for (Receta r : lista) {
            double orig = r.getNumPersonas();
            double obj = personasMap.getOrDefault(r.getId(), (int) orig);
            r.setNumPersonas((int) obj);
            double factor = obj / orig;

            for (Ingrediente ing : r.getIngredientes()) {
                double base = UtilsSrv.convertirNumero(ing.getCantidad());
                double escalada = base * factor;
                BigDecimal bd = BigDecimal.valueOf(escalada)
                        .setScale(2, RoundingMode.HALF_UP);
                ing.setCantidad(bd.stripTrailingZeros().toPlainString());
            }
        }

        return lista;
    }

    // —————— Métodos auxiliares ——————

    /** Escribe la lista completa de recetas en el archivo JSON */
    private static void persistirRecetas(Context context, List<Receta> lista) {
        Gson gson = new Gson();
        String json = gson.toJson(lista);
        try (FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(json);
        } catch (IOException e) {
            Log.e(TAG, "Error al persistir recetas: " + e.getMessage());
        }
    }

    /** Limpia la cache principal y de calendario */
    private static void invalidateCaches() {
        String mainKey = "todas_las_recetas";
        recetasCache.remove(mainKey);
        cacheTimestamps.remove(mainKey);

        // Limpia también todas las entradas de calendario
        for (String key : cacheTimestamps.keySet()) {
            if (key.startsWith("recetas_calendario_")) {
                recetasCache.remove(key);
                cacheTimestamps.remove(key);
            }
        }
    }
}
