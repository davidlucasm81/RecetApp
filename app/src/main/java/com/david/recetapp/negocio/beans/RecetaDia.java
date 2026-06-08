package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class RecetaDia implements Serializable {

    private String idReceta;
    private int numeroPersonas;
    private Map<String, String> ingredientesElegidos; // MainIngredientName -> ChosenIngredientName

    public RecetaDia(String idReceta, int numeroPersonas) {
        this(idReceta, numeroPersonas, new HashMap<>());
    }

    public RecetaDia(String idReceta, int numeroPersonas, Map<String, String> ingredientesElegidos) {
        this.idReceta = idReceta;
        this.numeroPersonas = numeroPersonas;
        this.ingredientesElegidos = ingredientesElegidos != null ? ingredientesElegidos : new HashMap<>();
    }

    public RecetaDia(){

    }

    public String getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(String idReceta) {
        this.idReceta = idReceta;
    }

    public int getNumeroPersonas() {
        return numeroPersonas;
    }

    public void setNumeroPersonas(int numeroPersonas) {
        this.numeroPersonas = numeroPersonas;
    }

    public Map<String, String> getIngredientesElegidos() {
        if (ingredientesElegidos == null) ingredientesElegidos = new HashMap<>();
        return ingredientesElegidos;
    }

    public void setIngredientesElegidos(Map<String, String> ingredientesElegidos) {
        this.ingredientesElegidos = ingredientesElegidos;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RecetaDia other = (RecetaDia) obj;
        return numeroPersonas == other.numeroPersonas &&
                Objects.equals(idReceta, other.idReceta) &&
                Objects.equals(ingredientesElegidos, other.ingredientesElegidos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReceta, numeroPersonas, ingredientesElegidos);
    }
}
