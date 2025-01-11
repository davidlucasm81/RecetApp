package com.david.recetapp.negocio.servicios;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Temporada;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UtilsSrv {

    // Devuelve la temporada dado un día de la semana
    public static Temporada getTemporadaFecha(LocalDate date) {
        // Obtener el mes de la fecha
        int month = date.getMonthValue(); // Enero = 1, Diciembre = 12

        // Mapear el mes a la temporada
        if (month == 12 || month <= 2) {
            return Temporada.INVIERNO;
        } else if (month <= 5) {
            return Temporada.PRIMAVERA;
        } else if (month <= 8) {
            return Temporada.VERANO;
        } else {
            return Temporada.OTONIO;
        }
    }

    @SuppressLint("RestrictedApi")
    public static Snackbar notificacion(Context context, String mensaje, int duracion) {
        // Crear un Snackbar con el mensaje
        View view = ((Activity) context).findViewById(android.R.id.content); // Vista raíz de la actividad
        Snackbar snackbar = Snackbar.make(view, "", duracion); // No mostramos el mensaje aquí porque lo personalizamos

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
        tv_toast.setGravity(Gravity.CENTER_VERTICAL);
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

        // Configurar el diseño personalizado en el Snackbar
        snackbarView.setBackgroundColor(Color.TRANSPARENT); // Hacer el fondo del Snackbar transparente
        snackbarView.setPadding(0, 0, 0, 0); // Eliminar el padding predeterminado

        // Añadir el diseño personalizado
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM; // Centrar horizontalmente, posición inferior
        layout.setLayoutParams(params);

        ((Snackbar.SnackbarLayout) snackbarView).addView(layout);

        // Retornar el Snackbar para que lo puedas mostrar
        return snackbar;
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static String capitalizeAndAddPeriod(String input) {
        if (input == null || input.isEmpty()) {
            return input; // Devolver el String sin modificar si es nulo o vacío
        }

        // Convertir la primera letra a mayúscula y mantener el resto igual
        String result = input.substring(0, 1).toUpperCase() + input.substring(1);

        // Verificar si el texto ya tiene un punto al final y agregar uno si no lo tiene
        if (!result.endsWith(".")) {
            result += ".";
        }

        return result;
    }

    public static boolean esNumeroEnteroOFraccionValida(String numero) {
        // Expresión regular para validar una fracción (numerador/denominador)
        String fraccionRegex = "^(-?\\d+)/(\\d+)$";

        // Si es una fracción, validar que el denominador no sea cero
        if (numero.matches(fraccionRegex)) {
            String[] partes = numero.split("/");
            int denominador = Integer.parseInt(partes[1]);
            return denominador != 0;
        }

        // Expresión regular para validar un número entero o decimal
        String numeroRegex = "^-?\\d*(\\.\\d+)?$";

        // Verificar si el número coincide con la expresión regular de un número entero o decimal
        return numero.matches(numeroRegex);
    }

    public static Double convertirNumero(String numero) {
        try {
            // Manejo de fracciones en formato "numerador/denominador"
            if (numero.contains("/")) {
                String[] partes = numero.split("/");
                if (partes.length == 2) {
                    int numerador = Integer.parseInt(partes[0].trim());
                    int denominador = Integer.parseInt(partes[1].trim());
                    return denominador != 0 ? (double) numerador / denominador : -1.0;
                }
            }
            // Manejo de enteros y decimales con punto o coma
            return Double.parseDouble(numero.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            // Valor no válido
            return -1.0;
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
        int diaActual = fechaActual.getDayOfMonth();
        int ultimoDiaMes = fechaActual.lengthOfMonth();

        // Usar un Stream optimizado para generar el rango de días
        return IntStream.rangeClosed(diaActual, ultimoDiaMes).boxed().collect(Collectors.toSet());
    }

    public static int obtenerColumnaCalendario(int diaDelMes) {
        // Obtener la fecha actual
        LocalDate fechaActual = LocalDate.now();

        // Crear una fecha con el día proporcionado, mes actual y año actual
        LocalDate fecha = fechaActual.withDayOfMonth(diaDelMes);

        // Obtener el día de la semana (1 = Lunes, 7 = Domingo)
        int diaDeLaSemana = fecha.getDayOfWeek().getValue();

        // Convertir el día de la semana a la columna correspondiente
        // Lunes = 0, Domingo = 6
        return (diaDeLaSemana == 7) ? 6 : diaDeLaSemana - 1;
    }

}
