package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.adaptadores.ListaCompraPorDiaIngredientesAdapter;
import com.david.recetapp.adaptadores.ListaCompraTodosIngredientesAdapter;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListaCompraActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(ListaCompraActivity.this, MainActivity.class);
        startActivity(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_compra);
        CalendarioBean calendario = CalendarioSrv.cargarCalendario(this);
        TextView textViewEmpty = findViewById(R.id.textViewEmpty);

        Button btnListaCompraTotal = findViewById(R.id.btnListaCompraTotal);
        Button btnListaCompraPorDias = findViewById(R.id.btnListaCompraPorDias);
        String[] units =getResources().getStringArray(R.array.quantity_units);
        int[] importanceValues = getResources().getIntArray(R.array.importance_values);

        Map<String, Integer> unitImportanceMap = new HashMap<>();
        for (int i = 0; i < units.length; i++) {
            unitImportanceMap.put(units[i], importanceValues[i]);
        }

        // Definir el comportamiento al hacer clic en los botones
        btnListaCompraTotal.setOnClickListener(v -> {
            btnListaCompraTotal.setEnabled(false);
            btnListaCompraPorDias.setEnabled(true);

            List<Ingrediente> ingredientes = CalendarioSrv.obtenerIngredientesListaCompraTotal(getApplicationContext(), calendario);
            if (calendario != null && !ingredientes.isEmpty()) {
                textViewEmpty.setVisibility(View.GONE);

                RecyclerView recyclerView = findViewById(R.id.recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                // Crear e inicializar el adaptador
                ListaCompraTodosIngredientesAdapter adapter = new ListaCompraTodosIngredientesAdapter(unitImportanceMap,ingredientes);

                // Configurar el RecyclerView con el adaptador
                recyclerView.setAdapter(adapter);
            } else {
                textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
            }
        });

        btnListaCompraPorDias.setOnClickListener(v -> {
            btnListaCompraTotal.setEnabled(true);
            btnListaCompraPorDias.setEnabled(false);
            Map<String, List<Ingrediente>> ingredientes = CalendarioSrv.obtenerIngredientesListaCompraDias(getApplicationContext(), calendario);
            if (calendario != null && !ingredientes.isEmpty()) {
                textViewEmpty.setVisibility(View.GONE);

                RecyclerView recyclerView = findViewById(R.id.recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                // Crear e inicializar el adaptador
                // Ordenar las claves del mapa
                List<String> diasOrdenados = ordenarDias(ingredientes);

                // Crear un nuevo mapa ordenado utilizando LinkedHashMap para mantener el orden
                LinkedHashMap<String, List<Ingrediente>> ingredientesOrdenados = new LinkedHashMap<>();
                for (String dia : diasOrdenados) {
                    ingredientesOrdenados.put(dia, ingredientes.get(dia));
                }
                ListaCompraPorDiaIngredientesAdapter adapter = new ListaCompraPorDiaIngredientesAdapter(ingredientesOrdenados);

                // Configurar el RecyclerView con el adaptador
                recyclerView.setAdapter(adapter);
            } else {
                textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
            }
        });

        // Inicialmente, el botón de "Lista de Compra Total" estará deshabilitado (seleccionado por defecto)
        btnListaCompraTotal.setEnabled(false);
        btnListaCompraPorDias.setEnabled(true);

        // Comportamiento por defecto
        List<Ingrediente> ingredientes = CalendarioSrv.obtenerIngredientesListaCompraTotal(getApplicationContext(), calendario);
        if (calendario != null && !ingredientes.isEmpty()) {
            textViewEmpty.setVisibility(View.GONE);

            RecyclerView recyclerView = findViewById(R.id.recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            // Crear e inicializar el adaptador
            ListaCompraTodosIngredientesAdapter adapter = new ListaCompraTodosIngredientesAdapter(unitImportanceMap,ingredientes);

            // Configurar el RecyclerView con el adaptador
            recyclerView.setAdapter(adapter);
        } else {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        }
    }

    private List<String> ordenarDias(Map<String, List<Ingrediente>> map) {
        // Definir listas con los nombres de los días en español e inglés
        String[] diasSemanaEsp = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        String[] diasSemanaEng = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        // Obtener el idioma actual o establecer uno predeterminado (español en este caso)
        String idiomaActual = Locale.getDefault().getLanguage();
        boolean esEspanol = idiomaActual.equals("es");

        // Obtener la lista de días correspondiente al idioma actual
        String[] diasSemanaActual = esEspanol ? diasSemanaEsp : diasSemanaEng;

        // Ordenar las claves del mapa de acuerdo al orden de los días de la semana en el idioma actual
        List<String> diasOrdenados = new ArrayList<>(map.keySet());
        diasOrdenados.sort((dia1, dia2) -> {
            int index1 = Arrays.asList(diasSemanaActual).indexOf(dia1);
            int index2 = Arrays.asList(diasSemanaActual).indexOf(dia2);
            return Integer.compare(index1, index2);
        });

        return diasOrdenados;
    }
}
