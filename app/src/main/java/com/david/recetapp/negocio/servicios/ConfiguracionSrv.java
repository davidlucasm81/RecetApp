package com.david.recetapp.negocio.servicios;

import android.content.Context;

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

public class ConfiguracionSrv {
    private static final String JSON = "configuracion.json";
    private static ConfiguracionBean configuracion;

    public static int getDiasRepeticionReceta(Context context) {
        if (configuracion == null) {
            inicializar(context);
        }
        return configuracion.getDiasRepeticionReceta();
    }

    public static void setDiasRepeticionReceta(Context context, int dias) {
        if (configuracion == null) {
            inicializar(context);
        }
        configuracion.setDiasRepeticionReceta(dias);
        guardar(context);
    }

    private static void guardar(Context context) {
        // Convertir la lista de recetas a JSON
        Gson gson = new Gson();
        String jsonRecetas = gson.toJson(configuracion);

        // Guardar el JSON en el archivo
        try {
            FileOutputStream fos = context.openFileOutput(JSON, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(jsonRecetas);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void inicializar(Context context) {
        // Cargar el archivo JSON desde el almacenamiento interno
        try {
            FileInputStream fis = context.openFileInput(JSON);
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

            // Convertir el JSON
            Gson gson = new Gson();
            Type listType = new TypeToken<ConfiguracionBean>() {
            }.getType();
            configuracion = gson.fromJson(jsonBuilder.toString(), listType);
        } catch (FileNotFoundException e) {
            configuracion = new ConfiguracionBean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ConfiguracionBean {
        private int diasRepeticionReceta;

        public ConfiguracionBean() {
            diasRepeticionReceta = 3;
        }

        public int getDiasRepeticionReceta() {
            return diasRepeticionReceta;
        }

        public void setDiasRepeticionReceta(int diasRepeticionReceta) {
            this.diasRepeticionReceta = diasRepeticionReceta;
        }
    }
}
