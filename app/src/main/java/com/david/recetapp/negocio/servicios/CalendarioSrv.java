package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class CalendarioSrv {
    private static final int LIMITE_DIAS = 3;
    public static List<Day> obtenerCalendario(Activity activity) {
        List<Day> days;
        // Cargamos el calendario
        SharedPreferences preferences = activity.getSharedPreferences("shared_calendar_prefs",Context.MODE_PRIVATE);
        String savedCalendarJson = preferences.getString("calendario", null);
        if (savedCalendarJson != null) {
            // Si el calendario existe, lo cargamos
            Type listType = new TypeToken<List<Day>>() {
            }.getType();
            days = new Gson().fromJson(savedCalendarJson, listType);

            // Comprobamos si el calendario es del mes actual
            if (!isCurrentMonthSaved(preferences)) {
                // Si es del mes actual entonces creamos uno nuevo
                days = generateDays(activity);
                saveCalendarToSharedPreferences(activity,days);
            }
        } else {
            // Si no existe, creamos uno nuevo
            days = generateDays(activity);
            saveCalendarToSharedPreferences(activity,days);
        }
        return days;
    }

    private static List<Day> generateDays(Activity activity) {
        List<Day> days = new ArrayList<>();

        // Obtenemos el mes y el año
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        // Ponemos el calendario al primer dia del mes
        calendar.set(currentYear, currentMonth, 1);

        // Obtenemos los dias del mes
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Generamos los Days para el mes actual sin recetas
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new Day(i, new ArrayList<>()));
        }

        // Guardamos el calendario
        saveCalendarToSharedPreferences(activity,days);

        return days;
    }

    private static boolean isCurrentMonthSaved(SharedPreferences preferences) {
        // Obtenemos el mes y el año almacenado
        int savedMonth = preferences.getInt("savedMonth", -1);
        int savedYear = preferences.getInt("savedYear", -1);

        // Obtenemos el mes y el año actual
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        // Comparamos si som iguales
        return savedMonth == currentMonth && savedYear == currentYear;
    }

    private static void saveCalendarToSharedPreferences(Activity activity, List<Day> days) {
        // Almacenamos el calendario como un JSON
        SharedPreferences preferences = activity.getSharedPreferences("shared_calendar_prefs",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String calendarJson = new Gson().toJson(days);
        editor.putString("calendario", calendarJson);

        // Almacenamos el mes y el año en el json
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);
        editor.putInt("savedMonth", currentMonth);
        editor.putInt("savedYear", currentYear);

        editor.apply();
        UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_actualizado), Toast.LENGTH_SHORT).show();
    }

    public static void actualizarDia(Activity activity, Day selectedDay) {
        List<Day> dias = obtenerCalendario(activity);
        for (Day dia : dias) {
            if (dia.getDayOfMonth() == selectedDay.getDayOfMonth()) {
                dia.setRecetas(selectedDay.getRecetas());
                saveCalendarToSharedPreferences(activity, dias);
                RecetasSrv.actualizarRecetasCalendario(activity, dia);
                return;
            }
        }
        UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();

    }

    public static void cargarRecetas(Activity activity) {
        List<Day> calendar = CalendarioSrv.obtenerCalendario(activity);
        List<Receta> recetas = RecetasSrv.cargarListaRecetasCalendario(activity, new ArrayList<>());

        // Convertir la lista a una cola (Queue)
        Queue<Receta> cola = new LinkedList<>(recetas);

        // Conjunto para realizar un seguimiento de las recetas utilizadas recientemente
        Set<Receta> recetasUtilizadasRecientemente = new HashSet<>();

        for (Day dia : calendar) {
            dia.setRecetas(new ArrayList<>());
            if (!esDiaAnteriorAlActual(dia)) {
                // Seleccionar una receta que no se haya utilizado recientemente y no se repita en los próximos 3 días
                addReceta(cola, recetasUtilizadasRecientemente, dia);
                addReceta(cola, recetasUtilizadasRecientemente, dia);
                // Actualizar las recetas del calendario
                RecetasSrv.actualizarRecetasCalendario(activity, dia);

                // Limpiar las recetas utilizadas recientemente después de 3 días
                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, LIMITE_DIAS, dia);
            }
        }

        // Guardar el calendario en las preferencias compartidas
        saveCalendarToSharedPreferences(activity, calendar);
    }

    private static void addReceta(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia) {
        Receta receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia, LIMITE_DIAS);
        if (receta != null) {
            // Marcar la receta como utilizada recientemente
            recetasUtilizadasRecientemente.add(receta);

            // Poner la receta al final de la cola para su uso futuro
            cola.offer(receta);

            // Agregar el ID de la receta al día actual
            dia.getRecetas().add(receta.getId());
        }
    }

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia, int limiteDias) {
        // Copiar la cola original para mantener el estado original
        Queue<Receta> colaOriginal = new LinkedList<>(cola);

        // Obtener y eliminar recetas de la cola hasta encontrar una que no se haya utilizado recientemente y no se repita en los próximos "limiteDias" días
        while (!cola.isEmpty()) {
            Receta receta = cola.poll();
            if (!recetasUtilizadasRecientemente.contains(receta) && !recetaRepetidaEnProximosDias(receta, dia, limiteDias)) {
                // Poner la receta al final de la cola original
                colaOriginal.offer(receta);

                return receta;
            }
        }

        // Restaurar la cola original
        cola.addAll(colaOriginal);
        return null;
    }

    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recetasUtilizadasRecientemente, int limiteDias, Day day) {
        // Limpiar las recetas utilizadas recientemente que tienen más de "limiteDias" días
        Set<Receta> recetasAEliminar = new HashSet<>();
        for (Receta receta : recetasUtilizadasRecientemente) {
            int diasDesdeUltimaUtilizacion = obtenerDiasDesdeUltimaUtilizacion(receta, day);

            if (diasDesdeUltimaUtilizacion >= limiteDias) {
                recetasAEliminar.add(receta);
            }
        }

        // Eliminar las recetas que han superado el límite de días
        recetasUtilizadasRecientemente.removeAll(recetasAEliminar);
    }

    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia, int limiteDias) {
        Date fechaReceta = receta.getFechaCalendario();
        if (fechaReceta != null) {
            Calendar calReceta = Calendar.getInstance();
            calReceta.setTime(fechaReceta);

            for (int i = 1; i <= limiteDias; i++) {
                calReceta.add(Calendar.DAY_OF_MONTH, 1);
                int dayOfMonthFuturo = calReceta.get(Calendar.DAY_OF_MONTH);
                if (dia.getDayOfMonth() + i == dayOfMonthFuturo) {
                    // Verificar si la receta está programada en el día futuro
                    return dia.getRecetas().contains(receta.getId());
                }
            }
        }
        return false;
    }

    private static int obtenerDiasDesdeUltimaUtilizacion(Receta receta, Day day) {
        Date fechaUltimaUtilizacion = receta.getFechaCalendario();
        if (fechaUltimaUtilizacion != null) {
            Calendar calUltimaUtilizacion = Calendar.getInstance();
            calUltimaUtilizacion.setTime(fechaUltimaUtilizacion);

            // Utilizar el día del objeto Day proporcionado
            calUltimaUtilizacion.set(Calendar.DAY_OF_MONTH, day.getDayOfMonth());

            Calendar calDay = Calendar.getInstance();
            calDay.setTime(receta.getFechaCalendario());

            // Calcular la diferencia en días sin contar las horas exactas
            long diferenciaMillis = calUltimaUtilizacion.getTimeInMillis() - calDay.getTimeInMillis();
            int diferenciaDias = (int) (diferenciaMillis / (24 * 60 * 60 * 1000));

            return diferenciaDias;
        }
        return 0;
    }

    public static boolean esDiaAnteriorAlActual(Day day) {
        Calendar calHoy = Calendar.getInstance();
        int dayOfMonthHoy = calHoy.get(Calendar.DAY_OF_MONTH);

        return day.getDayOfMonth() < dayOfMonthHoy;
    }
}
