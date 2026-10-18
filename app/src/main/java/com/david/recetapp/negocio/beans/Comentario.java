package com.david.recetapp.negocio.beans;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("unused")
@IgnoreExtraProperties
public class Comentario implements Serializable {

    private String id;
    private String recetaId;
    private String userId;
    private String userName;
    private String texto;
    private Date fecha;
    private Date fechaEdicion;

    // Constructor vacío requerido por Firestore
    public Comentario() {
        this.id = "";
        this.recetaId = "";
        this.userId = "";
        this.userName = "";
        this.texto = "";
        this.fecha = new Date();
        this.fechaEdicion = null;
    }

    public Comentario(String id, String recetaId, String userId, String userName, String texto, Date fecha) {
        this.id = id;
        this.recetaId = recetaId;
        this.userId = userId;
        this.userName = userName;
        this.texto = texto;
        this.fecha = (fecha != null) ? fecha : new Date();
        this.fechaEdicion = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecetaId() {
        return recetaId;
    }

    public void setRecetaId(String recetaId) {
        this.recetaId = recetaId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Date getFechaEdicion() {
        return fechaEdicion;
    }

    public void setFechaEdicion(Date fechaEdicion) {
        this.fechaEdicion = fechaEdicion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comentario that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
