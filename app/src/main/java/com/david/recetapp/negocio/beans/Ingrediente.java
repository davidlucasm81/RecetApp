package com.david.recetapp.negocio.beans;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Ingrediente implements Parcelable {
    private String nombre;
    private String cantidad;
    private String tipoCantidad;
    private double puntuacion;
    private boolean opcional;
    private String esSustitutoDe;
    private TipoIngrediente tipo;
    private String recetaId;
    private Receta recetaReferenciada;

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion) {
        this(nombre, cantidad, tipoCantidad, puntuacion, false, null);
    }

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion, boolean opcional) {
        this(nombre, cantidad, tipoCantidad, puntuacion, opcional, null);
    }

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion, boolean opcional, String esSustitutoDe) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.tipoCantidad = tipoCantidad;
        this.puntuacion = puntuacion;
        this.opcional = opcional;
        this.esSustitutoDe = esSustitutoDe;
    }

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion, boolean opcional, String esSustitutoDe, String recetaId) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.tipoCantidad = tipoCantidad;
        this.puntuacion = puntuacion;
        this.opcional = opcional;
        this.esSustitutoDe = esSustitutoDe;
        this.recetaId = recetaId;
    }

    public Ingrediente(Ingrediente other) {
        this.nombre = other.nombre;
        this.cantidad = other.cantidad;
        this.tipoCantidad = other.tipoCantidad;
        this.puntuacion = other.puntuacion;
        this.opcional = other.opcional;
        this.esSustitutoDe = other.esSustitutoDe;
        this.tipo = other.tipo;
        this.recetaId = other.recetaId;
        this.recetaReferenciada = other.recetaReferenciada;
    }

    protected Ingrediente(Parcel in) {
        nombre = in.readString();
        cantidad = in.readString();
        tipoCantidad = in.readString();
        puntuacion = in.readDouble();
        opcional = in.readByte() != 0;
        esSustitutoDe = in.readString();
        String tipoStr = in.readString();
        if (tipoStr != null) {
            tipo = TipoIngrediente.valueOf(tipoStr);
        }
        recetaId = in.readString();
    }

    public Ingrediente() {
        // Constructor vacío requerido por Firestore
    }


    public static final Creator<Ingrediente> CREATOR = new Creator<>() {
        @Override
        public Ingrediente createFromParcel(Parcel in) {
            return new Ingrediente(in);
        }

        @Override
        public Ingrediente[] newArray(int size) {
            return new Ingrediente[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nombre);
        dest.writeString(cantidad);
        dest.writeString(tipoCantidad);
        dest.writeDouble(puntuacion);
        dest.writeByte((byte) (opcional ? 1 : 0));
        dest.writeString(esSustitutoDe);
        dest.writeString(tipo != null ? tipo.name() : null);
        dest.writeString(recetaId);
    }

    // getters and setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipoCantidad() {
        return tipoCantidad;
    }

    public void setTipoCantidad(String tipoCantidad) {
        this.tipoCantidad = tipoCantidad;
    }

    public double getPuntuacion() {
        if (recetaId != null && !recetaId.isEmpty() && recetaReferenciada != null) {
            return recetaReferenciada.getPuntuacionDada();
        }
        return puntuacion;
    }

    public void setPuntuacion(double puntuacion) {
        this.puntuacion = puntuacion;
    }

    public boolean isOpcional() {
        return opcional;
    }

    public void setOpcional(boolean opcional) {
        this.opcional = opcional;
    }

    public String getEsSustitutoDe() {
        return esSustitutoDe;
    }

    public void setEsSustitutoDe(String esSustitutoDe) {
        this.esSustitutoDe = esSustitutoDe;
    }

    public TipoIngrediente getTipo() {
        return tipo;
    }

    public void setTipo(TipoIngrediente tipo) {
        this.tipo = tipo;
    }

    public String getRecetaId() {
        return recetaId;
    }

    public void setRecetaId(String recetaId) {
        this.recetaId = recetaId;
    }

    public void setRecetaReferenciada(Receta recetaReferenciada) {
        this.recetaReferenciada = recetaReferenciada;
    }
}
