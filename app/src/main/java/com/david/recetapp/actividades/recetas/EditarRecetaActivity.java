package com.david.recetapp.actividades.recetas;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.actividades.RecetaBaseActivity;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditarRecetaActivity extends RecetaBaseActivity {
    private static final Pattern patternIngredient = Pattern.compile("^(.+)\\s(-?\\d+)$");
    private static final String KEY_INGREDIENTES = "ingredientes";
    private static final String KEY_PASOS = "pasos";

    private EditText editTextNombre;
    private CheckBox checkboxInvierno;
    private CheckBox checkboxVerano;
    private CheckBox checkboxOtonio;
    private CheckBox checkboxPrimavera;
    private List<Temporada> temporadas;
    private EditText numberPickerNumeroPersonas;
    private AutoCompleteTextView autoCompleteTextViewNombreIngrediente;
    private EditText editTextCantidad;
    private LinearLayout linearLayoutIngredientes;
    private ArrayList<Ingrediente> ingredientes;
    private LinearLayout linearLayoutListaPasos;
    private ArrayList<Paso> pasos;
    private int draggedItemPosition = -1;
    private List<Alergeno> alergenos;
    private List<Alergeno> alergenosSeleccionados;
    private GridLayout gridLayout;
    private RatingBar estrellas;
    private Map<String, Integer> ingredientMap;
    private boolean isDragging = false;

    private ProgressBar progressBar;
    private Button btnGuardar;
    private Receta recetaActual;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(EditarRecetaActivity.this, MainActivity.class);
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

        // Obtener receta a editar
        int posicion = getIntent().getSerializableExtra("position", Integer.class);
        List<Receta> recetas = (List<Receta>) getIntent().getSerializableExtra("listaRecetas", ArrayList.class);
        assert recetas != null;
        recetaActual = recetas.get(posicion);

        initializeViews();
        cargarDatosReceta();
        setupIngredientesSection();
        setupPasosSection();
        setupAlergenosSection();
        setupGuardarButton();

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
        numberPickerNumeroPersonas = findViewById(R.id.numeroPersonas);
        autoCompleteTextViewNombreIngrediente = findViewById(R.id.autoCompleteTextViewNombreIngrediente);
        editTextCantidad = findViewById(R.id.editTextCantidad);
        linearLayoutIngredientes = findViewById(R.id.linearLayoutIngredientes);
        linearLayoutListaPasos = findViewById(R.id.linearLayoutListaPasos);
        gridLayout = findViewById(R.id.gridLayoutAlergenos);
        estrellas = findViewById(R.id.estrellas);
        btnGuardar = findViewById(R.id.btnCrear);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        temporadas = new ArrayList<>();
        pasos = new ArrayList<>();
        alergenos = new ArrayList<>();
        alergenosSeleccionados = new ArrayList<>();
        ingredientMap = new HashMap<>();
    }

    private void cargarDatosReceta() {
        CheckBox postre = findViewById(R.id.checkBoxPostre);
        postre.setChecked(recetaActual.isPostre());

        editTextNombre.setText(recetaActual.getNombre());

        checkboxInvierno.setChecked(recetaActual.getTemporadas().contains(Temporada.INVIERNO));
        checkboxVerano.setChecked(recetaActual.getTemporadas().contains(Temporada.VERANO));
        checkboxOtonio.setChecked(recetaActual.getTemporadas().contains(Temporada.OTONIO));
        checkboxPrimavera.setChecked(recetaActual.getTemporadas().contains(Temporada.PRIMAVERA));

        temporadas = recetaActual.getTemporadas();
        numberPickerNumeroPersonas.setText(String.valueOf(recetaActual.getNumPersonas()));

        ingredientes = (ArrayList<Ingrediente>) recetaActual.getIngredientes();
        pasos = (ArrayList<Paso>) recetaActual.getPasos();
        alergenosSeleccionados = recetaActual.getAlergenos();

        estrellas.setRating(recetaActual.getEstrellas());
    }

    private void setupIngredientesSection() {
        String[] ingredientList = getResources().getStringArray(R.array.ingredient_list);

        // Procesar lista de ingredientes para el mapa de puntuaciones
        for (String s : ingredientList) {
            Matcher m = patternIngredient.matcher(s.trim());
            if (m.matches()) {
                ingredientMap.put(Objects.requireNonNull(m.group(1)).toLowerCase(Locale.getDefault()),
                        Integer.parseInt(Objects.requireNonNull(m.group(2))));
            }
        }

        // Crear lista de nombres para el AutoComplete
        String[] ingredientNames = new String[ingredientList.length];
        for (int i = 0; i < ingredientList.length; i++) {
            String entry = ingredientList[i].trim();
            Matcher matcher = patternIngredient.matcher(entry);
            if (matcher.matches()) {
                ingredientNames[i] = matcher.group(1);
            } else {
                ingredientNames[i] = entry;
            }
        }

        ArrayAdapter<String> adapterIngrediente = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ingredientNames);
        autoCompleteTextViewNombreIngrediente.setAdapter(adapterIngrediente);

        // Configurar spinner
        Spinner spinner = findViewById(R.id.spinner_quantity_unit);
        String[] quantityUnits = getResources().getStringArray(R.array.quantity_units);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, quantityUnits);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        // Botón agregar ingrediente
        Button btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        btnAgregarIngrediente.setOnClickListener(v -> {
            String nombreIngrediente = autoCompleteTextViewNombreIngrediente.getText().toString().trim();
            String cantidad = editTextCantidad.getText().toString().trim();
            String tipoCantidad = (String) spinner.getSelectedItem();

            if (validarIngrediente(nombreIngrediente, cantidad, tipoCantidad)) {
                agregarIngrediente(nombreIngrediente, cantidad, tipoCantidad);
                limpiarCamposIngrediente();
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_aniadido), Toast.LENGTH_SHORT).show();
            } else {
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_no_aniadido), Toast.LENGTH_SHORT).show();
            }
        });

        mostrarIngredientes();
    }

    private boolean validarIngrediente(String nombre, String cantidad, String tipo) {
        return !nombre.isEmpty()
                && !cantidad.isEmpty()
                && tipo != null
                && !tipo.isEmpty()
                && UtilsSrv.esNumeroEnteroOFraccionValida(cantidad);
    }

    private void limpiarCamposIngrediente() {
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
                    View draggedView = linearLayoutListaPasos.getChildAt(sourcePosition);
                    linearLayoutListaPasos.removeViewAt(sourcePosition);
                    linearLayoutListaPasos.addView(draggedView, targetPosition);

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

        mostrarPasos();
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

    private void setupGuardarButton() {
        btnGuardar.setOnClickListener(v -> guardarReceta());
    }

    private void guardarReceta() {
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

        // Actualizar temporadas
        temporadas.clear();
        if (checkboxInvierno.isChecked()) temporadas.add(Temporada.INVIERNO);
        if (checkboxVerano.isChecked()) temporadas.add(Temporada.VERANO);
        if (checkboxOtonio.isChecked()) temporadas.add(Temporada.OTONIO);
        if (checkboxPrimavera.isChecked()) temporadas.add(Temporada.PRIMAVERA);

        // Actualizar receta
        recetaActual.setNombre(nombre);
        recetaActual.setIngredientes(ingredientes);
        RecetasSrv.setPuntuacionDada(recetaActual, this);
        recetaActual.setPasos(pasos);
        recetaActual.setTemporadas(temporadas);
        recetaActual.setNumPersonas(Integer.parseInt(numberPickerNumeroPersonas.getText().toString()));
        recetaActual.setEstrellas(estrellas.getRating());
        recetaActual.setAlergenos(alergenosSeleccionados);
        recetaActual.setShared(false);
        recetaActual.setPostre(postre.isChecked());

        // Guardar con callback
        editarRecetaConCallback(recetaActual);
    }

    private boolean validarTemporadas() {
        return checkboxInvierno.isChecked()
                || checkboxVerano.isChecked()
                || checkboxOtonio.isChecked()
                || checkboxPrimavera.isChecked();
    }

    private void editarRecetaConCallback(Receta receta) {
        mostrarProgreso(true);

        RecetasSrv.editarReceta(receta, new RecetasSrv.SimpleCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    mostrarProgreso(false);
                    UtilsSrv.notificacion(EditarRecetaActivity.this,
                            getString(R.string.receta_editada),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(EditarRecetaActivity.this, MainActivity.class);
                    intent.putExtra("aviso_receta_editada", getString(R.string.receta_editada));
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    mostrarProgreso(false);
                    UtilsSrv.notificacion(EditarRecetaActivity.this,
                            getString(R.string.error_editar_receta),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void mostrarProgreso(boolean mostrar) {
        if (progressBar != null) {
            progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }

        if (btnGuardar != null) {
            btnGuardar.setEnabled(!mostrar);
        }
    }

    protected int calculateTargetPosition(float y) {
        int count = linearLayoutListaPasos.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = linearLayoutListaPasos.getChildAt(i);
            if (y < child.getY() + child.getHeight() / 2f) {
                return i;
            }
        }
        return Math.min(Math.max(0, count), pasos.size() - 1);
    }

    protected void mostrarAlergenos() {
        gridLayout.removeAllViews();

        int columnas = 2;
        int filaActual = -1;

        for (int i = 0; i < alergenos.size(); i++) {
            if (i % columnas == 0) {
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

            checkBoxAlergeno.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!alergenosSeleccionados.contains(alergeno)) {
                        alergenosSeleccionados.add(alergeno);
                    }
                } else {
                    alergenosSeleccionados.removeIf(a -> a.getNombre().equals(alergeno.getNombre()));
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(filaActual);
            params.columnSpec = GridLayout.spec(i % columnas);
            alergenoView.setLayoutParams(params);
            gridLayout.addView(alergenoView);
        }
    }

    protected void agregarIngrediente(String nombre, String numero, String tipoCantidad) {
        Ingrediente ingrediente = new Ingrediente(nombre, numero, tipoCantidad,
                ingredientMap.getOrDefault(nombre.toLowerCase(Locale.getDefault()), -2));
        ingredientes.add(ingrediente);
        mostrarIngredientes();
    }

    protected void mostrarIngredientes() {
        linearLayoutIngredientes.removeAllViews();

        for (int i = 0; i < ingredientes.size(); i++) {
            final Ingrediente ingrediente = ingredientes.get(i);

            ViewGroup parent = findViewById(R.id.linearLayoutIngredientes);
            View ingredienteView = LayoutInflater.from(this).inflate(R.layout.list_item_ingrediente, parent, false);

            EditText editTextNombre = ingredienteView.findViewById(R.id.editTextNombreIngrediente);
            EditText editTextCantidad = ingredienteView.findViewById(R.id.editTextCantidad);
            Spinner spinnerCantidad = ingredienteView.findViewById(R.id.spinner_quantity_unit);

            List<String> opcionesTipoCantidad = Arrays.asList(getResources().getStringArray(R.array.quantity_units));
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, opcionesTipoCantidad);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCantidad.setAdapter(spinnerAdapter);

            int selectedTypeIndex = opcionesTipoCantidad.indexOf(ingrediente.getTipoCantidad());
            spinnerCantidad.setSelection(selectedTypeIndex);

            editTextNombre.setText(ingrediente.getNombre());
            editTextCantidad.setText(String.valueOf(ingrediente.getCantidad()));

            setupIngredienteListeners(ingrediente, editTextNombre, editTextCantidad, spinnerCantidad);

            ImageButton btnEliminar = ingredienteView.findViewById(R.id.btnEliminarIngrediente);
            btnEliminar.setOnClickListener(v -> {
                ingredientes.remove(ingrediente);
                mostrarIngredientes();
                UtilsSrv.notificacion(this, getString(R.string.ingrediente_eliminado), Toast.LENGTH_SHORT).show();
            });

            linearLayoutIngredientes.addView(ingredienteView);
        }
    }

    private void setupIngredienteListeners(Ingrediente ingrediente, EditText editTextNombre,
                                           EditText editTextCantidad, Spinner spinnerCantidad) {
        editTextNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    ingrediente.setNombre(charSequence.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        editTextCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    String cantidadStr = charSequence.toString().trim();
                    if (UtilsSrv.esNumeroEnteroOFraccionValida(cantidadStr)) {
                        ingrediente.setCantidad(cantidadStr);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        spinnerCantidad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedType = (String) adapterView.getItemAtPosition(position);
                ingrediente.setTipoCantidad(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    protected void mostrarPasos() {
        linearLayoutListaPasos.removeAllViews();

        for (int position = 0; position < pasos.size(); position++) {
            ViewGroup parent = findViewById(R.id.linearLayoutPaso);
            View convertView = LayoutInflater.from(this).inflate(R.layout.list_item_paso, parent, false);

            TextView textViewNumero = convertView.findViewById(R.id.textViewNumero);
            EditText editTextPaso = convertView.findViewById(R.id.editTextPaso);
            EditText editTextTiempo = convertView.findViewById(R.id.editTextTiempo);
            ImageButton btnEliminarPaso = convertView.findViewById(R.id.btnEliminarPaso);

            final Paso paso = pasos.get(position);
            final int posicion = position;

            setupPasoTouchListener(convertView, editTextPaso, posicion);
            setupPasoData(paso, position, textViewNumero, editTextPaso, editTextTiempo);
            setupPasoListeners(paso, convertView, editTextPaso, editTextTiempo);

            btnEliminarPaso.setOnClickListener(v -> {
                pasos.remove(paso);
                mostrarPasos();
                UtilsSrv.notificacion(this, getString(R.string.paso_eliminado), Toast.LENGTH_SHORT).show();
            });

            linearLayoutListaPasos.addView(convertView);
        }
    }

    private void setupPasoTouchListener(View convertView, EditText editTextPaso, int posicion) {
        convertView.setOnTouchListener((view, event) -> {
            int action = event.getAction();

            boolean isTouchingReorder = false;
            ImageView imageViewReorder = convertView.findViewById(R.id.imageViewReordenar);
            if (imageViewReorder != null) {
                Rect hitRect = new Rect();
                imageViewReorder.getHitRect(hitRect);
                isTouchingReorder = hitRect.contains((int) event.getX(), (int) event.getY());
            }

            if (editTextPaso == null) return false;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (isTouchingReorder) {
                        isDragging = true;
                        toggleEditTextsFocusability(false);
                        draggedItemPosition = posicion;
                        ClipData data = ClipData.newPlainText("", String.valueOf(draggedItemPosition));
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        ViewCompat.startDragAndDrop(view, data, shadowBuilder, null, 0);
                        view.performClick();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    toggleEditTextsFocusability(true);
                    draggedItemPosition = -1;
                    break;
            }
            return false;
        });
    }

    private void toggleEditTextsFocusability(boolean enabled) {
        for (int i = 0; i < linearLayoutListaPasos.getChildCount(); i++) {
            View childView = linearLayoutListaPasos.getChildAt(i);
            EditText editTextPaso1 = childView.findViewById(R.id.editTextPaso);
            EditText editTextTiempo1 = childView.findViewById(R.id.editTextTiempo);

            if (editTextPaso1 != null) {
                editTextPaso1.setFocusableInTouchMode(enabled);
                editTextPaso1.setFocusable(enabled);
                editTextPaso1.setCursorVisible(enabled);
            }
            if (editTextTiempo1 != null) {
                editTextTiempo1.setFocusableInTouchMode(enabled);
                editTextTiempo1.setFocusable(enabled);
                editTextTiempo1.setCursorVisible(enabled);
            }
        }
    }

    private void setupPasoData(Paso paso, int position, TextView textViewNumero,
                               EditText editTextPaso, EditText editTextTiempo) {
        if (paso != null) {
            textViewNumero.setText(String.format(Locale.getDefault(), "%d) ", position + 1));
            editTextPaso.setText(paso.getPaso());
            editTextTiempo.setText(paso.getTiempo());
        }
    }

    private void setupPasoListeners(Paso paso, View convertView, EditText editTextPaso, EditText editTextTiempo) {
        editTextPaso.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    paso.setPaso(charSequence.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        editTextTiempo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String tiempoText = charSequence.toString();
                if (tiempoText.matches("^\\d{2}:\\d{2}$")) {
                    paso.setTiempo(tiempoText);
                } else {
                    UtilsSrv.notificacion(convertView.getContext(),
                            convertView.getContext().getString(R.string.error_formato_tiempo),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
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