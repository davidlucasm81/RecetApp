package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Temporada;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        return calFecha1.get(Calendar.YEAR) == fecha2.get(Calendar.YEAR) && calFecha1.get(Calendar.MONTH) == fecha2.get(Calendar.MONTH) && calFecha1.get(Calendar.DAY_OF_MONTH) == fecha2.get(Calendar.DAY_OF_MONTH);
    }

    public static Toast notificacion(Context context, String mensaje, int duracion) {
        // Define new Toast Object
        Toast toast = new Toast(context.getApplicationContext());
        // Create a LinearLayout to hold the icon and text
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8));

        // Create an ImageView for the icon
        ImageView iv_icon = new ImageView(context);
        iv_icon.setImageResource(R.mipmap.icono_app);
        // Set the desired dimensions for the icon (adjust as needed)
        int iconSize = dpToPx(context, 32); // 32dp in this example
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iv_icon.setLayoutParams(iconParams);
        layout.addView(iv_icon);

        // Create TextView Object to set the text
        TextView tv_toast = new TextView(context);
        // Get text from parameter for toast
        tv_toast.setText(mensaje);
        // Set text color (white in this example)
        tv_toast.setTextColor(context.getColor(R.color.colorIcono));
        // Set text size (adjust as needed)
        tv_toast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // 16sp in this example
        // Set text gravity to center
        tv_toast.setGravity(Gravity.CENTER);
        // Set some extra padding between icon and text (adjust as needed)
        tv_toast.setPadding(dpToPx(context, 8), 0, 0, 0);
        // Set the maximum number of lines to 2 (you can change this based on your preference)
        tv_toast.setMaxLines(2);
        // Set ellipsize mode to end (if the text is longer than 2 lines, it will be truncated with "...")
        tv_toast.setEllipsize(TextUtils.TruncateAt.END);
        layout.addView(tv_toast);

        // Set background color and radius for my toast
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(context.getColor(R.color.colorBackground)); // Adjust the RGB values as per your desired color
        gd.setCornerRadius(20);
        // Remove the stroke to have a solid background color
        gd.setStroke(0, Color.TRANSPARENT);
        layout.setBackground(gd);

        // Set duration
        toast.setDuration(duracion);
        // Set the custom layout as the view for the Toast
        toast.setView(layout);

        return toast;
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

    public static SpannableString formatTexto(String texto, String[] palabrasClave) {
        SpannableString spannableTexto = new SpannableString(texto);

        for (String palabraClave : palabrasClave) {
            // Escapar caracteres especiales de la palabra clave para la expresión regular
            String palabraClaveEscaped = Pattern.quote(palabraClave);

            // Crear una expresión regular que coincida solo con palabras completas
            String regex = "\\b" + palabraClaveEscaped + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(texto);

            while (matcher.find()) {
                int startPos = matcher.start();
                int endPos = matcher.end();
                spannableTexto.setSpan(new StyleSpan(Typeface.BOLD), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return spannableTexto;
    }

    public static String sumarStrings(String numero1, String numero2) {
        // Parsear los números para obtener sus valores numéricos
        double valor1 = parsearNumero(numero1);
        double valor2 = parsearNumero(numero2);

        // Realizar la suma
        double resultado = valor1 + valor2;

        // Verificar si el resultado es un número entero
        if (esEntero(resultado)) {
            return String.valueOf((int) resultado); // Convertir a string el número entero
        } else {
            // Si el resultado es una fracción, obtener el numerador y denominador simplificados
            int numerador = (int) (resultado * 10000); // Multiplicar por 10000 para evitar errores de redondeo
            int denominador = 10000; // El denominador será 10000 para obtener 4 decimales

            // Simplificar la fracción dividiendo el numerador y el denominador por su máximo común divisor
            int mcd = obtenerMCD(numerador, denominador);
            numerador /= mcd;
            denominador /= mcd;

            // Devolver el resultado en formato fracción
            return numerador + "/" + denominador;
        }
    }

    private static double parsearNumero(String numero) {
        // Verificar si el número es una fracción en formato "numerador/denominador"
        Pattern patron = Pattern.compile("(\\d+)/(\\d+)");
        Matcher matcher = patron.matcher(numero);
        if (matcher.matches()) {
            int numerador = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            int denominador = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
            return (double) numerador / denominador;
        } else {
            // Si no es una fracción, parsear el número como un double
            return Double.parseDouble(numero);
        }
    }

    private static boolean esEntero(double numero) {
        // Verificar si el número es un entero (sin decimales)
        return numero == Math.floor(numero) && !Double.isInfinite(numero);
    }

    private static int obtenerMCD(int a, int b) {
        // Calcular el máximo común divisor utilizando el algoritmo de Euclides
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
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

    public static int obtenerPuntuacion(Map<String, Integer> ingredientMap, String nombre) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        Optional<Map.Entry<String, Integer>> ingredienteEncontrado = ingredientMap.entrySet().stream()
                .filter(entry -> levenshteinDistance.apply(entry.getKey(), nombre) < 2)
                .max(Comparator.comparingInt(Map.Entry::getValue));

        if (ingredienteEncontrado.isPresent()) {
            return ingredienteEncontrado.get().getValue();
        }
        else{
            return 0;
        }
    }
}
