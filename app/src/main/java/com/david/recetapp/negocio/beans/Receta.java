package com.david.recetapp.negocio.beans;

import androidx.collection.ArraySet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Receta implements Serializable {
    private final String id;
    private String nombre;
    private Set<Temporada> temporadas;
    private List<Ingrediente> ingredientes;
    private List<Paso> pasos;

    private List<Alergeno> alergenos;

    private float estrellas;

    private Date fechaCalendario;

    private boolean shared;

    private boolean postre;

    public Receta() {
        this.id = UUID.randomUUID().toString();
        this.nombre = "";
        this.temporadas = new ArraySet<>();
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
        this.estrellas = 0;
        this.fechaCalendario = null;
        this.alergenos = new ArrayList<>();
        this.shared = false;
        this.postre = false;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<Temporada> getTemporadas() {
        return temporadas;
    }

    public void setTemporadas(Set<Temporada> temporadas) {
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

    public String getId() {
        return id;
    }

    public List<Alergeno> getAlergenos() {
        return alergenos;
    }

    public void setAlergenos(List<Alergeno> alergenos) {
        this.alergenos = alergenos;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isPostre() {
        return postre;
    }

    public void setPostre(boolean postre) {
        this.postre = postre;
    }
}
