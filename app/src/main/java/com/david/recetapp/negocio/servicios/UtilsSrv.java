package com.david.recetapp.negocio.servicios;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.david.recetapp.R;
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
}
