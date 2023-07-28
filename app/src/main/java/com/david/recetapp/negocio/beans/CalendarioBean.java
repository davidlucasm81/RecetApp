package com.david.recetapp.negocio.beans;

import java.util.ArrayList;
import java.util.List;

public class CalendarioBean {
    private final List<DiaRecetas> listaDiaRecetas;
    private long ultimaActualizacion;

    public CalendarioBean() {
        this.listaDiaRecetas = new ArrayList<>();
        this.ultimaActualizacion = 0;
    }

    public List<DiaRecetas> getListaDiaRecetas() {
        return listaDiaRecetas;
    }

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }
}
