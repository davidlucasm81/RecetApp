package com.david.recetapp.actividades.recetas;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.actividades.RecetaBaseActivity;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.util.ArrayList;
import java.util.Locale;

public class AddRecetaActivity extends RecetaBaseActivity {

    private ProgressBar progressBar;
    private Button btnCrear;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddRecetaActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receta);

        initializeViews();
        setupIngredientesSection();
        setupPasosSection();
        setupAlergenosSection();
        setupCrearButton();

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    private void initializeViews() {
        editTextNombre = findViewById(R.id.editTextNombre);
        checkboxInvierno = findViewById(R.id.checkboxInvierno);
        checkboxVerano = findViewById(R.id.checkboxVerano);
        checkboxOtonio = findViewById(R.id.checkboxOtonio);
        checkboxPrimavera = findViewById(R.id.checkboxPrimavera);
        temporadas = new ArrayList<>();

        autoCompleteTextViewNombreIngrediente = findViewById(R.id.autoCompleteTextViewNombreIngrediente);
        numberPickerNumeroPersonas = findViewById(R.id.numeroPersonas);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);
        gridLayout = findViewById(R.id.gridLayoutAlergenos);
        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        estrellas = findViewById(R.id.estrellas);

        btnCrear = findViewById(R.id.btnCrear);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        pasos = new ArrayList<>();
        alergenos = new ArrayList<>();
        alergenosSeleccionados = new ArrayList<>();
    }

    private void setupIngredientesSection() {
        Spinner spinner = findViewById(R.id.spinner_quantity_unit);
        setupIngredientes(getResources().getStringArray(R.array.ingredient_list), spinner);

        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        btnAgregarIngrediente.setOnClickListener(v -> {
            String nombreIngrediente = autoCompleteTextViewNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();
            String tipoCantidad = (String) spinner.getSelectedItem();

            if (validarIngrediente(nombreIngrediente, cantidad, tipoCantidad)) {
                agregarIngrediente(nombreIngrediente, cantidad, tipoCantidad);
                limpiarCamposIngrediente(spinner);
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validarIngrediente(String nombre, String cantidad, String tipo) {
        return !nombre.isEmpty()
                && !cantidad.isEmpty()
                && tipo != null
                && !tipo.isEmpty()
                && UtilsSrv.esNumeroEnteroOFraccionValida(cantidad);
    }

    private void limpiarCamposIngrediente(Spinner spinner) {
        autoCompleteTextViewNombreIngrediente.setText("");
        editTextCantidad.setText("1");
    }

    private void setupPasosSection() {
        linearLayoutListaPasos.setOnDragListener((v, event) -> {
            if (isDragging && event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                int sourcePosition = Integer.parseInt(item.getText().toString());
                int targetPosition = calculateTargetPosition(event.getY());

                if (sourcePosition != targetPosition && sourcePosition != -1) {
                    Paso paso = pasos.remove(sourcePosition);
                    pasos.add(targetPosition, paso);
                    mostrarPasos();
                }
            }
            return true;
        });

        Button btnAgregarPaso = findViewById(R.id.btnAgregarPaso);
        btnAgregarPaso.setOnClickListener(v -> {
            EditText editTextPaso = findViewById(R.id.editTextPaso);
            EditText editTextHoras = findViewById(R.id.editTextHoras);
            EditText editTextMinutos = findViewById(R.id.editTextMinutos);

            String textoPaso = editTextPaso.getText().toString().trim();
            int horas = Integer.parseInt(editTextHoras.getText().toString());
            int minutos = Integer.parseInt(editTextMinutos.getText().toString());
            String tiempoPaso = String.format(Locale.getDefault(), "%02d:%02d", horas, minutos);

            if (!textoPaso.isEmpty()) {
                Paso paso = new Paso(textoPaso, tiempoPaso);
                pasos.add(paso);
                limpiarCamposPaso(editTextPaso, editTextHoras, editTextMinutos);
                mostrarPasos();
                UtilsSrv.notificacion(this, getString(R.string.paso_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                UtilsSrv.notificacion(this, getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void limpiarCamposPaso(EditText editTextPaso, EditText editTextHoras, EditText editTextMinutos) {
        editTextPaso.setText("");
        editTextHoras.setText("0");
        editTextMinutos.setText("0");
    }

    private void setupAlergenosSection() {
        String[] alergenosNombresArray = getResources().getStringArray(R.array.alergenos_conocidos_nombres);

        for (int i = 0; i < alergenosNombresArray.length; i++) {
            alergenos.add(new Alergeno(alergenosNombresArray[i], i));
        }

        mostrarAlergenos();
    }

    private void setupCrearButton() {
        btnCrear.setOnClickListener(v -> crearReceta());
    }

    private void crearReceta() {
        CheckBox postre = findViewById(R.id.checkBoxPostre);

        // Validaciones
        String nombre = editTextNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            UtilsSrv.notificacion(this, getString(R.string.no_nombre), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validarTemporadas()) {
            UtilsSrv.notificacion(this, getString(R.string.no_temporadas), Toast.LENGTH_SHORT).show();
            return;
        }

        if (ingredientes.isEmpty()) {
            UtilsSrv.notificacion(this, getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            return;
        }

        if (pasos.isEmpty()) {
            UtilsSrv.notificacion(this, getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir lista de temporadas
        temporadas.clear();
        if (checkboxInvierno.isChecked()) temporadas.add(Temporada.INVIERNO);
        if (checkboxVerano.isChecked()) temporadas.add(Temporada.VERANO);
        if (checkboxOtonio.isChecked()) temporadas.add(Temporada.OTONIO);
        if (checkboxPrimavera.isChecked()) temporadas.add(Temporada.PRIMAVERA);

        // Crear objeto receta
        Receta receta = new Receta();
        receta.setNombre(nombre);
        receta.setIngredientes(ingredientes);
        RecetasSrv.setPuntuacionDada(receta, this);
        receta.setPasos(pasos);
        receta.setTemporadas(temporadas);
        receta.setNumPersonas(Integer.parseInt(numberPickerNumeroPersonas.getText().toString()));
        receta.setEstrellas(estrellas.getRating());
        receta.setFechaCalendario(new java.util.Date(0));
        receta.setAlergenos(alergenosSeleccionados);
        receta.setShared(false);
        receta.setPostre(postre.isChecked());

        // Guardar receta con callback
        guardarReceta(receta);
    }

    private boolean validarTemporadas() {
        return checkboxInvierno.isChecked()
                || checkboxVerano.isChecked()
                || checkboxOtonio.isChecked()
                || checkboxPrimavera.isChecked();
    }

    private void guardarReceta(Receta receta) {
        // Mostrar progreso
        mostrarProgreso(true);

        RecetasSrv.addReceta(receta, new RecetasSrv.SimpleCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    mostrarProgreso(false);
                    UtilsSrv.notificacion(AddRecetaActivity.this,
                            getString(R.string.receta_creada),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddRecetaActivity.this, MainActivity.class);
                    intent.putExtra("aviso_receta_creada", getString(R.string.receta_creada));
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    mostrarProgreso(false);
                    UtilsSrv.notificacion(AddRecetaActivity.this,
                            getString(R.string.error_crear_receta),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void mostrarProgreso(boolean mostrar) {
        if (progressBar != null) {
            progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }

        if (btnCrear != null) {
            btnCrear.setEnabled(!mostrar);
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        ingredientes = savedInstanceState.getParcelableArrayList(KEY_INGREDIENTES, Ingrediente.class);
        pasos = savedInstanceState.getParcelableArrayList(KEY_PASOS, Paso.class);
        mostrarIngredientes();
        mostrarPasos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}