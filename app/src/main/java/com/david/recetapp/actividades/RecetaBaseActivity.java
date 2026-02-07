package com.david.recetapp.actividades;

import android.content.ClipData;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import androidx.core.view.ViewCompat;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
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

public abstract class RecetaBaseActivity extends AppCompatActivity {
    protected static final Pattern patternIngredient = Pattern.compile("^(.+)\\s(-?\\d+)$");
    protected static final String KEY_INGREDIENTES = "ingredientes";
    protected static final String KEY_PASOS = "pasos";
    protected EditText editTextNombre;
    protected CheckBox checkboxInvierno;
    protected CheckBox checkboxVerano;
    protected CheckBox checkboxOtonio;
    protected CheckBox checkboxPrimavera;
    protected List<Temporada> temporadas;
    protected EditText numberPickerNumeroPersonas;
    protected AutoCompleteTextView autoCompleteTextViewNombreIngrediente;
    protected EditText editTextCantidad;
    protected LinearLayout linearLayoutIngredientes;
    protected ArrayList<Ingrediente> ingredientes;
    protected LinearLayout linearLayoutListaPasos;
    protected ArrayList<Paso> pasos;
    protected int draggedItemPosition = -1;
    protected List<Alergeno> alergenos;
    protected List<Alergeno> alergenosSeleccionados;
    protected GridLayout gridLayout;
    protected RatingBar estrellas;
    protected Map<String, Integer> ingredientMap;
    protected boolean isDragging = false;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_INGREDIENTES, ingredientes);
        outState.putSerializable(KEY_PASOS, pasos);
    }

    protected void setupIngredientes(String[] ingredientList, Spinner spinner) {
        // ... Lógica para inicializar ingredientes y spinner ...
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
        ArrayAdapter<String> adapterIngrediente = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ingredientNames);
        autoCompleteTextViewNombreIngrediente.setAdapter(adapterIngrediente);
        ingredientMap = new HashMap<>();
        for (String s : ingredientList) {
            Matcher m = patternIngredient.matcher(s.trim());
            if (m.matches()) {
                ingredientMap.put(Objects.requireNonNull(m.group(1)).toLowerCase(Locale.getDefault()), Integer.parseInt(Objects.requireNonNull(m.group(2))));
            }
        }
        ingredientes = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.quantity_units));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }

    protected void agregarIngrediente(String nombre, String numero, String tipoCantidad) {
        Integer puntuacion = ingredientMap.getOrDefault(nombre.toLowerCase(Locale.getDefault()), -2);
        if (puntuacion == null) {
            puntuacion = -2; // Valor predeterminado en caso de que sea nulo
        }
        Ingrediente ingrediente = new Ingrediente(nombre, numero, tipoCantidad, puntuacion);
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
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesTipoCantidad);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCantidad.setAdapter(spinnerAdapter);
            int selectedTypeIndex = opcionesTipoCantidad.indexOf(ingrediente.getTipoCantidad());
            spinnerCantidad.setSelection(selectedTypeIndex);
            editTextNombre.setText(ingrediente.getNombre());
            editTextCantidad.setText(String.valueOf(ingrediente.getCantidad()));
            editTextNombre.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (!charSequence.toString().trim().isEmpty())
                        ingrediente.setNombre(charSequence.toString().trim());
                }
                @Override public void afterTextChanged(Editable editable) {}
            });
            editTextCantidad.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (!charSequence.toString().trim().isEmpty()) {
                        String cantidadStr = charSequence.toString().trim();
                        if (UtilsSrv.esNumeroEnteroOFraccionValida(cantidadStr)) {
                            ingrediente.setCantidad(cantidadStr);
                        }
                    }
                }
                @Override public void afterTextChanged(Editable editable) {}
            });
            spinnerCantidad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    String selectedType = (String) adapterView.getItemAtPosition(position);
                    ingrediente.setTipoCantidad(selectedType);
                }
                @Override public void onNothingSelected(AdapterView<?> adapterView) {}
            });
            ImageButton btnEliminar = ingredienteView.findViewById(R.id.btnEliminarIngrediente);
            btnEliminar.setOnClickListener(v -> {
                ingredientes.remove(ingrediente);
                mostrarIngredientes();
                UtilsSrv.notificacion(RecetaBaseActivity.this, getString(R.string.ingrediente_eliminado), Toast.LENGTH_SHORT).show();
            });
            linearLayoutIngredientes.addView(ingredienteView);
        }
    }

    protected void mostrarAlergenos() {
        gridLayout.removeAllViews();
        int columnas = 2;
        int filaActual = -1;
        for (int i = 0; i < alergenos.size(); i++) {
            if (i % columnas == 0) filaActual++;
            View alergenoView = LayoutInflater.from(this).inflate(R.layout.item_alergeno, gridLayout, false);
            ImageView imageViewIconoAlergeno = alergenoView.findViewById(R.id.imageViewIconoAlergeno);
            CheckBox checkBoxAlergeno = alergenoView.findViewById(R.id.checkBoxAlergeno);
            Alergeno alergeno = alergenos.get(i);
            imageViewIconoAlergeno.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));
            checkBoxAlergeno.setText(alergeno.getNombre());
            checkBoxAlergeno.setChecked(alergenosSeleccionados.stream().anyMatch(objeto -> alergeno.getNombre().equals(objeto.getNombre())));
            checkBoxAlergeno.setOnCheckedChangeListener((buttonView, isChecked) -> alergenosSeleccionados.add(alergeno));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(filaActual);
            params.columnSpec = GridLayout.spec(i % columnas);
            alergenoView.setLayoutParams(params);
            gridLayout.addView(alergenoView);
        }
    }

    protected int calculateTargetPosition(float y) {
        int count = linearLayoutListaPasos.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = linearLayoutListaPasos.getChildAt(i);
            if (y < child.getY() + child.getHeight() / (float) 2) {
                return i;
            }
        }
        return Math.min(Math.max(0, count), pasos.size() - 1);
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
            convertView.setOnTouchListener((view, event) -> {
                int action = event.getAction();
                boolean isTouchingReorder = false;
                ImageView imageViewReorder = convertView.findViewById(R.id.imageViewReordenar);
                if (imageViewReorder != null) {
                    Rect hitRect = new Rect();
                    imageViewReorder.getHitRect(hitRect);
                    isTouchingReorder = hitRect.contains((int) event.getX(), (int) event.getY());
                }
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (isTouchingReorder) {
                            isDragging = true;
                            for (int i = 0; i < linearLayoutListaPasos.getChildCount(); i++) {
                                View childView = linearLayoutListaPasos.getChildAt(i);
                                EditText editTextPaso1 = childView.findViewById(R.id.editTextPaso);
                                EditText editTextTiempo1 = childView.findViewById(R.id.editTextTiempo);
                                if (editTextPaso1 != null) {
                                    editTextPaso1.setFocusableInTouchMode(false);
                                    editTextPaso1.setFocusable(false);
                                    editTextPaso1.setCursorVisible(false);
                                }
                                if (editTextTiempo1 != null) {
                                    editTextTiempo1.setFocusableInTouchMode(false);
                                    editTextTiempo1.setFocusable(false);
                                    editTextTiempo1.setCursorVisible(false);
                                }
                            }
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
                        for (int i = 0; i < linearLayoutListaPasos.getChildCount(); i++) {
                            View childView = linearLayoutListaPasos.getChildAt(i);
                            EditText editTextPaso1 = childView.findViewById(R.id.editTextPaso);
                            EditText editTextTiempo1 = childView.findViewById(R.id.editTextTiempo);
                            if (editTextPaso1 != null) {
                                editTextPaso1.setFocusableInTouchMode(true);
                                editTextPaso1.setFocusable(true);
                                editTextPaso1.setCursorVisible(true);
                            }
                            if (editTextTiempo1 != null) {
                                editTextTiempo1.setFocusableInTouchMode(true);
                                editTextTiempo1.setFocusable(true);
                                editTextTiempo1.setCursorVisible(true);
                            }
                        }
                        draggedItemPosition = -1;
                        break;
                }
                return false;
            });
            if (paso != null) {
                textViewNumero.setText(String.format(Locale.getDefault(), "%d) ", position + 1));
                editTextPaso.setText(paso.getPaso());
                editTextTiempo.setText(paso.getTiempo());
                editTextPaso.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().trim().isEmpty())
                            paso.setPaso(charSequence.toString().trim());
                    }
                    @Override public void afterTextChanged(Editable editable) {}
                });
                editTextTiempo.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String tiempoText = charSequence.toString();
                        if (tiempoText.matches("^\\d{2}:\\d{2}$")) {
                            paso.setTiempo(tiempoText);
                        } else {
                            UtilsSrv.notificacion(convertView.getContext(), convertView.getContext().getString(R.string.error_formato_tiempo), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void afterTextChanged(Editable editable) {}
                });
                btnEliminarPaso.setOnClickListener(v -> {
                    pasos.remove(paso);
                    mostrarPasos();
                    UtilsSrv.notificacion(RecetaBaseActivity.this, getString(R.string.paso_eliminado), Toast.LENGTH_SHORT).show();
                });
            }
            linearLayoutListaPasos.addView(convertView);
        }
    }
}
