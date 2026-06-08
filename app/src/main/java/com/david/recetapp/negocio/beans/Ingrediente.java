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

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion) {
        this(nombre, cantidad, tipoCantidad, puntuacion, false);
    }

    public Ingrediente(String nombre, String cantidad, String tipoCantidad, double puntuacion, boolean opcional) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.tipoCantidad = tipoCantidad;
        this.puntuacion = puntuacion;
        this.opcional = opcional;
    }

    protected Ingrediente(Parcel in) {
        nombre = in.readString();
        cantidad = in.readString();
        tipoCantidad = in.readString();
        puntuacion = in.readDouble();
        opcional = in.readByte() != 0;
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
}
