package com.david.recetapp.negocio.beans;

import com.david.recetapp.R;

public enum MomentoReceta {
    COMIDA(R.string.comida),
    CENA(R.string.cena),
    AMBOS(R.string.ambos);

    private final int stringRes;

    MomentoReceta(int stringRes) {
        this.stringRes = stringRes;
    }

    @SuppressWarnings("unused")
    public int getStringRes() {
        return stringRes;
    }
}
