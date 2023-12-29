package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;

import com.david.recetapp.negocio.beans.Day;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecetasSrv {
    public static final String JSON = "lista_recetas.json";

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
            e.printStackTrace();
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
                e.printStackTrace();
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
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static List<Receta> cargarListaRecetasCalendario(Activity activity, List<String> idRecetas) {
        List<Receta> recetas = cargarListaRecetas(activity);
        Temporada temporada = UtilsSrv.getTemporadaFecha(new Date());
        // Nos quedamos con los que no hayan sido seleccionados y de la temporada actual (no postres) y ordenamos por fecha de adiccion al calendario (para añadir los que no han sido añadidos), puntuación y estrellas
        return recetas.stream().filter(r -> !idRecetas.contains(r.getId()) && r.getTemporadas().contains(temporada) && !r.isPostre()).sorted((r1, r2) -> {
            int resultado = Comparator.comparing(Receta::getFechaCalendario).compare(r1, r2);
            if (resultado != 0) {
                return resultado;
            }
            resultado = Comparator.comparing(Receta::getPuntuacionDada).compare(r2, r1);

            if (resultado != 0) {
                return resultado;
            }
            resultado = Comparator.comparing(Receta::getEstrellas).compare(r2, r1);
            return resultado;
        }).collect(Collectors.toList());
    }

    public static void actualizarRecetasCalendario(Activity activity, Day dia) {
        List<Receta> listaRecetas = cargarListaRecetas(activity);

        // Obtener la fecha actual con el año y mes actuales, pero con el día de dayOfMonth
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, dia.getDayOfMonth());
        Date fechaEspecifica = cal.getTime();

        listaRecetas.stream().filter(r -> dia.getRecetas().contains(r.getId())).forEach(r -> {
            r.setFechaCalendario(fechaEspecifica);
            editarReceta(activity, r);
        });
    }

    public static List<Receta> obtenerRecetasPorId(Activity activity, List<String> idRecetas) {
        List<Receta> listaRecetas = cargarListaRecetas(activity);
        return listaRecetas.stream().filter(r -> idRecetas.contains(r.getId())).collect(Collectors.toList());
    }
}