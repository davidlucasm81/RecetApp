package com.david.recetapp.negocio.servicios;

import android.content.Context;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio de recetas con cache en memoria persistente mientras la app está activa.
 */
public class RecetasSrv {
    private static final String TAG = "RecetasSrv";

    private static final FirebaseManager firebaseManager = new FirebaseManager();

    // Cache en memoria
    private static final List<Receta> recetasCache = new ArrayList<>();

    private static final Pattern patternIngredient = Pattern.compile("^(.+)\\s(-?\\d+)$");

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

    // ——— CARGA DESDE FIREBASE ———
    public static void cargarListaRecetas(Context context, RecetasCallback callback) {
        if (!recetasCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(recetasCache));
            return;
        }

        firebaseManager.cargarRecetas(context, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                recetasCache.clear();
                recetasCache.addAll(recetas);
                callback.onSuccess(new ArrayList<>(recetasCache));
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

        firebaseManager.cargarRecetas(context, true, new FirebaseManager.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                recetasCache.clear();
                recetasCache.addAll(recetas);
                callback.onSuccess(new ArrayList<>(recetasCache));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando recetas desde Firebase (forceServer=true)", e);
                callback.onFailure(e);
            }
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

    public static void setPuntuacionDada(Receta receta, Context context) {
        Map<String, Integer> ingredientMap = new HashMap<>();
        String[] ingredientList = context.getResources().getStringArray(R.array.ingredient_list);
        for (String s : ingredientList) {
            Matcher m = patternIngredient.matcher(s.trim());
            if (m.matches()) ingredientMap.put(Objects.requireNonNull(m.group(1)).toLowerCase(Locale.getDefault()), Integer.parseInt(m.group(2)));
        }

        for (Ingrediente ing : receta.getIngredientes()) {
            ing.setPuntuacion(ingredientMap.getOrDefault(ing.getNombre().toLowerCase(Locale.getDefault()), -2));
        }

        String[] units = context.getResources().getStringArray(R.array.quantity_units);
        int[] importanceValues = context.getResources().getIntArray(R.array.importance_values);
        Map<String, Integer> unitImportanceMap = new HashMap<>();
        for (int i = 0; i < units.length; i++) unitImportanceMap.put(units[i], importanceValues[i]);

        double cantidadTotal = receta.getIngredientes().stream()
                .filter(i -> i.getPuntuacion() >= 0)
                .mapToDouble(i -> UtilsSrv.convertirNumero(i.getCantidad()) * unitImportanceMap.getOrDefault(i.getTipoCantidad(), 1))
                .sum();

        if (cantidadTotal == 0) {
            receta.setPuntuacionDada(0);
            return;
        }

        receta.setPuntuacionDada(receta.getIngredientes().stream()
                .filter(i -> i.getPuntuacion() >= 0)
                .mapToDouble(i -> i.getPuntuacion() * (UtilsSrv.convertirNumero(i.getCantidad()) * unitImportanceMap.getOrDefault(i.getTipoCantidad(), 1) / cantidadTotal))
                .sum());
    }
}
