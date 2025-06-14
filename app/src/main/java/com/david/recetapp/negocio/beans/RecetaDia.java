package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class RecetaDia implements Serializable {

    private final String idReceta;
    private final int numeroPersonas;


    public RecetaDia(String idReceta, int numeroPersonas) {
        this.idReceta = idReceta;
        this.numeroPersonas = numeroPersonas;
    }

    public String getIdReceta() {
        return idReceta;
    }

    public int getNumeroPersonas() {
        return numeroPersonas;
    }
}
