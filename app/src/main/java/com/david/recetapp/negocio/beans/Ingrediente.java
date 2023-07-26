package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Ingrediente implements Serializable {
    private final String nombre;

    private int cantidad;

    private final String tipoCantidad;

    public Ingrediente(String nombre, int cantidad, String tipoCantidad) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.tipoCantidad = tipoCantidad;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipoCantidad() {
        return tipoCantidad;
    }


}
