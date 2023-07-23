package com.david.recetapp.negocio.servicios;

import com.david.recetapp.negocio.beans.Temporada;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UtilsSrv {

    // Método para obtener el nombre del día de la semana
    public static String obtenerDiaSemana(Date fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(fecha);
    }
    // Método para obtener la temporada de un día de la semana
    public static Temporada getTemporadaFecha(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);

        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
            case Calendar.FEBRUARY:
                return Temporada.INVIERNO;
            case Calendar.MARCH:
            case Calendar.APRIL:
            case Calendar.MAY:
                return Temporada.PRIMAVERA;
            case Calendar.JUNE:
            case Calendar.JULY:
            case Calendar.AUGUST:
                return Temporada.VERANO;
            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                return Temporada.OTONIO;
            default:
                return null;
        }
    }
}
