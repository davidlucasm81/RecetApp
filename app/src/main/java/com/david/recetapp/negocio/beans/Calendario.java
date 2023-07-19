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
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Calendario {
    private CalendarioBean calendarioBean;
    private final Queue<Receta> colaRecetas;

    private final Set<Receta> recetasSeleccionadas;
    private final Context context;


    public Calendario(Context context, int diasLimite) {
        this.context = context;
        this.calendarioBean = new CalendarioBean();
        this.recetasSeleccionadas = new HashSet<>();
        colaRecetas = new ArrayDeque<>();
        obtenerRecetas(diasLimite);
        InicioSemana();
    }

    public Calendario() {
        this.context = null;
        this.calendarioBean = new CalendarioBean();
        colaRecetas = new ArrayDeque<>();
        this.recetasSeleccionadas = new HashSet<>();
    }

    private void InicioSemana() {
        // Obtener la fecha actual
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);

        // Reiniciar la lista de recetas
        calendarioBean.getListaRecetas().clear();
        if (diaActual != Calendar.SUNDAY) {
            // Recorrer los días de la semana y agregar dos recetas para cada día
            for (int i = diaActual; i <= Calendar.SATURDAY; i++) {
                calendar.set(Calendar.DAY_OF_WEEK, i);

                // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
                DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
                diaRecetas.addReceta(obtenerReceta());
                diaRecetas.addReceta(obtenerReceta());

                // Agregar el objeto DiaRecetas a la lista
                calendarioBean.getListaRecetas().add(diaRecetas);
            }
        }

        // Domingo da igual que dia de la semana es que al ser el ultimo día hay que meterlo:
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
        DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
        diaRecetas.addReceta(obtenerReceta());
        diaRecetas.addReceta(obtenerReceta());

        // Agregar el objeto DiaRecetas a la lista
        calendarioBean.getListaRecetas().add(diaRecetas);

        // Guardar la lista de recetas actualizada en el archivo JSON
        guardarListaRecetas();
    }

    public List<DiaRecetas> getListaRecetas() {
        return calendarioBean.getListaRecetas();
    }

    private void obtenerRecetas(int diasLimite) {
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
            colaRecetas.addAll(listaRecetas);
        } catch (FileNotFoundException e) {
            // El archivo no existe, no se hace nada
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String obtenerReceta() {
        // Obtener la primera receta de la cola
        Receta receta = colaRecetas.poll();
        if (receta == null) {
            return "-1";
        }
        // Agregar la receta al final de la cola
        colaRecetas.offer(receta);
        if (recetasSeleccionadas.contains(receta))
            return "-1";
        receta.setFechaCalendario(new Date());
        recetasSeleccionadas.add(receta);
        return receta.getId();
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

    public CalendarioBean getCalendarioBean() {
        return calendarioBean;
    }

    public void setCalendarioBean(CalendarioBean calendarioBean) {
        this.calendarioBean = calendarioBean;
    }

}
