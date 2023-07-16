package com.david.recetapp.negocio.beans;

import android.content.Context;

import com.david.recetapp.R;

import java.io.Serializable;

public enum Temporada implements Serializable {

    PRIMAVERA(R.string.primavera),
    VERANO(R.string.verano),
    OTONIO(R.string.otonio),
    INVIERNO(R.string.invierno);

    private final int nombre;

    Temporada(int nombre) {
        this.nombre = nombre;
    }

    public String getNombre(Context context) {
        return context.getString(nombre);
    }
}