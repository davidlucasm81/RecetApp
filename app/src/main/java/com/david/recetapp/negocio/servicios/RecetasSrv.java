package com.david.recetapp.negocio.servicios;

import android.content.Context;

import com.david.recetapp.negocio.beans.Receta;
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
            recetas.stream().filter(r -> r.getIngredientes().stream().anyMatch(i -> i.getPuntuacion()<1)).forEach(r ->{
                r.setPuntuacionDada(context);
            });
            return recetas.stream().sorted((r1, r2) -> {
                int resultado = Comparator.comparing(Receta::getFechaCalendario)
                        .compare(r1, r2);
                if (resultado != 0) {
                    return resultado;
                }
                resultado =  Comparator.comparing(Receta::getPuntuacionDada)
                        .compare(r2, r1);

                if (resultado != 0) {
                    return resultado;
                }
                resultado = Comparator.comparing(Receta::getEstrellas)
                        .compare(r2, r1);
                return resultado;
            }).collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            // El archivo no existe, no se hace nada
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void refrescarFechasRecetas(Context context) {
        List<Receta> listaRecetas = cargarListaRecetas(context);
        listaRecetas.forEach(r -> r.setFechaCalendario(new Date(0)));
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

    public static void addReceta(Context context, Receta receta) {
        List<Receta> listaRecetas = cargarListaRecetas(context);
        if(listaRecetas.stream().noneMatch(r -> receta.getId().equals(r.getId()))){
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
            // Añadir receta al calendario si tiene algun dia vacio
            CalendarioSrv.addReceta(context);
        }

    }
    public static void editarReceta(Context context, Receta receta){
        List<Receta> listaRecetas = cargarListaRecetas(context);
        Optional<Receta> recetaEncontrada = listaRecetas.stream().filter(r -> receta.getId().equals(r.getId())).findFirst();
        if(recetaEncontrada.isPresent()){
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
            // Añadir receta al calendario si tiene algun dia vacio
            CalendarioSrv.addReceta(context);
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
        //Si existe la receta en el calendario la borramos y añadimos otra:
        CalendarioSrv.eliminarReceta(context, receta);
    }

    public static void modificarRecetas(Context context, ArrayList<Receta> recetas) {
        List<Receta> listaRecetas = cargarListaRecetas(context);
        listaRecetas.removeIf(r -> recetas.stream().anyMatch(receta -> receta.getId().equals(r.getId())));
        listaRecetas.addAll(recetas);
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
