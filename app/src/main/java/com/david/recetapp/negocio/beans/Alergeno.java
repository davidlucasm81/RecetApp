package com.david.recetapp.negocio.beans;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class Alergeno implements Parcelable {
    private String nombre;
    private int numero;

    public Alergeno(String nombre, int numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    protected Alergeno(Parcel in) {
        nombre = in.readString();
        numero = in.readInt();
    }

    public static final Creator<Alergeno> CREATOR = new Creator<>() {
        @Override
        public Alergeno createFromParcel(Parcel in) {
            return new Alergeno(in);
        }

        @Override
        public Alergeno[] newArray(int size) {
            return new Alergeno[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nombre);
        dest.writeInt(numero);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alergeno alergeno)) return false;
        return numero == alergeno.numero &&
                Objects.equals(nombre, alergeno.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, numero);
    }

    public Alergeno(){

    }
}
