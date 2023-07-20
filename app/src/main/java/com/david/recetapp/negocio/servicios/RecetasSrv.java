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
import java.util.List;
import java.util.stream.Collectors;

public class RecetasSrv {

    public static void guardarListaRecetas(Context context, List<Receta> listaRecetas) {
        // Convertir la lista de recetas a JSON
        Gson gson = new Gson();
        String jsonRecetas = gson.toJson(listaRecetas);

        // Guardar el JSON en el archivo
        try {
            FileOutputStream fos = context.openFileOutput("lista_recetas.json", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(jsonRecetas);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Receta> obtenerRecetas(Context context, int diasLimite) {
        // Cargar el archivo JSON desde el almacenamiento interno
        try {
            FileInputStream fis = context.openFileInput("lista_recetas.json");
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
            List<Receta> listaRecetas = gson.fromJson(jsonBuilder.toString(), listType);

            // Obtener la fecha y hora actual del ordenador en milisegundos
            long tiempoActual = System.currentTimeMillis();

            // Ordenamos por fecha y después por estrellas
            listaRecetas = listaRecetas.stream().filter(r1 -> {
                // Calcular la diferencia en milisegundos entre la fecha actual y 'tuFecha'
                long diferenciaEnMilisegundos = tiempoActual - r1.getFechaCalendario().getTime();

                // Convertir la diferencia en milisegundos a días
                long diasPasados = diferenciaEnMilisegundos / (1000 * 60 * 60 * 24);
                return diasPasados >= diasLimite;
            }).sorted((r1, r2) -> r1.getFechaCalendario().compareTo(r2.getFechaCalendario()) - (int) (r1.getEstrellas() - r2.getEstrellas())).collect(Collectors.toList());

            // Agregar las recetas a la cola
            return listaRecetas;
        } catch (FileNotFoundException e) {
            // El archivo no existe, no se hace nada
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
