package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.adaptadores.IngredienteDiaAdapter;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.util.List;

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
        List<Ingrediente> ingredientes = CalendarioSrv.obtenerIngredientesListaCompra(this, calendario);
        if (calendario != null && !ingredientes.isEmpty()) {
            textViewEmpty.setVisibility(View.GONE);

            RecyclerView recyclerView = findViewById(R.id.recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            // Crear e inicializar el adaptador
            IngredienteDiaAdapter adapter = new IngredienteDiaAdapter(ingredientes);

            // Configurar el RecyclerView con el adaptador
            recyclerView.setAdapter(adapter);
        } else {
            textViewEmpty.setVisibility(View.VISIBLE); // Muestra el TextView si la lista está vacía
        }
    }
}
