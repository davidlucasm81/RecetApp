package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaRecetas implements Serializable {
    private Date fecha;
    private List<String> recetas; //IDs de las recetas

    public DiaRecetas(Date fecha) {
        this.fecha = fecha;
        this.recetas = new ArrayList<>();
    }

    // Constructor y métodos getters

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public List<String> getRecetas() {
        return recetas;
    }

    public void setRecetas(List<String> recetas) {
        this.recetas = recetas;
    }

    // Método para obtener la fecha formateada como String
    public String getFechaString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(fecha);
    }

    public void addReceta(String idReceta) {
        this.recetas.add(idReceta);
    }
}