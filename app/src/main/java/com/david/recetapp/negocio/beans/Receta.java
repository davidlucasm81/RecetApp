package com.david.recetapp.negocio.beans;

import android.content.Context;

import androidx.collection.ArraySet;

import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private float puntuacionDada;

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
        this.puntuacionDada = -1;
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

    public void setIngredientes(Context context, List<Ingrediente> ingredientes) {
        this.ingredientes = ingredientes;
        setPuntuacionDada(context);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Receta receta = (Receta) o;
        return id.equals(receta.id) && Objects.equals(nombre, receta.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public float getPuntuacionDada() {
        return puntuacionDada;
    }
    public void setPuntuacionDada(Context context){
        String[] ingredientList = context.getResources().getStringArray(R.array.ingredient_list);

        Map<String, Integer> ingredientMap = new HashMap<>();

        for (String s : ingredientList) {
            // Utilizar una expresión regular para encontrar el número al final
            String regex = "(.+) (\\d+)$";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(s.trim());
            if (matcher.find()) {
                // Agregar el nombre y la puntuación al mapa
                ingredientMap.put(matcher.group(1), Integer.parseInt(Objects.requireNonNull(matcher.group(2))));
            }
        }
        this.puntuacionDada = 0;
        int i=1;
        for(Ingrediente ingrediente : this.ingredientes){
            this.puntuacionDada += UtilsSrv.obtenerPuntuacion(ingredientMap,ingrediente.getNombre(), this.puntuacionDada/i);
            i++;
        }
        this.puntuacionDada /=this.ingredientes.size();
    }
}
