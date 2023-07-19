package com.david.recetapp.actividades;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

public class EditarRecetaActivity extends AppCompatActivity {

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

    private RatingBar estrellas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_receta);

        // Obtiene índice del día seleccionado del Intent
        int posicion = (int) getIntent().getSerializableExtra("position");
        @SuppressWarnings("unchecked") List<Receta> recetas = (List<Receta>) getIntent().getSerializableExtra("listaRecetas");
        Receta receta = recetas.get(posicion);

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
        editTextNombreIngrediente = findViewById(R.id.editTextNombreIngrediente);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);

        ingredientes = receta.getIngredientes();
        mostrarIngredientes();

        btnAgregarIngrediente.setOnClickListener(v -> {
            String nombreIngrediente = editTextNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();

            if (!nombreIngrediente.isEmpty() && !cantidad.isEmpty()) {
                int cantidadNumerica = Integer.parseInt(cantidad);
                agregarIngrediente(nombreIngrediente, cantidadNumerica);
                editTextNombreIngrediente.setText("");
                editTextCantidad.setText("1");
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });

        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        pasos = receta.getPasos();
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
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.paso_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });

        estrellas = findViewById(R.id.estrellas);
        estrellas.setRating(receta.getEstrellas());
        Button btnCrear = findViewById(R.id.btnCrear);

        btnCrear.setOnClickListener(v -> {
            String nombre = editTextNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.no_nombre), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkboxInvierno.isChecked() && !checkboxVerano.isChecked() && !checkboxOtonio.isChecked() && !checkboxPrimavera.isChecked()) {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.no_temporadas), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ingredientes.isEmpty()) {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
                return;
            }
            if (pasos.isEmpty()) {
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.paso_no_aniadido), Toast.LENGTH_SHORT).show();
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

            // Guardar la lista actualizada en el archivo JSON
            guardarListaRecetas(recetas);

            // Crear un Intent para volver a la pantalla inicial
            Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.receta_editada), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditarRecetaActivity.this, VerRecetasActivity.class);
            intent.putExtra("aviso_receta_editada", this.getString(R.string.receta_editada));

            // Iniciar la actividad y pasar el Intent
            startActivity(intent);
        });

    }

    private void agregarIngrediente(String nombre, int numero) {
        Ingrediente ingrediente = new Ingrediente(nombre, numero);
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
            textViewNumero.setText(String.valueOf(ingrediente.getCantidad()));

            ImageButton btnEliminar = ingredienteView.findViewById(R.id.btnEliminarIngrediente);
            btnEliminar.setOnClickListener(v -> {
                ingredientes.remove(ingrediente);
                mostrarIngredientes();
                Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.ingrediente_eliminado), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(EditarRecetaActivity.this, this.getString(R.string.paso_eliminado), Toast.LENGTH_SHORT).show();
                });
            }

            linearLayoutListaPasos.addView(convertView);
        }
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
}
