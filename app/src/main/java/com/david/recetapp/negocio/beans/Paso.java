package com.david.recetapp.negocio.beans;

import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.io.Serializable;

public class Paso implements Serializable {
    private String tiempo;
    private String paso;

    public Paso(String paso, String tiempo) {
        this.paso = UtilsSrv.capitalizeAndAddPeriod(paso);
        this.tiempo = tiempo;
    }

    public String getTiempo() {
        return tiempo;
    }

    public String getPaso() {
        return paso;
    }

    public void setTiempo(String tiempo) {
        this.tiempo = tiempo;
    }

    public void setPaso(String paso) {
        this.paso = UtilsSrv.capitalizeAndAddPeriod(paso);
    }
}
