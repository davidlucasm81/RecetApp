package com.david.recetapp.negocio.beans;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Receta implements Parcelable {


    private final String id;
    private String nombre;
    private List<Temporada> temporadas;
    private List<Ingrediente> ingredientes;
    private List<Paso> pasos;
    private List<Alergeno> alergenos;
    private float estrellas;
    private int numPersonas;
    private Date fechaCalendario;
    private boolean shared;
    private boolean postre;
    private double puntuacionDada;

    public Receta() {
        this.id = UUID.randomUUID().toString();
        this.nombre = "";
        this.temporadas = new ArrayList<>();
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
        this.alergenos = new ArrayList<>();
        this.estrellas = 0;
        this.numPersonas = -1;
        this.fechaCalendario = null;
        this.shared = false;
        this.postre = false;
        this.puntuacionDada = -1;
    }

    protected Receta(Parcel in) {
        id = in.readString();
        nombre = in.readString();

        // Temporadas
        int seasonCount = in.readInt();
        temporadas = new ArrayList<>();
        for (int i = 0; i < seasonCount; i++) {
            int ord = in.readInt();
            temporadas.add(Temporada.values()[ord]);
        }

        // Ingredientes & Pasos
        ingredientes = in.createTypedArrayList(Ingrediente.CREATOR);
        pasos        = in.createTypedArrayList(Paso.CREATOR);

        // Alergenos
        List<Alergeno> algList = in.createTypedArrayList(Alergeno.CREATOR);
        assert algList != null;
        alergenos = new ArrayList<>(algList);

        estrellas       = in.readFloat();
        numPersonas     = in.readInt();
        long ts         = in.readLong();
        fechaCalendario = (ts != -1L) ? new Date(ts) : null;
        shared          = in.readByte() != 0;
        postre          = in.readByte() != 0;
        puntuacionDada  = in.readDouble();
    }

    public static final Creator<Receta> CREATOR = new Creator<>() {
        @Override
        public Receta createFromParcel(Parcel in) {
            return new Receta(in);
        }

        @Override
        public Receta[] newArray(int size) {
            return new Receta[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(nombre);

        // Temporadas
        dest.writeInt(temporadas.size());
        for (Temporada t : temporadas) {
            dest.writeInt(t.ordinal());
        }

        // Ingredientes & Pasos
        dest.writeTypedList(ingredientes);
        dest.writeTypedList(pasos);

        // Alergenos
        dest.writeTypedList(new ArrayList<>(alergenos));

        dest.writeFloat(estrellas);
        dest.writeInt(numPersonas);
        dest.writeLong(fechaCalendario != null ? fechaCalendario.getTime() : -1L);
        dest.writeByte((byte) (shared ? 1 : 0));
        dest.writeByte((byte) (postre ? 1 : 0));
        dest.writeDouble(puntuacionDada);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters, setters y lógica de puntuación idéntica a la versión Serializable

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Temporada> getTemporadas() {
        return temporadas;
    }

    public void setTemporadas(List<Temporada> temporadas) {
        this.temporadas = temporadas;
    }

    public List<Ingrediente> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(List<Ingrediente> ingredientes) {
        this.ingredientes = ingredientes;
    }


    public List<Paso> getPasos() {
        return pasos;
    }

    public void setPasos(List<Paso> pasos) {
        this.pasos = pasos;
    }

    public float getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(float estrellas) {
        this.estrellas = estrellas;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
    }

    public Date getFechaCalendario() {
        return fechaCalendario;
    }

    public void setFechaCalendario(Date fechaCalendario) {
        this.fechaCalendario = fechaCalendario;
    }

    public String getId() {
        return id;
    }

    public List<Alergeno> getAlergenos() {
        return alergenos;
    }

    public void setAlergenos(List<Alergeno> alergenos) {
        this.alergenos = alergenos;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isPostre() {
        return postre;
    }

    public void setPostre(boolean postre) {
        this.postre = postre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Receta receta = (Receta) o;
        return id.equals(receta.id) && Objects.equals(nombre, receta.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public double getPuntuacionDada() {
        return puntuacionDada;
    }

    public void setPuntuacionDada(double puntuacionDada){
        this.puntuacionDada = puntuacionDada;
    }

}
