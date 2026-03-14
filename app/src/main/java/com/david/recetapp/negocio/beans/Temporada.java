package com.david.recetapp.negocio.beans;

import androidx.annotation.StringRes;
import com.david.recetapp.R;

public enum Temporada {

    PRIMAVERA(R.string.primavera),
    VERANO(R.string.verano),
    OTONIO(R.string.otonio),
    INVIERNO(R.string.invierno);

    @StringRes
    private final int stringRes;

    Temporada(@StringRes int stringRes) {
        this.stringRes = stringRes;
    }

    @StringRes
    public int getStringRes() {
        return stringRes;
    }
}