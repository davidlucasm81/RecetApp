package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class Alergeno implements Serializable {
    private String nombre;
    private int numero;

    public Alergeno(String nombre, int numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }
}