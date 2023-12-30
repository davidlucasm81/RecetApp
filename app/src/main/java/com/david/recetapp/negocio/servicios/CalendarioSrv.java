package com.david.recetapp.negocio.servicios;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.SparseArray;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


public class CalendarioSrv {
    private static final int LIMITE_DIAS = 3;

    public static List<Day> obtenerCalendario(Activity activity) {
        List<Day> days;
        // Cargamos el calendario
        SharedPreferences preferences = activity.getSharedPreferences("shared_calendar_prefs", Context.MODE_PRIVATE);
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
                limpiarRecetasUtilizadasRecientemente(recetasUtilizadasRecientemente, dia);
            }
        }

        // Guardar el calendario en las preferencias compartidas
        saveCalendarToSharedPreferences(activity, calendar);
    }

    private static void addReceta(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia) {
        Receta receta = obtenerRecetaNoRepetida(cola, recetasUtilizadasRecientemente, dia);
        if (receta != null) {
            // Marcar la receta como utilizada recientemente
            recetasUtilizadasRecientemente.add(receta);

            // Poner la receta al final de la cola para su uso futuro
            cola.offer(receta);

            // Agregar el ID de la receta al día actual
            dia.getRecetas().add(receta.getId());
        }
    }

    private static Receta obtenerRecetaNoRepetida(Queue<Receta> cola, Set<Receta> recetasUtilizadasRecientemente, Day dia) {
        // Copiar la cola original para mantener el estado original
        Queue<Receta> colaOriginal = new LinkedList<>(cola);

        // Obtener y eliminar recetas de la cola hasta encontrar una que no se haya utilizado recientemente y no se repita en los próximos "limiteDias" días
        while (!cola.isEmpty()) {
            Receta receta = cola.poll();
            if (!recetasUtilizadasRecientemente.contains(receta)) {
                assert receta != null;
                if (!recetaRepetidaEnProximosDias(receta, dia)) {
                    // Poner la receta al final de la cola original
                    colaOriginal.offer(receta);

                    return receta;
                }
            }
        }

        // Restaurar la cola original
        cola.addAll(colaOriginal);
        return null;
    }

    private static void limpiarRecetasUtilizadasRecientemente(Set<Receta> recetasUtilizadasRecientemente, Day day) {
        // Limpiar las recetas utilizadas recientemente que tienen más de "limiteDias" días
        Set<Receta> recetasAEliminar = new HashSet<>();
        for (Receta receta : recetasUtilizadasRecientemente) {
            int diasDesdeUltimaUtilizacion = obtenerDiasDesdeUltimaUtilizacion(receta, day);

            if (diasDesdeUltimaUtilizacion >= CalendarioSrv.LIMITE_DIAS) {
                recetasAEliminar.add(receta);
            }
        }

        // Eliminar las recetas que han superado el límite de días
        recetasUtilizadasRecientemente.removeAll(recetasAEliminar);
    }

    private static boolean recetaRepetidaEnProximosDias(Receta receta, Day dia) {
        Date fechaReceta = receta.getFechaCalendario();
        if (fechaReceta != null) {
            Calendar calReceta = Calendar.getInstance();
            calReceta.setTime(fechaReceta);

            for (int i = 1; i <= CalendarioSrv.LIMITE_DIAS; i++) {
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

            return (int) (diferenciaMillis / (24 * 60 * 60 * 1000));
        }
        return 0;
    }

    public static boolean esDiaAnteriorAlActual(Day day) {
        Calendar calHoy = Calendar.getInstance();
        int dayOfMonthHoy = calHoy.get(Calendar.DAY_OF_MONTH);

        return day.getDayOfMonth() < dayOfMonthHoy;
    }


    public static String obtenerListaCompraSemana(Activity activity) {
        List<Integer> diasSemana = UtilsSrv.obtenerDiasSemanaActual();
        List<Day> calendario = obtenerCalendario(activity);

        // Cargar las correspondencias de unidades de cantidad a gramos desde el XML
        SparseArray<Integer> unidadesAGramos = cargarUnidadesAGramos(activity.getResources());

        Map<String, BigDecimal> resultado = calendario.stream()
                .filter(d -> diasSemana.contains(d.getDayOfMonth()))
                .flatMap(dia -> RecetasSrv.obtenerRecetasPorId(activity, dia.getRecetas()).stream())
                .flatMap(receta -> receta.getIngredientes().stream())
                .collect(Collectors.groupingBy(
                        Ingrediente::getNombre,
                        Collectors.reducing(BigDecimal.ZERO,
                                ingrediente -> sumarCantidadesAGramos(ingrediente.getCantidad(), ingrediente.getTipoCantidad(), unidadesAGramos, activity.getResources()),
                                BigDecimal::add)
                ));

        return resultado.entrySet().stream()
                .map(entry -> "- " + entry.getValue() + " g " + entry.getKey() + "\n")
                .collect(Collectors.joining());
    }

    private static SparseArray<Integer> cargarUnidadesAGramos(Resources resources) {
        @SuppressLint("UseSparseArrays") SparseArray<Integer> unidadesAGramos = new SparseArray<>();
        String[] unidadesArray = resources.getStringArray(R.array.quantity_units);
        int[] valoresArray = resources.getIntArray(R.array.importance_values);

        for (int i = 0; i < unidadesArray.length; i++) {
            unidadesAGramos.put(i, valoresArray[i]);
        }

        return unidadesAGramos;
    }


    private static BigDecimal sumarCantidadesAGramos(String cantidad, String tipoCantidad, SparseArray<Integer> unidadesAGramos, Resources resources) {
        int factorConversion = unidadesAGramos.get(obtenerIndiceTipoCantidad(tipoCantidad, resources), 1);
        return BigDecimal.valueOf(UtilsSrv.convertirNumero(cantidad)).multiply(BigDecimal.valueOf(factorConversion));
    }

    private static int obtenerIndiceTipoCantidad(String tipoCantidad, Resources resources) {
        String[] unidades =  resources.getStringArray(R.array.quantity_units);
        for (int i = 0; i < unidades.length; i++) {
            if (unidades[i].equals(tipoCantidad)) {
                return i;
            }
        }
        return 0;
    }
}
