package com.david.recetapp.actividades;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.google.gson.Gson;
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
        // Controla el comportamiento del botón "Atrás"
        Intent intent = new Intent(ImportExportActivity.this, VerRecetasActivity.class);
        startActivity(intent);
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
        mGetContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                this::onFileSelected);
        // Solicitar permisos
        checkAndRequestPermissions();
    }

    private void onFileSelected(Uri fileUri) {
        if (fileUri != null) {
            try {
                String jsonData = leerArchivoJSON(fileUri);
                List<Receta> listaRecetasImportadas = convertirJsonAListaRecetas(jsonData);
                mergeListaRecetas(listaRecetasImportadas);
                UtilsSrv.notificacion(this, getString(R.string.importacion_ok), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                UtilsSrv.notificacion(this, getString(R.string.importacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @noinspection ResultOfMethodCallIgnored
     */
    private void exportarListaRecetas() {
        List<Receta> listaRecetas = obtenerListaRecetas();

        String jsonData = convertirListaAJson(listaRecetas);

        try {
            // Crear el directorio en el almacenamiento público
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (publicDir != null) {
                //noinspection ResultOfMethodCallIgnored
                publicDir.mkdirs(); // Asegurarse de que el directorio exista

                // Crear el archivo en el directorio público
                File file = new File(publicDir, RecetasSrv.JSON);
                FileWriter writer = new FileWriter(file);
                writer.write(jsonData);
                writer.close();

                // Compartir el archivo con cualquier aplicación
                Uri fileUri = FileProvider.getUriForFile(this, "com.david.recetapp.provider", file);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.compartir_recetas)));
            } else {
                UtilsSrv.notificacion(this, getString(R.string.exportacion_error) + ": publicDir is null", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            UtilsSrv.notificacion(this, getString(R.string.exportacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                UtilsSrv.notificacion(this, getString(R.string.importacion_ok), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                UtilsSrv.notificacion(this, getString(R.string.importacion_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<Receta> obtenerListaRecetas() {
        return RecetasSrv.cargarListaRecetas(this);
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
        // Convertir el JSON a una lista de objetos Receta utilizando GSON
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Receta>>() {
        }.getType();
        // Agregar las recetas a la cola
        List<Receta> listaRecetas = gson.fromJson(jsonData, listType);
        return listaRecetas != null ? listaRecetas : new ArrayList<>();
    }

    private void mergeListaRecetas(List<Receta> listaRecetasImportadas) {
        List<Receta> listaRecetasActuales = obtenerListaRecetas();
        HashSet<Receta> recetasUnicas = new HashSet<>(listaRecetasActuales);

        for (Receta recetaImportada : listaRecetasImportadas) {
            // Establecer el atributo "shared" como true en las recetas importadas
            recetaImportada.setShared(true);
            recetaImportada.setFechaCalendario(new Date(0));
            recetasUnicas.add(recetaImportada);
        }

        List<Receta> listaRecetasMerge = new ArrayList<>(recetasUnicas);
        guardarListaRecetas(listaRecetasMerge);
    }

    private void guardarListaRecetas(List<Receta> listaRecetas) {
        for (Receta receta : listaRecetas)
            RecetasSrv.addReceta(this, receta);
    }

}