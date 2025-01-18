package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.david.recetapp.negocio.beans.Receta;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    public static List<Receta> cargarListaRecetasCalendario(Context context, List<String> idRecetas) {
        List<Receta> recetas = cargarListaRecetas(context);
        Temporada temporada = UtilsSrv.getTemporadaFecha(LocalDate.now());
        // Nos quedamos con los que no hayan sido seleccionados y de la temporada actual y ordenamos por puntuación y estrellas
        return recetas.stream()
                .filter(r -> !idRecetas.contains(r.getId()) && r.getTemporadas().contains(temporada))
                .sorted(Comparator.comparing(Receta::getPuntuacionDada, Comparator.reverseOrder())
                        .thenComparing(Receta::getEstrellas, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public static void actualizarRecetaCalendario(Context context, Receta receta, int diaMes, boolean add) {
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