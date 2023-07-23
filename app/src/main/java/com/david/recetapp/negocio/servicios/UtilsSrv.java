package com.david.recetapp.negocio.servicios;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UtilsSrv {

    // Método para obtener el nombre del día de la semana
    public static String obtenerDiaSemana(Date fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(fecha);
    }
}
