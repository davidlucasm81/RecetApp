package com.david.recetapp.negocio.servicios;

import com.david.recetapp.R;

public class AlergenosSrv {

    public static int obtenerImagen(int numero) {
        return switch (numero) {
            case 0 -> R.drawable.ic_gluten;
            case 1 -> R.drawable.ic_lacteos;
            case 2 -> R.drawable.ic_cacahuetes;
            case 3 -> R.drawable.ic_soja;
            case 4 -> R.drawable.ic_pescado;
            case 5 -> R.drawable.ic_mariscos;
            case 6 -> R.drawable.ic_huevos;
            default -> R.drawable.alergeno_placeholder;
        };
    }
}
