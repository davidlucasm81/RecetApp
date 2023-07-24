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
        return capitalizeFirstLetter(dateFormat.format(fecha));
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

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Obtener la primera letra en mayúscula utilizando Character.toUpperCase()
        String firstLetter = Character.toUpperCase(input.charAt(0)) + "";

        // Obtener el resto del String a partir del segundo carácter
        String restOfString = input.substring(1);

        // Concatenar la primera letra en mayúscula con el resto del String
        return firstLetter + restOfString;
    }

    public static boolean esMismoDia(long fecha1, Calendar fecha2) {
        Calendar calFecha1 = Calendar.getInstance();
        calFecha1.setTimeInMillis(fecha1);

        return calFecha1.get(Calendar.YEAR) == fecha2.get(Calendar.YEAR) &&
                calFecha1.get(Calendar.MONTH) == fecha2.get(Calendar.MONTH) &&
                calFecha1.get(Calendar.DAY_OF_MONTH) == fecha2.get(Calendar.DAY_OF_MONTH);
    }
}
