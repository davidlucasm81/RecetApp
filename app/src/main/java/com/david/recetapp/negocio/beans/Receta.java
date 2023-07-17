package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Receta implements Serializable {

    private String nombre;
    private List<Temporada> temporadas;
    private List<Ingrediente> ingredientes;
    private List<Paso> pasos;

    private float estrellas;

    private Date fechaCalendario;

    public Receta() {
        this.nombre = "";
        this.temporadas = new ArrayList<>();
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
        this.estrellas = 0;
        this.fechaCalendario = null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Temporada> getTemporadas() {
        return temporadas;
    }

    public void setTemporadas(List<Temporada> temporadas) {
        this.temporadas = temporadas;
    }

    public List<Ingrediente> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(List<Ingrediente> ingredientes) {
        this.ingredientes = ingredientes;
    }

    public List<Paso> getPasos() {
        return pasos;
    }

    public void setPasos(List<Paso> pasos) {
        this.pasos = pasos;
    }

    public float getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(float estrellas) {
        this.estrellas = estrellas;
    }

    public Date getFechaCalendario() {
        return fechaCalendario;
    }

    public void setFechaCalendario(Date fechaCalendario) {
        this.fechaCalendario = fechaCalendario;
    }
}
