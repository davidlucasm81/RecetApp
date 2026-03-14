package com.david.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.david.recetapp.actividades.LoginActivity;
import com.david.recetapp.fragments.CalendarioFragment;
import com.david.recetapp.fragments.ListaCompraFragment;
import com.david.recetapp.fragments.RecetasFragment;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Comprobar sesión ANTES de cargar el layout
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            irALogin();
            return;
        }

        // Asignar userId a los servicios
        RecetasSrv.setUserId(user.getUid());
        CalendarioSrv.setUserId(user.getUid());

        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // No hacer nada
            }
        });

        ImageButton btnVerRecetas = findViewById(R.id.btnVerRecetas);
        ImageButton btnCalendario = findViewById(R.id.btnCalendario);
        ImageButton btnListaCompra = findViewById(R.id.btnListaCompra);
        ImageButton btnLogout = findViewById(R.id.btnLogout); // ← nuevo botón

        if (savedInstanceState == null) {
            String fragmentToLoad = getIntent().getStringExtra("FRAGMENT_TO_LOAD");
            if ("CalendarioFragment".equals(fragmentToLoad)) {
                cargarFragmento(new CalendarioFragment());
                marcarBotonSeleccionado(btnCalendario);
            } else {
                cargarFragmento(new RecetasFragment());
                marcarBotonSeleccionado(btnVerRecetas);
            }
        }

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

        btnLogout.setOnClickListener(v -> confirmarLogout());
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cerrar_sesion))
                .setMessage(getString(R.string.confirmar_cerrar_sesion))
                .setPositiveButton(getString(R.string.aceptar), (d, w) -> cerrarSesion())
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
    }

    private void cerrarSesion() {
        // Limpiar cachés antes de cerrar sesión
        RecetasSrv.limpiarCaches();

        FirebaseAuth.getInstance().signOut();
        irALogin();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void cargarFragmento(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void marcarBotonSeleccionado(ImageButton botonSeleccionado) {
        findViewById(R.id.btnVerRecetas).setEnabled(true);
        findViewById(R.id.btnCalendario).setEnabled(true);
        findViewById(R.id.btnListaCompra).setEnabled(true);
        botonSeleccionado.setEnabled(false);
    }
}