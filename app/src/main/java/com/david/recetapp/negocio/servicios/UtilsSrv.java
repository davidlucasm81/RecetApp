package com.david.recetapp.negocio.servicios;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import android.widget.FrameLayout;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Temporada;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UtilsSrv {

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

    public static Snackbar notificacion(Context context, String mensaje, int duracion) {
        // Crear un Snackbar con el mensaje
        View view = ((Activity) context).findViewById(android.R.id.content); // Vista raíz de la actividad
        Snackbar snackbar = Snackbar.make(view, mensaje, duracion);

        // Obtener la vista del Snackbar
        View snackbarView = snackbar.getView();

        // Crear un LinearLayout para personalizar el diseño
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8));

        // Crear un ImageView para el icono
        ImageView iv_icon = new ImageView(context);
        iv_icon.setImageResource(R.mipmap.icono_app); // Ajusta el recurso de tu icono aquí
        int iconSize = dpToPx(context, 32); // Tamaño del icono en dp
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iv_icon.setLayoutParams(iconParams);
        layout.addView(iv_icon);

        // Crear un TextView para el mensaje
        TextView tv_toast = new TextView(context);
        tv_toast.setText(mensaje);
        tv_toast.setTextColor(context.getColor(R.color.colorIcono)); // Color del texto
        tv_toast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Tamaño del texto en sp
        tv_toast.setGravity(Gravity.CENTER);
        tv_toast.setPadding(dpToPx(context, 8), 0, 0, 0); // Padding entre icono y texto
        tv_toast.setMaxLines(2); // Limitar el texto a dos líneas
        tv_toast.setEllipsize(TextUtils.TruncateAt.END); // Truncar el texto si es largo
        layout.addView(tv_toast);

        // Establecer el fondo con color y radio de esquina
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(context.getColor(R.color.colorBackground)); // Color de fondo
        gd.setCornerRadius(20); // Radio de las esquinas
        gd.setStroke(0, Color.TRANSPARENT); // Sin borde
        layout.setBackground(gd);

        // Ahora sustituimos el diseño de la vista del Snackbar con nuestro LinearLayout personalizado
        snackbarView.setPadding(0, 0, 0, 0); // Eliminar el padding predeterminado
        ((FrameLayout) snackbarView).addView(layout, 0); // Añadir el layout personalizado al Snackbar

        // Retornar el Snackbar para que lo puedas mostrar
        return snackbar;
    }


    // Helper method to convert dp to pixels
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public static String capitalizeAndAddPeriod(String input) {
        if (input == null || input.isEmpty()) {
            return input; // Devolver el String sin modificar si es nulo o vacío
        }

        // Convertir la primera letra a mayúscula
        String firstLetter = input.substring(0, 1).toUpperCase();
        String restOfWord = input.substring(1);

        // Verificar si ya hay un punto al final
        boolean hasPeriod = input.charAt(input.length() - 1) == '.';

        // Agregar un punto al final si no se encuentra
        if (!hasPeriod) {
            return firstLetter + restOfWord + ".";
        }

        // Devolver el String original si ya tiene un punto al final
        return firstLetter + restOfWord;
    }

    public static boolean esNumeroEnteroOFraccionValida(String numero) {
        // Verificar si el número es una fracción en formato "numerador/denominador"
        String[] partes = numero.split("/");
        if (partes.length == 2) {
            try {
                Integer.parseInt(partes[0]);
                int denominador = Integer.parseInt(partes[1]);
                return denominador != 0; // Es una fracción válida si el denominador no es cero
            } catch (NumberFormatException e) {
                // No se puede parsear la fracción, no es válida
                return false;
            }
        } else {
            // No es una fracción, verificar si es un número (entero o decimal) válido
            try {
                // Intentar parsear como entero
                Integer.parseInt(numero);
                return true; // Es un número entero válido
            } catch (NumberFormatException e1) {
                try {
                    // Intentar parsear como decimal usando coma o punto como separador decimal
                    Double.parseDouble(numero.replace(',', '.'));
                    return true; // Es un número decimal válido
                } catch (NumberFormatException e2) {
                    // No se puede parsear como entero ni decimal, no es válido
                    return false;
                }
            }
        }
    }

    public static Double convertirNumero(String numero) {
        // Verificar si el número es una fracción en formato "numerador/denominador"
        String[] partes = numero.split("/");
        if (partes.length == 2) {
            try {
                int numerador = Integer.parseInt(partes[0]);
                int denominador = Integer.parseInt(partes[1]);
                if (denominador != 0) {
                    return (double) numerador / denominador;
                } else {
                    return -1.0; // Fracción no válida
                }
            } catch (NumberFormatException e) {
                // No se puede parsear la fracción, retorna null
                return -1.0;
            }
        } else {
            try {
                // Intentar parsear como entero
                return (double) Integer.parseInt(numero);
            } catch (NumberFormatException e1) {
                try {
                    // Intentar parsear como decimal usando coma o punto como separador decimal
                    return Double.parseDouble(numero.replace(',', '.'));
                } catch (NumberFormatException e2) {
                    // No se puede parsear como entero ni decimal, retorna null
                    return -1.0;
                }
            }
        }
    }

    public static double obtenerPuntuacion(Map<String, Integer> ingredientMap, String nombre, double defaultValue) {
        Optional<Map.Entry<String, Integer>> ingredienteEncontrado = ingredientMap.entrySet().stream().filter(entry -> nombre.toLowerCase(Locale.getDefault()).equals(entry.getKey().toLowerCase(Locale.getDefault()))).max(Comparator.comparingInt(Map.Entry::getValue));

        if (ingredienteEncontrado.isPresent()) {
            return ingredienteEncontrado.get().getValue();
        } else {
            return defaultValue;
        }
    }

    public static Set<Integer> obtenerDiasRestantesMes() {
        LocalDate fechaActual = LocalDate.now();
        LocalDate ultimoDiaMes = fechaActual.with(TemporalAdjusters.lastDayOfMonth());
        int diasRestantes = ultimoDiaMes.getDayOfMonth() - fechaActual.getDayOfMonth() + 1;

        return IntStream.iterate(0, i -> i + 1)
                .limit(diasRestantes)
                .mapToObj(fechaActual::plusDays)
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toSet());
    }

    // Método para obtener el nombre del día a partir del día del mes
    public static int obtenerColumnaCalendario(int diaDelMes) {
        // Obtener el mes y año actual del sistema
        Calendar calendario = Calendar.getInstance();
        int mesActual = calendario.get(Calendar.MONTH);
        int anioActual = calendario.get(Calendar.YEAR);

        // Configurar la fecha con el día del mes, mes y año
        calendario.set(anioActual, mesActual, diaDelMes);

        // Obtener el día de la semana
        int diaDeLaSemana = calendario.get(Calendar.DAY_OF_WEEK);

        // Convertir el número del día de la semana a un nombre de día
        int numero = (diaDeLaSemana - 1);
        return (numero == 0) ? 6 : numero - 1;
    }

}
