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

        // Cargar recetas y verificar valoraciones pendientes de días pasados
        RecetasSrv.cargarListaRecetas(this, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(java.util.List<com.david.recetapp.negocio.beans.Receta> recetas) {
                verificarRecetasPendientesValoracion();
            }
            @Override
            public void onFailure(Exception e) {
                verificarRecetasPendientesValoracion();
            }
        });
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

    private void verificarRecetasPendientesValoracion() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate hace30Dias = hoy.minusDays(30);
        
        new Thread(() -> {
            try {
                java.util.List<com.david.recetapp.negocio.beans.Day> diasPasados = CalendarioSrv.getDiasEnRangoSync(hace30Dias, hoy.minusDays(1));
                if (diasPasados.isEmpty()) return;

                java.util.List<com.david.recetapp.negocio.beans.Receta> todasRecetas = RecetasSrv.getRecetas();
                if (todasRecetas.isEmpty()) return;

                java.util.Map<String, com.david.recetapp.negocio.beans.Receta> recetaMap = new java.util.HashMap<>();
                for (com.david.recetapp.negocio.beans.Receta r : todasRecetas) {
                    if (r.getId() != null) recetaMap.put(r.getId(), r);
                }

                android.content.SharedPreferences prefs = getSharedPreferences("RecetappPrefs", MODE_PRIVATE);
                long currentTime = System.currentTimeMillis();

                for (com.david.recetapp.negocio.beans.Day d : diasPasados) {
                    if (d.getRecetas() == null) continue;
                    java.time.LocalDate fechaDia = CalendarioSrv.getLocalDate(d.getDayOfMonth(), d.getMonth(), d.getYear());
                    
                    for (com.david.recetapp.negocio.beans.RecetaDia rd : d.getRecetas()) {
                        com.david.recetapp.negocio.beans.Receta receta = recetaMap.get(rd.getIdReceta());
                        if (receta == null) continue;
                        
                        if (receta.getEstrellas() >= 0f) continue;

                        String dateStr = d.getYear() + "_" + d.getMonth() + "_" + d.getDayOfMonth();
                        String dismissedKey = "dismissed_" + receta.getId() + "_" + dateStr;
                        String postponedKey = "postponed_" + receta.getId() + "_" + dateStr;

                        if (prefs.getBoolean(dismissedKey, false)) continue;
                        long postponedUntil = prefs.getLong(postponedKey, 0L);
                        if (currentTime < postponedUntil) continue;

                        runOnUiThread(() -> mostrarDialogoValoracionPendiente(receta, fechaDia));
                        return;
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error verificando recetas pendientes de valoración", e);
            }
        }).start();
    }

    private void mostrarDialogoValoracionPendiente(com.david.recetapp.negocio.beans.Receta receta, java.time.LocalDate fecha) {
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_valorar_receta, null);
        android.widget.TextView tv = dialogView.findViewById(R.id.txtMensajeValoracion);
        android.widget.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarValoracion);

        tv.setText(getString(R.string.valorar_receta_mensaje, receta.getNombre(), fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        String dateStr = fecha.getYear() + "_" + (fecha.getMonthValue() - 1) + "_" + fecha.getDayOfMonth();
        android.content.SharedPreferences prefs = getSharedPreferences("RecetappPrefs", MODE_PRIVATE);

        new AlertDialog.Builder(this)
                .setTitle(R.string.valorar_receta_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    if (rating > 0f) {
                        receta.setEstrellas(rating);
                        RecetasSrv.editarReceta(receta, new RecetasSrv.SimpleCallback() {
                            @Override public void onSuccess() {
                                com.david.recetapp.negocio.servicios.UtilsSrv.notificacion(MainActivity.this, getString(R.string.receta_editada), android.widget.Toast.LENGTH_SHORT);
                            }
                            @Override public void onFailure(Exception e) {
                                com.david.recetapp.negocio.servicios.UtilsSrv.notificacion(MainActivity.this, getString(R.string.error_editar_receta), android.widget.Toast.LENGTH_SHORT);
                            }
                        });
                    }
                })
                .setNeutralButton(R.string.puntuar_mas_tarde, (dialog, which) -> {
                    long tomorrow = System.currentTimeMillis() + (24L * 60 * 60 * 1000);
                    prefs.edit().putLong("postponed_" + receta.getId() + "_" + dateStr, tomorrow).apply();
                })
                .setNegativeButton(R.string.no_poner_puntuacion, (dialog, which) ->
                    prefs.edit().putBoolean("dismissed_" + receta.getId() + "_" + dateStr, true).apply()
                )
                .show();
    }
}