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

import androidx.annotation.NonNull;
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
    private EditText editTextYoutubeIA;
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
        editTextYoutubeIA = findViewById(R.id.editTextYoutubeIA);
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
        String youtubeUrl = editTextYoutubeIA.getText().toString().trim();
        if (descripcion.isEmpty() && youtubeUrl.isEmpty()) {
            UtilsSrv.notificacion(this, "Proporciona una descripción o un enlace de YouTube", Toast.LENGTH_SHORT).show();
            return;
        }

        String instruccion;
        if (!descripcion.isEmpty() && !youtubeUrl.isEmpty()) {
            instruccion = "Extrae la receta de este vídeo de YouTube: " + youtubeUrl + " y ten en cuenta esta descripción adicional: " + descripcion;
        } else if (!youtubeUrl.isEmpty()) {
            instruccion = "Extrae la receta detallada de este vídeo de YouTube: " + youtubeUrl;
        } else {
            instruccion = "Genera una receta detallada basada en esta descripción: " + descripcion;
        }

        String[] quantityUnits = getResources().getStringArray(R.array.quantity_units);
        String unidadesStr = String.join(", ", quantityUnits);

        String[] alergenosArr = getResources().getStringArray(R.array.alergenos_conocidos_nombres);
        String alergenosStr = String.join(", ", alergenosArr);

        generatedPrompt = getString(R.string.ia_prompt_template, instruccion, unidadesStr, alergenosStr);
        
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
        String rawJson = editTextJsonIA.getText().toString().trim();
        if (rawJson.isEmpty()) {
            UtilsSrv.notificacion(this, "Pega el JSON primero", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = getJsonObject(rawJson);
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

    @NonNull
    private static JSONObject getJsonObject(String rawJson) throws JSONException {
        String jsonInput = rawJson;

        // Robust extraction of JSON object: find first { and last }
        int firstBrace = jsonInput.indexOf('{');
        int lastBrace = jsonInput.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            jsonInput = jsonInput.substring(firstBrace, lastBrace + 1);
        }
        jsonInput = jsonInput.trim();

        return new JSONObject(jsonInput);
    }

    private Receta parseReceta(JSONObject json) throws JSONException {
        Receta receta = new Receta();
        receta.setId(java.util.UUID.randomUUID().toString());
        receta.setNombre(json.optString("nombre", ""));
        
        String urlFromJson = json.optString("youtubeUrl", "");
        if (urlFromJson.isEmpty()) {
            // Si el JSON no trae URL, usamos la que el usuario puso originalmente en el campo de arriba
            urlFromJson = editTextYoutubeIA.getText().toString().trim();
        }
        receta.setYoutubeUrl(urlFromJson);
        
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
