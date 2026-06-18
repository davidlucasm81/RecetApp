package com.david.recetapp.actividades.recetas;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.MomentoReceta;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IAInputActivity extends AppCompatActivity {
    private static final String TAG = "IAInputActivity";

    private EditText editTextDescIA;
    private Button btnGenerarPrompt;
    private Button btnCopiarPrompt;
    private EditText editTextJsonIA;
    private Button btnProcesarIA;
    private String generatedPrompt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ia_input);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        editTextDescIA = findViewById(R.id.editTextDescIA);
        btnGenerarPrompt = findViewById(R.id.btnGenerarPrompt);
        btnCopiarPrompt = findViewById(R.id.btnCopiarPrompt);
        editTextJsonIA = findViewById(R.id.editTextJsonIA);
        btnProcesarIA = findViewById(R.id.btnProcesarIA);
    }

    private void setupListeners() {
        btnGenerarPrompt.setOnClickListener(v -> generarPrompt());
        btnCopiarPrompt.setOnClickListener(v -> copiarPrompt());
        btnProcesarIA.setOnClickListener(v -> procesarJSON());
    }

    private void generarPrompt() {
        String descripcion = editTextDescIA.getText().toString().trim();
        if (descripcion.isEmpty()) {
            UtilsSrv.notificacion(this, "Describe la receta primero", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] quantityUnits = getResources().getStringArray(R.array.quantity_units);
        String unidadesStr = String.join(", ", quantityUnits);

        String[] alergenosArr = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
        String alergenosStr = String.join(", ", alergenosArr);

        generatedPrompt = getString(R.string.ia_prompt_template, descripcion, unidadesStr, alergenosStr);
        
        btnCopiarPrompt.setVisibility(View.VISIBLE);
        UtilsSrv.notificacion(this, "Prompt generado. Cópialo y úsalo en tu IA.", Toast.LENGTH_LONG).show();
    }

    private void copiarPrompt() {
        if (generatedPrompt.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recipe Prompt", generatedPrompt);
        clipboard.setPrimaryClip(clip);
        UtilsSrv.notificacion(this, "Prompt copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void procesarJSON() {
        String jsonInput = editTextJsonIA.getText().toString().trim();
        if (jsonInput.isEmpty()) {
            UtilsSrv.notificacion(this, "Pega el JSON primero", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (jsonInput.startsWith("```json")) {
                jsonInput = jsonInput.substring(7);
            }
            if (jsonInput.endsWith("```")) {
                jsonInput = jsonInput.substring(0, jsonInput.length() - 3);
            }
            jsonInput = jsonInput.trim();

            JSONObject json = new JSONObject(jsonInput);
            Receta receta = parseReceta(json);
            
            Intent intent = new Intent(this, AnadirRecetaIAActivity.class);
            intent.putExtra("receta_ia", receta);
            startActivity(intent);
            finish();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parseando JSON", e);
            UtilsSrv.notificacion(this, getString(R.string.ia_error_parseo), Toast.LENGTH_LONG).show();
        }
    }

    private Receta parseReceta(JSONObject json) throws JSONException {
        Receta receta = new Receta();
        receta.setNombre(json.optString("nombre", ""));
        
        String tipoStr = json.optString("tipoReceta", "PRINCIPAL").toUpperCase();
        try {
            receta.setTipoReceta(TipoReceta.valueOf(tipoStr));
        } catch (IllegalArgumentException ignored) {}

        String momentoStr = json.optString("momentoReceta", "AMBOS").toUpperCase();
        try {
            receta.setMomentoReceta(MomentoReceta.valueOf(momentoStr));
        } catch (IllegalArgumentException ignored) {}

        receta.setNumPersonas(json.optInt("numPersonas", 4));
        receta.setEstrellas(0f);

        JSONArray temps = json.optJSONArray("temporadas");
        List<Temporada> temporadas = new ArrayList<>();
        if (temps != null) {
            for (int i = 0; i < temps.length(); i++) {
                try {
                    temporadas.add(Temporada.valueOf(temps.getString(i).toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        receta.setTemporadas(temporadas);

        JSONArray alers = json.optJSONArray("alergenos");
        List<com.david.recetapp.negocio.beans.Alergeno> alergenosList = new ArrayList<>();
        if (alers != null) {
            String[] alergenosNombresArray = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
            for (int i = 0; i < alers.length(); i++) {
                String alerNombre = alers.getString(i);
                for (int j = 0; j < alergenosNombresArray.length; j++) {
                    if (alergenosNombresArray[j].equalsIgnoreCase(alerNombre)) {
                        alergenosList.add(new com.david.recetapp.negocio.beans.Alergeno(alergenosNombresArray[j], j));
                        break;
                    }
                }
            }
        }
        receta.setAlergenos(alergenosList);

        JSONArray ings = json.optJSONArray("ingredientes");
        List<Ingrediente> ingredientes = new ArrayList<>();
        if (ings != null) {
            for (int i = 0; i < ings.length(); i++) {
                JSONObject ingJson = ings.getJSONObject(i);
                String sustituto = ingJson.isNull("esSustitutoDe") ? null : ingJson.optString("esSustitutoDe");
                ingredientes.add(new Ingrediente(
                        ingJson.optString("nombre", ""),
                        ingJson.optString("cantidad", "1"),
                        ingJson.optString("tipoCantidad", "unidad"),
                        -2,
                        ingJson.optBoolean("opcional", false),
                        sustituto
                ));
            }
        }
        receta.setIngredientes(ingredientes);

        JSONArray ps = json.optJSONArray("pasos");
        List<Paso> pasos = new ArrayList<>();
        if (ps != null) {
            for (int i = 0; i < ps.length(); i++) {
                JSONObject pasoJson = ps.getJSONObject(i);
                pasos.add(new Paso(
                        pasoJson.optString("paso", ""),
                        pasoJson.optString("tiempo", "00:00")
                ));
            }
        }
        receta.setPasos(pasos);

        return receta;
    }
}