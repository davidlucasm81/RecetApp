package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Day implements Serializable {
    private int dayOfMonth;
    private int month;
    private int year;
    private List<RecetaDia> recetas;

    public Day(int dayOfMonth, List<RecetaDia> recetas) {
        this.dayOfMonth = dayOfMonth;
        this.recetas = recetas;
        if(this.recetas == null)
            this.recetas = new ArrayList<>();
        // Valores por defecto para compatibilidad
        Calendar now = Calendar.getInstance();
        this.month = now.get(Calendar.MONTH);
        this.year = now.get(Calendar.YEAR);
    }

    public Day(int dayOfMonth, int month, int year, List<RecetaDia> recetas) {
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.year = year;
        this.recetas = recetas;
        if(this.recetas == null)
            this.recetas = new ArrayList<>();
    }

    public Day(){

    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    @SuppressWarnings("unused")
    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public int getMonth() {
        return month;
    }

    @SuppressWarnings("unused")
    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    @SuppressWarnings("unused")
    public void setYear(int year) {
        this.year = year;
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
