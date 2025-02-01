package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Day implements Serializable {
    private final int dayOfMonth;
    private List<RecetaDia> recetas;

    public Day(int dayOfMonth, List<RecetaDia> recetas) {
        this.dayOfMonth = dayOfMonth;
        this.recetas = recetas;
        if(this.recetas == null)
            this.recetas = new ArrayList<>();
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public List<RecetaDia> getRecetas() {
        return recetas;
    }

    public void setRecetas(List<RecetaDia> recetas) {
        this.recetas = recetas;
    }

    public void removeReceta(String id) {
        recetas.removeIf(r -> r.getIdReceta().equals(id));
    }
}
