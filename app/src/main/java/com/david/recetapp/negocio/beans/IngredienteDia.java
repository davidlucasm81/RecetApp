package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class IngredienteDia implements Serializable {
    public Ingrediente ingrediente;

    public boolean isComprado() {
        return comprado;
    }

    public void setComprado(boolean comprado) {
        this.comprado = comprado;
    }

    public boolean comprado;

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public IngredienteDia() {
        this.ingrediente = null;
        this.comprado = false;
    }
}