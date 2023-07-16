package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public enum Temporada implements Serializable {

    PRIMAVERA("Primavera"),
    VERANO("Verano"),
    OTONIO("Otoño"),
    INVIERNO("Invierno");

    private final String nombre;

    Temporada(String nombre){
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}