package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EditarRecetaActivity extends AppCompatActivity {
    private static final String KEY_INGREDIENTES = "ingredientes";
    private static final String KEY_PASOS = "pasos";
    private EditText editTextNombre;
    private CheckBox checkboxInvierno;
    private CheckBox checkboxVerano;
    private CheckBox checkboxOtonio;
    private CheckBox checkboxPrimavera;
    private Set<Temporada> temporadas;

    private AutoCompleteTextView autoCompleteTextViewNombreIngrediente;
    private EditText editTextCantidad;
    private LinearLayout linearLayoutIngredientes;
    private ArrayList<Ingrediente> ingredientes;

    private LinearLayout linearLayoutListaPasos;
    private ArrayList<Paso> pasos;
    private List<Alergeno> alergenos;
    private List<Alergeno> alergenosSeleccionados;
    private GridLayout gridLayout;
    private RatingBar estrellas;

    @Override
    public void onBackPressed() {
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(EditarRecetaActivity.this, VerRecetasActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_INGREDIENTES, ingredientes);
        outState.putSerializable(KEY_PASOS, pasos);
    }
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_receta);

        // Obtiene índice del día seleccionado del Intent
        int posicion = (int) getIntent().getSerializableExtra("position");
        @SuppressWarnings("unchecked") List<Receta> recetas = (List<Receta>) getIntent().getSerializableExtra("listaRecetas");
        Receta receta = recetas.get(posicion);

        CheckBox postre = findViewById(R.id.checkBoxPostre);
        postre.setChecked(receta.isPostre());

        gridLayout = findViewById(R.id.gridLayoutAlergenos);

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextNombre.setText(receta.getNombre());
        checkboxInvierno = findViewById(R.id.checkboxInvierno);
        if (receta.getTemporadas().contains(Temporada.INVIERNO)) {
            checkboxInvierno.setChecked(true);
        }
        checkboxVerano = findViewById(R.id.checkboxVerano);
        if (receta.getTemporadas().contains(Temporada.VERANO)) {
            checkboxVerano.setChecked(true);
        }
        checkboxOtonio = findViewById(R.id.checkboxOtonio);
        if (receta.getTemporadas().contains(Temporada.OTONIO)) {
            checkboxOtonio.setChecked(true);
        }
        checkboxPrimavera = findViewById(R.id.checkboxPrimavera);
        if (receta.getTemporadas().contains(Temporada.PRIMAVERA)) {
            checkboxPrimavera.setChecked(true);
        }
        temporadas = receta.getTemporadas();
        autoCompleteTextViewNombreIngrediente = findViewById(R.id.autoCompleteTextViewNombreIngrediente);
        // Obtener la lista de ingredientes desde resources (strings.xml) o cualquier otra fuente de datos
        String[] ingredientList = getResources().getStringArray(R.array.ingredient_list);

        // Crear un adaptador con la lista de ingredientes y configurarlo en el AutoCompleteTextView
        ArrayAdapter<String> adapterIngredientes = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ingredientList);
        autoCompleteTextViewNombreIngrediente.setAdapter(adapterIngredientes);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);

        ingredientes = (ArrayList<Ingrediente>) receta.getIngredientes();
        mostrarIngredientes();

        Spinner spinner = findViewById(R.id.spinner_quantity_unit);

        // Obtén las opciones de unidades de medida desde el archivo de recursos
        String[] quantityUnits = getResources().getStringArray(R.array.quantity_units);

        // Crea un adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, quantityUnits);

        // Especifica el diseño para los elementos desplegables
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Establece el adaptador en el Spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        btnAgregarIngrediente.setOnClickListener(v -> {
            String nombreIngrediente = autoCompleteTextViewNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();
            String tipoCantidad = (String) spinner.getSelectedItem();

            if (!nombreIngrediente.isEmpty() && !cantidad.isEmpty() && tipoCantidad != null && !tipoCantidad.isEmpty()) {
                int cantidadNumerica = Integer.parseInt(cantidad);
                agregarIngrediente(nombreIngrediente, cantidadNumerica, tipoCantidad);
                autoCompleteTextViewNombreIngrediente.setText("");
                editTextCantidad.setText("1");
                UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT);
            } else {
                UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT);
            }
        });

        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        pasos = (ArrayList<Paso>) receta.getPasos();
        mostrarPasos();

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
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.paso_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
        // Obtener arrays de recursos
        String[] alergenosNombresArray = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
        alergenos = new ArrayList<>();

        // Crear objetos Alergeno a partir de los nombres de alérgenos y sus IDs de drawables
        for (int i = 0; i < alergenosNombresArray.length; i++) {
            alergenos.add(new Alergeno(alergenosNombresArray[i], i));
        }
        // Agregar más alérgenos según sea necesario
        alergenosSeleccionados = receta.getAlergenos();
        mostrarAlergenos();

        estrellas = findViewById(R.id.estrellas);
        estrellas.setRating(receta.getEstrellas());
        Button btnCrear = findViewById(R.id.btnCrear);

        btnCrear.setOnClickListener(v -> {
            String nombre = editTextNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.no_nombre), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkboxInvierno.isChecked() && !checkboxVerano.isChecked() && !checkboxOtonio.isChecked() && !checkboxPrimavera.isChecked()) {
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.no_temporadas), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ingredientes.isEmpty()) {
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
                return;
            }
            if (pasos.isEmpty()) {
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkboxInvierno.isChecked()) {
                temporadas.add(Temporada.INVIERNO);
            }
            if (checkboxVerano.isChecked()) {
                temporadas.add(Temporada.VERANO);
            }
            if (checkboxOtonio.isChecked()) {
                temporadas.add(Temporada.OTONIO);
            }
            if (checkboxPrimavera.isChecked()) {
                temporadas.add(Temporada.PRIMAVERA);
            }

            receta.setNombre(nombre);
            receta.setIngredientes(ingredientes);
            receta.setPasos(pasos);
            receta.setTemporadas(temporadas);
            receta.setEstrellas(estrellas.getRating());
            receta.setAlergenos(alergenosSeleccionados);
            receta.setShared(false);
            receta.setPostre(postre.isChecked());

            // Guardar la lista actualizada en el archivo JSON
            RecetasSrv.guardarListaRecetas(this, recetas);

            // Crear un Intent para volver a la pantalla inicial
             UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.receta_editada), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditarRecetaActivity.this, VerRecetasActivity.class);
            intent.putExtra("aviso_receta_editada", this.getString(R.string.receta_editada));

            // Iniciar la actividad y pasar el Intent
            startActivity(intent);
        });

        if (savedInstanceState != null) {
            ingredientes = (ArrayList<Ingrediente>) savedInstanceState.getSerializable(KEY_INGREDIENTES);
            pasos = (ArrayList<Paso>) savedInstanceState.getSerializable(KEY_PASOS);
            mostrarIngredientes();
            mostrarPasos();
        }

    }

    private void mostrarAlergenos() {
        gridLayout.removeAllViews();

        int columnas = 2; // Cantidad de columnas en la cuadrícula
        int filaActual = -1;

        for (int i = 0; i < alergenos.size(); i++) {
            if (i % columnas == 0) {
                // Crear una nueva fila en el GridLayout
                filaActual++;
            }

            View alergenoView = LayoutInflater.from(this).inflate(R.layout.item_alergeno, gridLayout, false);
            ImageView imageViewIconoAlergeno = alergenoView.findViewById(R.id.imageViewIconoAlergeno);
            CheckBox checkBoxAlergeno = alergenoView.findViewById(R.id.checkBoxAlergeno);

            Alergeno alergeno = alergenos.get(i);
            imageViewIconoAlergeno.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));
            checkBoxAlergeno.setText(alergeno.getNombre());
            checkBoxAlergeno.setChecked(alergenosSeleccionados.stream()
                    .anyMatch(objeto -> alergeno.getNombre().equals(objeto.getNombre())));

            checkBoxAlergeno.setOnCheckedChangeListener((buttonView, isChecked) -> alergenosSeleccionados.add(alergeno));

            // Agregar el elemento a la cuadrícula
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(filaActual);
            params.columnSpec = GridLayout.spec(i % columnas);
            alergenoView.setLayoutParams(params);
            gridLayout.addView(alergenoView);
        }
    }

    private void agregarIngrediente(String nombre, int numero, String tipoCantidad) {
        Ingrediente ingrediente = new Ingrediente(nombre, numero, tipoCantidad);
        ingredientes.add(ingrediente);
        mostrarIngredientes();
    }

    // Agregar un método para mostrar los ingredientes en el LinearLayout
    private void mostrarIngredientes() {
        linearLayoutIngredientes.removeAllViews();

        for (Ingrediente ingrediente : ingredientes) {
            ViewGroup parent = findViewById(R.id.linearLayoutIngredientes);
            View ingredienteView = LayoutInflater.from(this).inflate(R.layout.list_item_ingrediente, parent, false);

            TextView textViewIngrediente = ingredienteView.findViewById(R.id.textViewIngrediente);
            TextView textViewNumero = ingredienteView.findViewById(R.id.textViewNumero);

            textViewIngrediente.setText(ingrediente.getNombre());
            textViewNumero.setText(MessageFormat.format("{0} {1}", ingrediente.getCantidad(), ingrediente.getTipoCantidad()));

            ImageButton btnEliminar = ingredienteView.findViewById(R.id.btnEliminarIngrediente);
            btnEliminar.setOnClickListener(v -> {
                ingredientes.remove(ingrediente);
                mostrarIngredientes();
                 UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.ingrediente_eliminado), Toast.LENGTH_SHORT).show();
            });

            linearLayoutIngredientes.addView(ingredienteView);
        }
    }

    // Agregar un método para mostrar los pasos en el LinearLayout
    private void mostrarPasos() {
        linearLayoutListaPasos.removeAllViews();

        for (int position = 0; position < pasos.size(); position++) {
            ViewGroup parent = findViewById(R.id.linearLayoutPaso);
            View convertView = LayoutInflater.from(this).inflate(R.layout.list_item_paso, parent, false);

            TextView textViewNumero = convertView.findViewById(R.id.textViewNumero);
            TextView textViewPaso = convertView.findViewById(R.id.textViewPaso);
            TextView textViewTiempo = convertView.findViewById(R.id.textViewTiempo);
            Paso paso = pasos.get(position);
            if (paso != null) {
                textViewNumero.setText(String.format(Locale.getDefault(), "%d) ", position + 1));
                textViewPaso.setText(paso.getPaso());
                textViewTiempo.setText(paso.getTiempo());

                ImageButton btnEliminarPaso = convertView.findViewById(R.id.btnEliminarPaso);
                btnEliminarPaso.setOnClickListener(v -> {
                    pasos.remove(paso);
                    mostrarPasos();
                     UtilsSrv.notificacion(EditarRecetaActivity.this, this.getString(R.string.paso_eliminado), Toast.LENGTH_SHORT).show();
                });
            }

            linearLayoutListaPasos.addView(convertView);
        }
    }
}
