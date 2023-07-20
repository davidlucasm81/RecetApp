package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Paso implements Serializable {
    private final String tiempo;
    private final String paso;

    public Paso(String paso, String tiempo) {
        this.paso = paso;
        this.tiempo = tiempo;
    }

    public String getTiempo() {
        return tiempo;
    }

    public String getPaso() {
        return paso;
    }

}
