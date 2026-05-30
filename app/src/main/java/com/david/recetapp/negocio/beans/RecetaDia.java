package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class RecetaDia implements Serializable {

    private String idReceta;
    private int numeroPersonas;

    public RecetaDia(String idReceta, int numeroPersonas) {
        this.idReceta = idReceta;
        this.numeroPersonas = numeroPersonas;
    }

    public RecetaDia(){

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RecetaDia other = (RecetaDia) obj;
        if (numeroPersonas != other.numeroPersonas) return false;
        if (idReceta == null) return other.idReceta == null;
        return idReceta.equals(other.idReceta);
    }

    @Override
    public int hashCode() {
        int result = (idReceta != null) ? idReceta.hashCode() : 0;
        result = 31 * result + numeroPersonas;
        return result;
    }
}
