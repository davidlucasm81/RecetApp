package com.david.recetapp.actividades;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.NoteAdapter;
import com.david.recetapp.negocio.beans.NoteItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// TODO: Al editar notas no las guarda
// Unirlo a lo de las recetas
// Que al tachar una al salir y volver a entrar lo borre

public class NotasActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notes_prefs";
    private static final String NOTES_KEY = "notes_list";

    private NoteAdapter adapter;
    private List<NoteItem> notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notes = loadNotesFromSharedPreferences();
        if(notes == null){
            notes = new ArrayList<>();
        }

        adapter = new NoteAdapter(notes, position -> {
            notes.remove(position);
            adapter.notifyItemRemoved(position);
            saveNotesToSharedPreferences();
        });

        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            notes.add(new NoteItem("", false)); // Agrega una nota vacía por defecto
            adapter.notifyItemInserted(notes.size() - 1);
            saveNotesToSharedPreferences();
        });
    }

    private void saveNotesToSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(notes);
        editor.putString(NOTES_KEY, json);
        editor.apply();
    }

    private List<NoteItem> loadNotesFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(NOTES_KEY, "");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<NoteItem>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
