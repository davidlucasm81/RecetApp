package com.david.recetapp.negocio.beans;

import android.os.Parcel;
import android.os.Parcelable;

import com.david.recetapp.negocio.servicios.UtilsSrv;

public class Paso implements Parcelable {
    public static final Creator<Paso> CREATOR = new Creator<>() {
        @Override
        public Paso createFromParcel(Parcel in) {
            return new Paso(in);
        }

        @Override
        public Paso[] newArray(int size) {
            return new Paso[size];
        }
    };
    private String tiempo;
    private String paso;

    public Paso(String paso, String tiempo) {
        this.paso = UtilsSrv.capitalizeAndAddPeriod(paso);
        this.tiempo = tiempo;
    }

    protected Paso(Parcel in) {
        tiempo = in.readString();
        paso = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tiempo);
        dest.writeString(paso);
    }

    public String getTiempo() {
        return tiempo;
    }

    public void setTiempo(String tiempo) {
        this.tiempo = tiempo;
    }

    public String getPaso() {
        return paso;
    }

    public void setPaso(String paso) {
        this.paso = UtilsSrv.capitalizeAndAddPeriod(paso);
    }
}
