package com.david.recetapp.negocio.beans;

import android.content.Context;

import androidx.collection.ArraySet;

import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Receta implements Serializable {
    private static final Pattern patternIngredient = Pattern.compile("^(.+)\\s(-?\\d+)$");  // grupo 1 = nombre; grupo 2 = puntuación (pos. o neg.)

    private final String id;
    private String nombre;
    private Set<Temporada> temporadas;
    private List<Ingrediente> ingredientes;
    private List<Paso> pasos;

    private Set<Alergeno> alergenos;

    private float estrellas;

    private int numPersonas;

    private Date fechaCalendario;

    private boolean shared;

    private boolean postre;

    private double puntuacionDada;

    public Receta() {
        this.id = UUID.randomUUID().toString();
        this.nombre = "";
        this.temporadas = new ArraySet<>();
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
        this.estrellas = 0;
        this.numPersonas = -1;
        this.fechaCalendario = null;
        this.alergenos = new HashSet<>();
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

    public void setIngredientes(List<Ingrediente> ingredientes, Context context) {
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

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
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

    public Set<Alergeno> getAlergenos() {
        return alergenos;
    }

    public void setAlergenos(Set<Alergeno> alergenos) {
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

    public double getPuntuacionDada() {
        return puntuacionDada;
    }

    public void setPuntuacionDada(Context context) {
        Map<String, Integer> ingredientMap = new HashMap<>();
        String[] ingredientList = context.getResources().getStringArray(R.array.ingredient_list);

        for (String s : ingredientList) {
            Matcher m = patternIngredient.matcher(s.trim());
            if (m.matches()) {
                // guardamos la clave ya en minúsculas
                ingredientMap.put(Objects.requireNonNull(m.group(1)).toLowerCase(Locale.getDefault()), Integer.parseInt(m.group(2)));
            }
        }

        for (Ingrediente ingredient : this.ingredientes) {
            if (ingredientMap.containsKey(ingredient.getNombre().toLowerCase(Locale.getDefault()))) {
                //noinspection DataFlowIssue
                double puntuacion = ingredientMap.get(ingredient.getNombre().toLowerCase(Locale.getDefault()));
                ingredient.setPuntuacion(puntuacion);
            }
            else{
                ingredient.setPuntuacion(-2);
            }
        }


        String[] units = context.getResources().getStringArray(R.array.quantity_units);
        int[] importanceValues = context.getResources().getIntArray(R.array.importance_values);

        Map<String, Integer> unitImportanceMap = new HashMap<>();
        for (int i = 0; i < units.length; i++) {
            unitImportanceMap.put(units[i], importanceValues[i]);
        }
        double cantidadTotal = this.ingredientes.stream().filter(i -> i.getPuntuacion()>=0).mapToDouble(ingrediente -> {
            double cantidad = (UtilsSrv.esNumeroEnteroOFraccionValida(ingrediente.getCantidad())) ? UtilsSrv.convertirNumero(ingrediente.getCantidad()) : 1;
            //noinspection DataFlowIssue
            double tipoCantidadFactor = unitImportanceMap.getOrDefault(ingrediente.getTipoCantidad(), 1);
            return cantidad * tipoCantidadFactor;
        }).sum();

        this.puntuacionDada = this.ingredientes.stream().filter(i -> i.getPuntuacion()>=0).mapToDouble(ingrediente -> {
            double cantidad = (UtilsSrv.esNumeroEnteroOFraccionValida(ingrediente.getCantidad())) ? UtilsSrv.convertirNumero(ingrediente.getCantidad()) : 1;
            //noinspection DataFlowIssue
            double tipoCantidadFactor = unitImportanceMap.getOrDefault(ingrediente.getTipoCantidad(), 1);

            // Pondera la puntuación del ingrediente más que la cantidad y el tipo de cantidad
            return ingrediente.getPuntuacion() * ((cantidad * tipoCantidadFactor) / cantidadTotal);
        }).sum();

    }

}
