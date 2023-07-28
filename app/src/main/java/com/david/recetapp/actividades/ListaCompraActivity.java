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

import java.util.List;
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
                ListaCompraTodosIngredientesAdapter adapter = new ListaCompraTodosIngredientesAdapter(ingredientes);

                // Configurar el RecyclerView con el adaptador
                recyclerView.setAdapter(adapter);
            } else {
                textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
            }
        });

        btnListaCompraPorDias.setOnClickListener(v -> {
            btnListaCompraTotal.setEnabled(true);
            btnListaCompraPorDias.setEnabled(false);
            // TODO: Agregar aquí el código para mostrar la lista de compra por días
            Map<String, List<Ingrediente>> ingredientes = CalendarioSrv.obtenerIngredientesListaCompraDias(getApplicationContext(), calendario);
            if (calendario != null && !ingredientes.isEmpty()) {
                textViewEmpty.setVisibility(View.GONE);

                RecyclerView recyclerView = findViewById(R.id.recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                // Crear e inicializar el adaptador
                ListaCompraPorDiaIngredientesAdapter adapter = new ListaCompraPorDiaIngredientesAdapter(ingredientes);

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
            ListaCompraTodosIngredientesAdapter adapter = new ListaCompraTodosIngredientesAdapter(ingredientes);

            // Configurar el RecyclerView con el adaptador
            recyclerView.setAdapter(adapter);
        } else {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        }
    }
}
