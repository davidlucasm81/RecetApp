package com.david.recetapp.negocio.beans;

import java.io.Serializable;

public class NoteItem implements Serializable {
    private String text;
    private boolean isChecked;

    public NoteItem(String text, boolean isChecked) {
        this.text = text;
        this.isChecked = isChecked;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}

