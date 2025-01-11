package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alergeno)) return false;
        Alergeno alergeno = (Alergeno) o;
        return getNumero() == alergeno.getNumero() && Objects.equals(getNombre(), alergeno.getNombre());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNombre(), getNumero());
    }
}