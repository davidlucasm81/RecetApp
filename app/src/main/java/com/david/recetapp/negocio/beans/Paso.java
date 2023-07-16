package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Paso implements Serializable {
    private String tiempo;
    private String paso;
    public Paso(String paso, String tiempo){
        this.paso = paso;
        this.tiempo = tiempo;
    }

    public String getTiempo() {
        return tiempo;
    }

    public void setTiempo(String tiempo) {
        this.tiempo = tiempo;
    }

    public String getPaso() {
        return paso;
    }

    public void setPaso(String paso) {
        this.paso = paso;
    }
}
