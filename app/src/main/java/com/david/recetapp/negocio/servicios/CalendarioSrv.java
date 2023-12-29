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
import java.util.List;
import java.util.stream.Collectors;

public class CalendarioSrv {
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
                List<String> recetasAnteriores = dia.getRecetas();
                dia.setRecetas(selectedDay.getRecetas());
                saveCalendarToSharedPreferences(activity, dias);
                RecetasSrv.actualizarRecetasCalendario(activity, selectedDay.getRecetas().stream().filter(r -> recetasAnteriores.stream().noneMatch(ra -> r.equals(ra))).collect(Collectors.toList()));
                return;
            }
        }
        UtilsSrv.notificacion(activity, activity.getString(R.string.calendario_no_actualizado), Toast.LENGTH_SHORT).show();

    }
}
