package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Ingrediente implements Serializable {
    private String nombre;
    private String cantidad;
    private String tipoCantidad;

    public Ingrediente(String nombre, String cantidad, String tipoCantidad) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.tipoCantidad = tipoCantidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipoCantidad() {
        return tipoCantidad;
    }

    public void setTipoCantidad(String tipoCantidad) {
        this.tipoCantidad = tipoCantidad;
    }


}
