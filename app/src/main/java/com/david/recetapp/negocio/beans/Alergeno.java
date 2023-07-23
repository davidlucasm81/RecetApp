package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Alergeno implements Serializable {
    private final String nombre;
    private final int numero;

    public Alergeno(String nombre, int numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public int getNumero() {
        return numero;
    }

}