package com.david.recetapp.negocio.beans;

import android.content.Context;

import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
        colaRecetas.addAll(RecetasSrv.obtenerRecetas(context,diasLimite));
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
        RecetasSrv.guardarListaRecetas(context, new ArrayList<>(colaRecetas));
    }

    public List<DiaRecetas> getListaRecetas() {
        return calendarioBean.getListaRecetas();
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

    public CalendarioBean getCalendarioBean() {
        return calendarioBean;
    }

    public void setCalendarioBean(CalendarioBean calendarioBean) {
        this.calendarioBean = calendarioBean;
    }

}
