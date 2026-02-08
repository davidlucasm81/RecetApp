package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Day implements Serializable {
    private int dayOfMonth;
    private List<RecetaDia> recetas;

    public Day(int dayOfMonth, List<RecetaDia> recetas) {
        this.dayOfMonth = dayOfMonth;
        this.recetas = recetas;
        if(this.recetas == null)
            this.recetas = new ArrayList<>();
    }

    public Day(){

    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    // Getter y setter)

    public List<RecetaDia> getRecetas() {
        return recetas;
    }

    public void setRecetas(List<RecetaDia> recetas) {
        this.recetas = recetas;
    }

    public void removeReceta(String id) {
        recetas.removeIf(r -> r.getIdReceta().equals(id));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Day other = (Day) obj;
        return dayOfMonth == other.dayOfMonth && recetas.equals(other.recetas);
    }
}
