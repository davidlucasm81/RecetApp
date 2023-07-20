package com.david.recetapp.negocio.servicios;

import com.david.recetapp.R;

public class AlergenosSrv {

    public static int obtenerImagen(int numero) {
        switch (numero) {
            case 0:
                return R.drawable.ic_gluten;
            case 1:
                return R.drawable.ic_lacteos;
            case 2:
                return R.drawable.ic_cacahuetes;
            case 3:
                return R.drawable.ic_soja;
            case 4:
                return R.drawable.ic_pescado;
            case 5:
                return R.drawable.ic_mariscos;
            case 6:
                return R.drawable.ic_huevos;
            default:
                return R.drawable.alergeno_placeholder;
        }
    }
}
