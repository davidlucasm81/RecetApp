package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.DiaRecetas;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;

public class CalendarioSrv {
    private static final String JSON = "calendario.json";
    private static Queue<Receta> colaRecetas;

    public static CalendarioBean cargarCalendario(Context context) {
        try {
            FileInputStream fis = context.openFileInput(JSON);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            Gson gson = new Gson();
            CalendarioBean calendarioBean = gson.fromJson(br, CalendarioBean.class);
            if (calendarioBean == null) {
                throw new FileNotFoundException();
            }
            br.close();
            isr.close();
            fis.close();
            Calendar calendar = Calendar.getInstance();
            int diaActual = calendar.get(Calendar.DAY_OF_WEEK);
            if (diaActual == Calendar.MONDAY && !UtilsSrv.esMismoDia(calendarioBean.getUltimaActualizacion(), calendar)) {
                calendarioBean = crearNuevoCalendario(context);
                actualizarCalendario(context, calendarioBean);
            }
            return calendarioBean;
        } catch (FileNotFoundException | NullPointerException e) {
            // El archivo no existe, se crea un nuevo Calendario
            CalendarioBean calendarioBean = crearNuevoCalendario(context);
            actualizarCalendario(context, calendarioBean);
            return calendarioBean;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CalendarioBean crearNuevoCalendario(Context context) {
        // Obtenemos recetas
        CalendarioBean calendarioBean = new CalendarioBean();
        colaRecetas = new ArrayDeque<>();
        colaRecetas.addAll(RecetasSrv.cargarListaRecetas(context));
        // Obtener la fecha actual
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);
        if (diaActual != Calendar.SUNDAY) {
            // Recorrer los días de la semana y agregar dos recetas para cada día
            for (int i = diaActual; i <= Calendar.SATURDAY; i++) {
                calendar.set(Calendar.DAY_OF_WEEK, i);

                // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
                DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
                diaRecetas.addReceta(obtenerReceta(context, calendar.getTimeInMillis()));
                diaRecetas.addReceta(obtenerReceta(context, calendar.getTimeInMillis()));

                // Agregar el objeto DiaRecetas a la lista
                calendarioBean.getListaRecetas().add(diaRecetas);
            }
        }

        // Domingo da igual que dia de la semana es que al ser el ultimo día hay que meterlo:
        calendar.add(Calendar.DAY_OF_WEEK, 1);

        // Crear un objeto DiaRecetas con la fecha y las recetas correspondientes
        DiaRecetas diaRecetas = new DiaRecetas(calendar.getTime());
        diaRecetas.addReceta(obtenerReceta(context, calendar.getTimeInMillis()));
        diaRecetas.addReceta(obtenerReceta(context, calendar.getTimeInMillis()));

        // Agregar el objeto DiaRecetas a la lista
        calendarioBean.getListaRecetas().add(diaRecetas);

        // Guardar la lista de recetas actualizada en el archivo JSON
        RecetasSrv.guardarListaRecetas(context, new ArrayList<>(colaRecetas));
        return calendarioBean;
    }

    private static String obtenerReceta(Context context, long tiempoActual) {
        // Obtener la primera receta de la cola
        Receta receta = colaRecetas.poll();
        if (receta == null) {
            return "-1";
        }
        long diasLimite = ConfiguracionSrv.getDiasRepeticionReceta(context);
        Receta recetaElegida = receta;
        do {
            // Agregar la receta al final de la cola
            colaRecetas.offer(recetaElegida);
            // Calcular la diferencia en milisegundos entre la fecha actual y 'tuFecha'
            assert recetaElegida != null;
            long diferenciaEnMilisegundos = tiempoActual - recetaElegida.getFechaCalendario().getTime();
            // Convertir la diferencia en milisegundos a días
            long diasPasados = diferenciaEnMilisegundos / (1000 * 60 * 60 * 24);
            Temporada temporada = UtilsSrv.getTemporadaFecha(new Date(tiempoActual));
            if (diasPasados > diasLimite && recetaElegida.getTemporadas().contains(temporada) && !recetaElegida.isPostre()) {
                recetaElegida.setFechaCalendario(new Date(tiempoActual));
                return recetaElegida.getId();
            } else {
                recetaElegida = colaRecetas.poll();
            }
        }
        while (recetaElegida != receta);
        colaRecetas.offer(recetaElegida);
        // No se ha encontrado receta valida para el dia, devolver -1
        return "-1";
    }


    public static void actualizarCalendario(Context context, CalendarioBean calendario) {
        // Realizar la lógica de actualización del Calendario
        // Establecer la última actualización con la fecha y hora actual
        Toast.makeText(context, context.getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
        calendario.setUltimaActualizacion(System.currentTimeMillis());

        // Guardar el Calendario en "calendario.json"
        try {
            // Convertir la lista de objetos Receta a JSON utilizando GSON
            Gson gson = new Gson();
            String json = gson.toJson(calendario);

            // Guardar el JSON en el almacenamiento interno
            FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void eliminarReceta(Context context, Receta receta) {
        CalendarioBean calendario = cargarCalendario(context);
        assert calendario != null;
        colaRecetas = new ArrayDeque<>();
        colaRecetas.addAll(RecetasSrv.cargarListaRecetas(context));
        for (DiaRecetas dia : calendario.getListaRecetas()) {
            if (dia.getRecetas().contains(receta.getId())) {
                dia.getRecetas().remove(receta.getId());
                dia.addReceta(obtenerReceta(context, dia.getFecha().getTime()));
            }
        }
        actualizarCalendario(context, calendario);
        // Guardar la lista de recetas actualizada en el archivo JSON
        RecetasSrv.guardarListaRecetas(context, new ArrayList<>(colaRecetas));
    }

    public static void addReceta(Context context) {
        CalendarioBean calendario = cargarCalendario(context);
        assert calendario != null;
        colaRecetas = new ArrayDeque<>();
        colaRecetas.addAll(RecetasSrv.cargarListaRecetas(context));
        for (DiaRecetas dia : calendario.getListaRecetas()) {
            if (dia.getRecetas().contains("-1")) {
                String id = obtenerReceta(context, dia.getFecha().getTime());
                dia.getRecetas().remove("-1");
                dia.addReceta(id);
                RecetasSrv.actualizarFechaCalendario(context, id);
            }
        }
        // Guardar la lista de recetas actualizada en el archivo JSON
        RecetasSrv.guardarListaRecetas(context, new ArrayList<>(colaRecetas));
    }
}
