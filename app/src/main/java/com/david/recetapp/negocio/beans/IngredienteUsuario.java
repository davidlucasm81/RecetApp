package com.david.recetapp.negocio.beans;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class IngredienteUsuario {
    private String nombre;
    private int puntuacion;

    public IngredienteUsuario() {
        // Requerido por Firestore
    }

    public IngredienteUsuario(String nombre, int puntuacion) {
        this.nombre = nombre;
        this.puntuacion = puntuacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }
}
