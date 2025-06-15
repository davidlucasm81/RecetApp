package com.david.recetapp.actividades.recetas;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

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
import java.util.HashSet;
import java.util.Locale;

public class AddRecetaActivity extends RecetaBaseActivity {
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

        editTextNombre = findViewById(R.id.editTextNombre);
        checkboxInvierno = findViewById(R.id.checkboxInvierno);
        checkboxVerano = findViewById(R.id.checkboxVerano);
        checkboxOtonio = findViewById(R.id.checkboxOtonio);
        checkboxPrimavera = findViewById(R.id.checkboxPrimavera);
        temporadas = new ArraySet<>();
        CheckBox postre = findViewById(R.id.checkBoxPostre);
        autoCompleteTextViewNombreIngrediente = findViewById(R.id.autoCompleteTextViewNombreIngrediente);
        numberPickerNumeroPersonas = findViewById(R.id.numeroPersonas);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);
        gridLayout = findViewById(R.id.gridLayoutAlergenos);
        Spinner spinner = findViewById(R.id.spinner_quantity_unit);
        setupIngredientes(getResources().getStringArray(R.array.ingredient_list), spinner);
        btnAgregarIngrediente.setOnClickListener(v -> {
            String nombreIngrediente = autoCompleteTextViewNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();
            String tipoCantidad = (String) spinner.getSelectedItem();
            if (!nombreIngrediente.isEmpty() && !cantidad.isEmpty() && tipoCantidad != null && !tipoCantidad.isEmpty() && UtilsSrv.esNumeroEnteroOFraccionValida(cantidad)) {
                agregarIngrediente(nombreIngrediente, cantidad, tipoCantidad);
                autoCompleteTextViewNombreIngrediente.setText("");
                editTextCantidad.setText("1");
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        pasos = new ArrayList<>();
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
                editTextPaso.setText("");
                editTextHoras.setText("0");
                editTextMinutos.setText("0");
                mostrarPasos();
                UtilsSrv.notificacion(this, getString(R.string.paso_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                UtilsSrv.notificacion(this, getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
        String[] alergenosNombresArray = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
        alergenos = new ArrayList<>();
        for (int i = 0; i < alergenosNombresArray.length; i++) {
            alergenos.add(new Alergeno(alergenosNombresArray[i], i));
        }
        alergenosSeleccionados = new HashSet<>();
        mostrarAlergenos();
        estrellas = findViewById(R.id.estrellas);
        Button btnCrear = findViewById(R.id.btnCrear);
        btnCrear.setOnClickListener(v -> {
            String nombre = editTextNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                UtilsSrv.notificacion(this, getString(R.string.no_nombre), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!checkboxInvierno.isChecked() && !checkboxVerano.isChecked() && !checkboxOtonio.isChecked() && !checkboxPrimavera.isChecked()) {
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
            if (checkboxInvierno.isChecked()) temporadas.add(Temporada.INVIERNO);
            if (checkboxVerano.isChecked()) temporadas.add(Temporada.VERANO);
            if (checkboxOtonio.isChecked()) temporadas.add(Temporada.OTONIO);
            if (checkboxPrimavera.isChecked()) temporadas.add(Temporada.PRIMAVERA);
            Receta receta = new Receta();
            receta.setNombre(nombre);
            receta.setIngredientes(ingredientes, this);
            receta.setPasos(pasos);
            receta.setTemporadas(temporadas);
            receta.setNumPersonas(Integer.parseInt(numberPickerNumeroPersonas.getText().toString()));
            receta.setEstrellas(estrellas.getRating());
            receta.setFechaCalendario(new java.util.Date(0));
            receta.setAlergenos(alergenosSeleccionados);
            receta.setShared(false);
            receta.setPostre(postre.isChecked());
            RecetasSrv.addReceta(this, receta);
            UtilsSrv.notificacion(this, getString(R.string.receta_creada), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("aviso_receta_creada", getString(R.string.receta_creada));
            startActivity(intent);
        });
        if (savedInstanceState != null) {
            ingredientes = savedInstanceState.getParcelableArrayList(KEY_INGREDIENTES, Ingrediente.class);
            pasos = savedInstanceState.getParcelableArrayList(KEY_PASOS, Paso.class);
            mostrarIngredientes();
            mostrarPasos();
        }
    }
}


