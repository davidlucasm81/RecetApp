package com.david.recetapp.actividades;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.FirebaseManager;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ImportExportActivity extends AppCompatActivity {

    private static final int PICK_JSON_FILE_REQUEST = 1;
    private ActivityResultLauncher<String> mGetContentLauncher;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    private final FirebaseManager firebaseManager = new FirebaseManager();

    private void checkAndRequestPermissions() {
        int permissionReadStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        List<String> permissions = new ArrayList<>();

        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Devolver que no se importó nada si el usuario sale manualmente
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export);

        Button importButton = findViewById(R.id.importButton);
        Button exportButton = findViewById(R.id.exportButton);

        importButton.setOnClickListener(v -> mGetContentLauncher.launch("application/json"));
        exportButton.setOnClickListener(v -> exportarListaRecetas());

        // Inicializar el lanzador de resultados para la selección de archivos
        mGetContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onFileSelected);
        // Solicitar permisos
        checkAndRequestPermissions();
    }

    private void onFileSelected(Uri fileUri) {
        if (fileUri != null) {
            try {
                String jsonData = leerArchivoJSON(fileUri);
                List<Receta> listaRecetasImportadas = convertirJsonAListaRecetas(jsonData);
                mergeListaRecetas(listaRecetasImportadas);
            } catch (IOException e) {
                UtilsSrv.notificacion(this, getString(R.string.importacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportarListaRecetas() {
        // Mostrar progreso
        UtilsSrv.notificacion(this, getString(R.string.exportando_recetas), Toast.LENGTH_SHORT).show();

        RecetasSrv.cargarListaRecetas(this, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> listaRecetas) {
                String jsonData = convertirListaAJson(listaRecetas);

                try {
                    // Crear el archivo en el almacenamiento específico de la aplicación
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "lista_recetas_firebase.json");
                    FileWriter writer = new FileWriter(file);
                    writer.write(jsonData);
                    writer.close();

                    // Compartir el archivo con cualquier aplicación
                    Uri fileUri = FileProvider.getUriForFile(ImportExportActivity.this, "com.david.recetapp.provider", file);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/json");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.compartir_recetas)));

                    UtilsSrv.notificacion(ImportExportActivity.this, getString(R.string.exportacion_ok), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    UtilsSrv.notificacion(ImportExportActivity.this, getString(R.string.exportacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                UtilsSrv.notificacion(ImportExportActivity.this, getString(R.string.exportacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_JSON_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            try {
                String jsonData = leerArchivoJSON(fileUri);
                List<Receta> listaRecetasImportadas = convertirJsonAListaRecetas(jsonData);
                mergeListaRecetas(listaRecetasImportadas);
            } catch (IOException e) {
                UtilsSrv.notificacion(this, getString(R.string.importacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String convertirListaAJson(List<Receta> listaRecetas) {
        Gson gson = new Gson();
        return gson.toJson(listaRecetas);
    }

    private String leerArchivoJSON(Uri fileUri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        ContentResolver contentResolver = getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(fileUri);
        if (inputStream != null) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
        }

        return stringBuilder.toString();
    }

    private List<Receta> convertirJsonAListaRecetas(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) return new ArrayList<>();

        // Gson con deserializer tolerante para Date
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (jsonElem, type, ctx) -> {
                    if (jsonElem == null || jsonElem.isJsonNull()) return null;

                    if (jsonElem.isJsonPrimitive()) {
                        if (jsonElem.getAsJsonPrimitive().isNumber()) {
                            // Número → milisegundos
                            return new Date(jsonElem.getAsLong());
                        } else if (jsonElem.getAsJsonPrimitive().isString()) {
                            String s = jsonElem.getAsString();
                            // Intentar varios formatos
                            return parseDateString(s);
                        }
                    }

                    // fallback
                    return null;
                })
                .create();

        Type listType = new TypeToken<List<Receta>>() {}.getType();
        try {
            return gson.fromJson(jsonData, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Helper para parsear strings como "Jan 4, 2026 10:54:20"
    private static Date parseDateString(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        String[] patterns = new String[] {
                "MMM d, yyyy HH:mm:ss",      // Jan 4, 2026 10:54:20
                "MMM dd, yyyy HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",  // ISO
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };

        for (String p : patterns) {
            try {
                return new java.text.SimpleDateFormat(p, java.util.Locale.ENGLISH).parse(s);
            } catch (Exception ignored) {}
        }

        return null;
    }


    private void mergeListaRecetas(List<Receta> listaRecetasImportadas) {
        if (listaRecetasImportadas.isEmpty()) {
            UtilsSrv.notificacion(this, getString(R.string.no_recetas_importar), Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        UtilsSrv.notificacion(this, getString(R.string.importando_recetas) + " " + listaRecetasImportadas.size(), Toast.LENGTH_SHORT).show();

        // Primero cargar las recetas actuales
        RecetasSrv.cargarListaRecetas(this, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> listaRecetasActuales) {
                HashSet<String> idsExistentes = new HashSet<>();
                for (Receta receta : listaRecetasActuales) {
                    idsExistentes.add(receta.getId());
                }

                // Filtrar recetas que no existen
                List<Receta> recetasNuevas = new ArrayList<>();
                for (Receta recetaImportada : listaRecetasImportadas) {
                    if (!idsExistentes.contains(recetaImportada.getId())) {
                        // Establecer atributos para recetas importadas
                        recetaImportada.setShared(true);
                        recetaImportada.setFechaCalendario(new Date(0));
                        recetasNuevas.add(recetaImportada);
                    }
                }

                if (recetasNuevas.isEmpty()) {
                    String msg = getString(R.string.todas_recetas_ya_existen);
                    Log.d("ImportExportActivity", "No new recipes to import. Msg: " + msg);
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
                    return;
                }


                // Importar usando batch en Firebase
                firebaseManager.importarRecetas(recetasNuevas, new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            String msg = getString(R.string.importacion_ok) + ": " + recetasNuevas.size() + " recetas";
                            Log.d("ImportExportActivity", "Import finished OK -> setting RESULT_OK. Msg: " + msg);
                            // Mostrar Toast explícito con application context (más fiable si se hace justo antes de finish())
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                            // Devolver resultado OK antes de cerrar
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        UtilsSrv.notificacion(ImportExportActivity.this,
                                getString(R.string.importacion_error) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        // No cerramos la activity para que el usuario revise el error
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Si falla la carga, importar de todas formas
                UtilsSrv.notificacion(ImportExportActivity.this, getString(R.string.importando_todas_recetas), Toast.LENGTH_SHORT).show();

                for (Receta receta : listaRecetasImportadas) {
                    receta.setShared(true);
                    receta.setFechaCalendario(new Date(0));
                }

                firebaseManager.importarRecetas(listaRecetasImportadas, new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            String msg = getString(R.string.importacion_ok) + ": " + listaRecetasImportadas.size() + " recetas";
                            Log.d("ImportExportActivity", "Import finished OK -> setting RESULT_OK. Msg: " + msg);
                            // Mostrar Toast explícito con application context (más fiable si se hace justo antes de finish())
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                            // Devolver resultado OK antes de cerrar
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        UtilsSrv.notificacion(ImportExportActivity.this,
                                getString(R.string.importacion_error) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
}