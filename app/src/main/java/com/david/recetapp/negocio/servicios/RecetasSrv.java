package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.util.Log;

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
import java.util.stream.Collectors;

public class RecetasSrv {
    public static final String JSON = "lista_recetas.json";
    private static final String TAG = "RecetasSrv";

    public static List<Receta> cargarListaRecetas(Context context) {
        // Cargar el archivo JSON desde el almacenamiento interno
        try {
            FileInputStream fis = context.openFileInput(JSON);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }

            br.close();
            isr.close();
            fis.close();

            // Convertir el JSON a una lista de objetos Receta utilizando GSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Receta>>() {
            }.getType();
            // Agregar las recetas a la cola
            List<Receta> recetas = gson.fromJson(jsonBuilder.toString(), listType);
            recetas.forEach(r -> r.setPuntuacionDada(context));
            return recetas;
        } catch (FileNotFoundException e) {
            // El archivo no existe, no se hace nada
        } catch (IOException e) {
            Log.e(TAG, "Error en 'cargarListaRecetas': " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static void addReceta(Context context, Receta receta) {
        List<Receta> listaRecetas = cargarListaRecetas(context);
        if (listaRecetas.stream().noneMatch(r -> receta.getId().equals(r.getId()))) {
            // Agregar la receta al principio de la lista
            listaRecetas.add(0, receta);

            // Guardar la lista actualizada en el archivo JSON
            // Convertir la lista de recetas a JSON
            Gson gson = new Gson();
            String jsonRecetas = gson.toJson(listaRecetas);

            // Guardar el JSON en el archivo
            try {
                FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(jsonRecetas);
                osw.close();
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "Error en 'addReceta': " + e.getMessage());
            }
        }

    }

    public static void editarReceta(Context context, Receta receta) {
        List<Receta> listaRecetas = cargarListaRecetas(context);
        Optional<Receta> recetaEncontrada = listaRecetas.stream().filter(r -> receta.getId().equals(r.getId())).findFirst();
        if (recetaEncontrada.isPresent()) {
            listaRecetas.remove(recetaEncontrada.get());
            // Agregar la receta al principio de la lista
            listaRecetas.add(0, receta);

            // Guardar la lista actualizada en el archivo JSON
            // Convertir la lista de recetas a JSON
            Gson gson = new Gson();
            String jsonRecetas = gson.toJson(listaRecetas);

            // Guardar el JSON en el archivo
            try {
                FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(jsonRecetas);
                osw.close();
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "Error en 'editarReceta': " + e.getMessage());
            }
        }
    }

    public static void eliminarReceta(Context context, int position, List<Receta> listaRecetasParcial) {
        // Eliminar la receta de la lista de recetas y actualizar el archivo JSON
        Receta receta = listaRecetasParcial.remove(position);

        List<Receta> listaRecetas = cargarListaRecetas(context);
        listaRecetas.removeIf(r -> r.getId().equals(receta.getId()));
        // Convertir la lista de recetas a JSON
        Gson gson = new Gson();
        String jsonRecetas = gson.toJson(listaRecetas);

        // Guardar el JSON en el archivo
        try {
            FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(jsonRecetas);
            osw.close();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error en 'eliminarReceta': " + e.getMessage());
        }
    }

    public static List<Receta> cargarListaRecetasCalendario(Context context, List<RecetaDia> idRecetas) {
        List<Receta> recetas = cargarListaRecetas(context);
        Temporada temporada = UtilsSrv.getTemporadaFecha(LocalDate.now());

        // Convertimos la lista de recetas ya seleccionadas en un Set para mejorar eficiencia en la búsqueda
        Set<String> recetasSeleccionadas = idRecetas.stream().map(RecetaDia::getIdReceta).collect(Collectors.toSet());

        return recetas.stream().filter(r -> !recetasSeleccionadas.contains(r.getId()) && r.getTemporadas().contains(temporada)).sorted(Comparator.comparing(Receta::getPuntuacionDada, Comparator.reverseOrder()).thenComparing(Receta::getEstrellas, Comparator.reverseOrder())).collect(Collectors.toList());
    }


    public static void actualizarRecetaCalendario(Context context, String idReceta, int diaMes, boolean add) {
        Optional<Receta> optionalReceta = cargarListaRecetas(context).stream().filter(r -> r.getId().equals(idReceta)).findAny();
        if(optionalReceta.isPresent()) {
            Receta receta = optionalReceta.get();
            // Obtener la fecha actual con el año y mes actuales, pero con el día de dayOfMonth
            Date fechaEspecifica = new Date(0);
            if (diaMes > 0) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, diaMes);
                fechaEspecifica = cal.getTime();
                if (add && receta.getFechaCalendario().after(fechaEspecifica)) {
                    return;
                }
            }
            receta.setFechaCalendario(fechaEspecifica);
            editarReceta(context, receta);
        }
    }

    @NonNull
    public static List<Receta> getRecetasAdaptadasCalendario(List<Receta> recetasTotales, Day selectedDay) {
        final List<Receta> listaRecetas;
        Set<String> recetasIdsDia = selectedDay.getRecetas().stream().map(RecetaDia::getIdReceta).collect(Collectors.toSet());

        // Filtrar las recetas que están en el día seleccionado
        listaRecetas = recetasTotales.stream().filter(r -> recetasIdsDia.contains(r.getId())).collect(Collectors.toList());

        // Recorrer la lista de recetas
        // 1) Prepara un mapa de idReceta → numPersonas para acceso O(1)
        Map<String, Integer> personasPorReceta = selectedDay.getRecetas().stream()
                .collect(Collectors.toMap(
                        RecetaDia::getIdReceta,
                        RecetaDia::getNumeroPersonas
                ));

        for (Receta r : listaRecetas) {
            // 2) Calcula el número de personas objetivo (o usa el original si no está en el mapa)
            double original = r.getNumPersonas();
            //noinspection DataFlowIssue
            double objetivo = personasPorReceta.getOrDefault(r.getId(), (int) original);
            r.setNumPersonas((int) objetivo);

            // 3) Factor de escalado
            double factor = objetivo / original;

            // 4) Actualiza cada ingrediente con redondeo a 2 decimales
            for (Ingrediente ing : r.getIngredientes()) {
                double base = UtilsSrv.convertirNumero(ing.getCantidad());
                double escalada = base * factor;

                // BigDecimal para redondear HALF_UP a 2 decimales
                BigDecimal bd = BigDecimal
                        .valueOf(escalada)
                        .setScale(2, RoundingMode.HALF_UP);

                // stripTrailingZeros convierte "2.00" → "2", "2.50" → "2.5"
                ing.setCantidad(bd.stripTrailingZeros().toPlainString());
            }
        }
        return listaRecetas;
    }
}