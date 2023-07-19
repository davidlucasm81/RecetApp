package com.david.recetapp.actividades;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.RecetasDiaExpandableListAdapter;
import com.david.recetapp.negocio.beans.DiaRecetas;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecetasDiaActivity extends AppCompatActivity {
    private ExpandableListView expandableListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recetas_dia_activity);

        expandableListView = findViewById(R.id.expandableListView);

        // Obtiene índice del día seleccionado del Intent
        DiaRecetas diaRecetas = (DiaRecetas) getIntent().getSerializableExtra("diaRecetas");

        TextView textViewEmpty = findViewById(R.id.textViewEmpty);

        if (diaRecetas.getRecetas().isEmpty() || diaRecetas.getRecetas().stream().allMatch(s -> s.equals("-1"))) {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        } else {
            textViewEmpty.setVisibility(View.GONE); // Oculta el TextView si la lista no está vacía
        }

        // Obten el TextView del título
        TextView tituloTextView = findViewById(R.id.tituloTextView);

        String diaSemana = obtenerDiaSemana(diaRecetas.getFecha());
        tituloTextView.setText(diaSemana);
        List<Receta> listaRecetas = cargarListaRecetas().stream().filter(r -> diaRecetas.getRecetas().contains(r.getId())).collect(Collectors.toList());
        if(listaRecetas.isEmpty()){
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        }
        RecetasDiaExpandableListAdapter expandableListAdapter = new RecetasDiaExpandableListAdapter(this, listaRecetas, expandableListView);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
            return true;
        });
    }

    // Método para obtener el nombre del día de la semana
    private String obtenerDiaSemana(Date fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(fecha);
    }

    private List<Receta> cargarListaRecetas() {
        List<Receta> listaRecetas = new ArrayList<>();

        try {
            // Cargar el archivo JSON desde el almacenamiento interno
            FileInputStream fis = openFileInput("lista_recetas.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }

            br.close();
            isr.close();
            fis.close();

            // Convertir el JSON a una lista de objetos Receta utilizando GSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Receta>>() {
            }.getType();
            listaRecetas = gson.fromJson(jsonBuilder.toString(), listType);
        } catch (FileNotFoundException e) {
            // Si el archivo no existe, se crea una nueva lista vacía
            listaRecetas = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listaRecetas;
    }
}