package com.david.recetapp.actividades;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddRecetasActivity extends AppCompatActivity {

    private EditText editTextNombre;
    private CheckBox checkboxInvierno;
    private CheckBox checkboxVerano;
    private CheckBox checkboxOtonio;
    private CheckBox checkboxPrimavera;
    private List<Temporada> temporadas;

    private EditText editTextNombreIngrediente;
    private EditText editTextCantidad;
    private LinearLayout linearLayoutIngredientes;
    private List<Ingrediente> ingredientes;

    private LinearLayout linearLayoutListaPasos;
    private List<Paso> pasos;

    private List<Alergeno> alergenos;
    private List<Alergeno> alergenosSeleccionados;
    private GridLayout gridLayout;
    private RatingBar estrellas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receta);

        editTextNombre = findViewById(R.id.editTextNombre);
        checkboxInvierno = findViewById(R.id.checkboxInvierno);
        checkboxVerano = findViewById(R.id.checkboxVerano);
        checkboxOtonio = findViewById(R.id.checkboxOtonio);
        checkboxPrimavera = findViewById(R.id.checkboxPrimavera);
        temporadas = new ArrayList<>();
        editTextNombreIngrediente = findViewById(R.id.editTextNombreIngrediente);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);
        gridLayout = findViewById(R.id.gridLayoutAlergenos);

        ingredientes = new ArrayList<>();
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
            String nombreIngrediente = editTextNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();
            String tipoCantidad = (String) spinner.getSelectedItem();

            if (!nombreIngrediente.isEmpty() && !cantidad.isEmpty() && tipoCantidad != null && !tipoCantidad.isEmpty()) {
                int cantidadNumerica = Integer.parseInt(cantidad);
                agregarIngrediente(nombreIngrediente, cantidadNumerica, tipoCantidad);
                editTextNombreIngrediente.setText("");
                editTextCantidad.setText("1");
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });

        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        pasos = new ArrayList<>();

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
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.paso_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });
        // Obtener arrays de recursos
        String[] alergenosNombresArray = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
        alergenos = new ArrayList<>();

        // Crear objetos Alergeno a partir de los nombres de alérgenos y sus IDs de drawables
        for (int i = 0; i < alergenosNombresArray.length; i++) {
            alergenos.add(new Alergeno(alergenosNombresArray[i], i));
        }
        alergenosSeleccionados = new ArrayList<>();
        mostrarAlergenos();
        estrellas = findViewById(R.id.estrellas);

        Button btnCrear = findViewById(R.id.btnCrear);

        btnCrear.setOnClickListener(v -> {
            String nombre = editTextNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.no_nombre), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkboxInvierno.isChecked() && !checkboxVerano.isChecked() && !checkboxOtonio.isChecked() && !checkboxPrimavera.isChecked()) {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.no_temporadas), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ingredientes.isEmpty()) {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
                return;
            }
            if (pasos.isEmpty()) {
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
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

            Receta receta = new Receta();

            receta.setNombre(nombre);
            receta.setIngredientes(ingredientes);
            receta.setPasos(pasos);
            receta.setTemporadas(temporadas);
            receta.setEstrellas(estrellas.getRating());
            receta.setFechaCalendario(new Date(0));
            receta.setAlergenos(alergenosSeleccionados);

            // Obtener la lista actual de recetas desde el archivo JSON
            List<Receta> listaRecetas = cargarListaRecetas();

            // Agregar la receta al principio de la lista
            listaRecetas.add(0, receta);

            // Guardar la lista actualizada en el archivo JSON
            guardarListaRecetas(listaRecetas);

            // Crear un Intent para volver a la pantalla inicial
            Toast.makeText(AddRecetasActivity.this, this.getString(R.string.receta_creada), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AddRecetasActivity.this, MainActivity.class);
            intent.putExtra("aviso_receta_creada", this.getString(R.string.receta_creada));

            // Iniciar la actividad y pasar el Intent
            startActivity(intent);
        });

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
            imageViewIconoAlergeno.setImageResource(obtenerImagen(alergeno.getNumero()));
            checkBoxAlergeno.setText(alergeno.getNombre());
            checkBoxAlergeno.setChecked(alergenosSeleccionados.stream()
                    .anyMatch(objeto -> alergeno.getNombre().equals(objeto.getNombre())));

            checkBoxAlergeno.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alergenosSeleccionados.add(alergeno);
            });

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
                Toast.makeText(AddRecetasActivity.this, this.getString(R.string.ingrediente_eliminado), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(AddRecetasActivity.this, this.getString(R.string.paso_eliminado), Toast.LENGTH_SHORT).show();
                });
            }

            linearLayoutListaPasos.addView(convertView);
        }
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

    private void guardarListaRecetas(List<Receta> listaRecetas) {
        try {
            // Convertir la lista de objetos Receta a JSON utilizando GSON
            Gson gson = new Gson();
            String json = gson.toJson(listaRecetas);

            // Guardar el JSON en el almacenamiento interno
            FileOutputStream fos = openFileOutput("lista_recetas.json", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int obtenerImagen(int numero) {
        switch (numero) {
            case 0:
                return R.drawable.ic_gluten;
            case 1:
                return R.drawable.ic_lacteos;
            case 2:
                return R.drawable.ic_cacahuetes;
            case 3:
                return R.drawable.ic_soja;
            case 4:
                return R.drawable.ic_pescado;
            case 5:
                return R.drawable.ic_mariscos;
            case 6:
                return R.drawable.ic_huevos;
            default:
                return R.drawable.alergeno_placeholder;
        }
    }
}

