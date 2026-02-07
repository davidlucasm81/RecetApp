package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class RecetaDia implements Serializable {

    private String idReceta;
    private int numeroPersonas;

    public RecetaDia(String idReceta, int numeroPersonas) {
        this.idReceta = idReceta;
        this.numeroPersonas = numeroPersonas;
    }

    public String getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(String idReceta) {
        this.idReceta = idReceta;
    }

    public int getNumeroPersonas() {
        return numeroPersonas;
    }

    public void setNumeroPersonas(int numeroPersonas) {
        this.numeroPersonas = numeroPersonas;
    }
}
