package com.david.recetapp.negocio.beans;

import java.util.ArrayList;
import java.util.List;

public class CalendarioBean {
    private final List<DiaRecetas> listaRecetas;
    private long ultimaActualizacion;

    public CalendarioBean() {
        this.listaRecetas = new ArrayList<>();
        this.ultimaActualizacion = 0;
    }

    public List<DiaRecetas> getListaRecetas() {
        return listaRecetas;
    }

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }
}
