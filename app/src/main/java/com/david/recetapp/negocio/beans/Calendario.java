package com.david.recetapp.negocio.beans;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import java.util.Queue;
import java.util.stream.Collectors;

public class Calendario implements Serializable {
    private final List<DiaRecetas> listaRecetas;
    private final Queue<Receta> colaRecetas;
    private final Context context;
    private long ultimaActualizacion;

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public Calendario(Context context) {
        this.context = context;
        listaRecetas = new ArrayList<>();
        colaRecetas = new ArrayDeque<>();
        obtenerRecetas();
        InicioSemana();
    }

    public void InicioSemana() {
        // Obtener la fecha actual
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);

        // Reiniciar la lista de recetas
        listaRecetas.clear();
        if (diaActual != Calendar.SUNDAY) {
            // Recorrer los días de la semana y agregar dos recetas para cada día
            for (int i = diaActual; i <= Calendar.SATURDAY; i++) {
                calendar.set(Calendar.DAY_OF_WEEK, i);

                // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
                DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
                diaRecetas.addReceta(obtenerReceta());
                diaRecetas.addReceta(obtenerReceta());

                // Agregar el objeto DiaRecetas a la lista
                listaRecetas.add(diaRecetas);
            }
        }

        // Domingo da igual que dia de la semana es que al ser el ultimo día hay que meterlo:
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
        DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
        diaRecetas.addReceta(obtenerReceta());
        diaRecetas.addReceta(obtenerReceta());

        // Agregar el objeto DiaRecetas a la lista
        listaRecetas.add(diaRecetas);

        // Guardar la lista de recetas actualizada en el archivo JSON
        guardarListaRecetas();
    }

    public List<DiaRecetas> getListaRecetas() {
        return listaRecetas;
    }

    public void obtenerRecetas() {
        // Cargar el archivo JSON desde el almacenamiento interno
        try {
            //TODO: NO LO OBTIENE
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
            // Ordenamos por fecha y después por estrellas
            listaRecetas = listaRecetas.stream().sorted((r1, r2) -> r1.getFechaCalendario().compareTo(r2.getFechaCalendario()) - (int) (r1.getEstrellas() - r2.getEstrellas())).collect(Collectors.toList());

            // Agregar las recetas a la cola
            colaRecetas.addAll(listaRecetas);
        } catch (FileNotFoundException e) {
            // El archivo no existe, no se hace nada
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: METER COMPROBACION DE FECHA DE CALENDARIO <= FECHA LIMITE
    public Receta obtenerReceta() {
        // Obtener la primera receta de la cola
        Receta receta = colaRecetas.poll();
        receta.setFechaCalendario(new Date());
        if (receta != null)
            // Agregar la receta al final de la cola
            colaRecetas.offer(receta);

        return receta;
    }

    private void guardarListaRecetas() {
        // Convertir la cola de recetas en una lista
        List<Receta> listaRecetas = new ArrayList<>(colaRecetas);

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

}
