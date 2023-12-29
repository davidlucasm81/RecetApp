package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Day implements Serializable {
    private final int dayOfMonth;
    private List<Receta> recetas;

    public Day(int dayOfMonth, List<Receta> recetas) {
        this.dayOfMonth = dayOfMonth;
        this.recetas = recetas;
        if(this.recetas == null)
            this.recetas = new ArrayList<>();
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public List<Receta> getRecetas() {
        return recetas;
    }

    public void setRecetas(List<Receta> recetas) {
        this.recetas = recetas;
    }
}
