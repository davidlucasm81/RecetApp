package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiaRecetas implements Serializable {
    private final Date fecha;
    private final List<String> recetas; //IDs de las recetas

    private final List<IngredienteDia> ingredientesDias;

    public DiaRecetas(Date fecha) {
        this.fecha = fecha;
        this.recetas = new ArrayList<>();
        this.ingredientesDias = new ArrayList<>();
    }

    public Date getFecha() {
        return fecha;
    }

    public List<String> getRecetas() {
        return recetas;
    }

    public List<IngredienteDia> getIngredientesDias() {
        return ingredientesDias;
    }

    public void addReceta(Receta receta) {
        if (receta == null) {
            this.recetas.add("-1");
            return;
        }
        this.recetas.add(receta.getId());
        for (Ingrediente ingrediente : receta.getIngredientes()) {
            IngredienteDia ingredienteDia = new IngredienteDia();
            ingredienteDia.setIngrediente(ingrediente);
            ingredienteDia.setComprado(false);
            ingredientesDias.add(ingredienteDia);
        }
    }
}