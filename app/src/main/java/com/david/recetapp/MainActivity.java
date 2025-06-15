package com.david.recetapp;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.david.recetapp.fragments.CalendarioFragment;
import com.david.recetapp.fragments.ListaCompraFragment;
import com.david.recetapp.fragments.RecetasFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Manejo moderno del botón atrás
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // No hacer nada, el usuario permanece en la pestaña actual
            }
        });

        // Referencia a los botones
        ImageButton btnVerRecetas = findViewById(R.id.btnVerRecetas);
        ImageButton btnCalendario = findViewById(R.id.btnCalendario);
        ImageButton btnListaCompra = findViewById(R.id.btnListaCompra);

        // Verifica si se debe cargar un fragmento específico desde el Intent
        if (savedInstanceState == null) {
            String fragmentToLoad = getIntent().getStringExtra("FRAGMENT_TO_LOAD");

            if ("CalendarioFragment".equals(fragmentToLoad)) {
                cargarFragmento(new CalendarioFragment());
                marcarBotonSeleccionado(btnCalendario);
            } else {
                cargarFragmento(new RecetasFragment()); // Cargar el fragmento inicial (Recetas)
                marcarBotonSeleccionado(btnVerRecetas);
            }
        }

        // Configurar los listeners para cambiar entre fragments
        btnVerRecetas.setOnClickListener(v -> {
            cargarFragmento(new RecetasFragment());
            marcarBotonSeleccionado(btnVerRecetas);
        });

        btnCalendario.setOnClickListener(v -> {
            cargarFragmento(new CalendarioFragment());
            marcarBotonSeleccionado(btnCalendario);
        });

        btnListaCompra.setOnClickListener(v -> {
            cargarFragmento(new ListaCompraFragment());
            marcarBotonSeleccionado(btnListaCompra);
        });
    }

    // Carga un fragmento en el contenedor
    private void cargarFragmento(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    // Destacar el botón seleccionado
    private void marcarBotonSeleccionado(ImageButton botonSeleccionado) {
        findViewById(R.id.btnVerRecetas).setEnabled(true);
        findViewById(R.id.btnCalendario).setEnabled(true);
        findViewById(R.id.btnListaCompra).setEnabled(true);

        botonSeleccionado.setEnabled(false); // Deshabilita el botón seleccionado
    }
}
