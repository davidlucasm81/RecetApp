package com.david.recetapp.actividades.recetas;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.servicios.RecetasSrv;

public class AnadirRecetaIAActivity extends AddRecetaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_receta_ia);

        initializeViews();
        setupIngredientesSection();
        setupPasosSection();
        setupAlergenosSection();
        setupCrearButton();

        Receta recetaIA = getIntent().getParcelableExtra("receta_ia", Receta.class);
        if (recetaIA != null) {
            rellenarCampos(recetaIA);
        }
    }

    private void rellenarCampos(Receta receta) {
        editTextNombre.setText(receta.getNombre());
        numberPickerNumeroPersonas.setText(String.valueOf(receta.getNumPersonas()));
        estrellas.setRating(receta.getEstrellas());
        
        if (editTextYoutubeUrl != null) {
            editTextYoutubeUrl.setText(receta.getYoutubeUrl());
        }

        chipInvierno.setChecked(receta.getTemporadas().contains(Temporada.INVIERNO));
        chipVerano.setChecked(receta.getTemporadas().contains(Temporada.VERANO));
        chipOtonio.setChecked(receta.getTemporadas().contains(Temporada.OTONIO));
        chipPrimavera.setChecked(receta.getTemporadas().contains(Temporada.PRIMAVERA));

        AutoCompleteTextView spinnerTipo = findViewById(R.id.spinnerTipoReceta);
        String[] tiposArr = getResources().getStringArray(R.array.tipos_receta);
        if (receta.getTipoReceta() != null && receta.getTipoReceta().ordinal() < tiposArr.length) {
            spinnerTipo.setText(tiposArr[receta.getTipoReceta().ordinal()], false);
            if (receta.getTipoReceta() == TipoReceta.PRINCIPAL) {
                findViewById(R.id.layoutMomentoReceta).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.layoutMomentoReceta).setVisibility(View.GONE);
            }
        }

        AutoCompleteTextView spinnerMomento = findViewById(R.id.spinnerMomentoReceta);
        String[] momentosArr = getResources().getStringArray(R.array.momentos_receta);
        if (receta.getMomentoReceta() != null && receta.getMomentoReceta().ordinal() < momentosArr.length) {
            spinnerMomento.setText(momentosArr[receta.getMomentoReceta().ordinal()], false);
        }

        if (receta.getIngredientes() != null) {
            for (Ingrediente ing : receta.getIngredientes()) {
                String nombre = ing.getNombre();
                if (nombre != null) {
                    // Si el ingrediente es conocido, usamos su puntuación oficial/custom en lugar de la de la IA (a menos que sea -2)
                    if (RecetasSrv.isIngredienteConocido(nombre)) {
                        Integer punt = RecetasSrv.getScoreFromCaches(nombre);
                        if (punt != null) {
                            ing.setPuntuacion(punt);
                        }
                    } else if (ing.getPuntuacion() == -2 || ing.getPuntuacion() == 0) {
                        // Si la IA no dio puntuación o dio 0 por error, marcamos como -1 para revisión
                        ing.setPuntuacion(-1);
                    }
                }
            }
            ingredientes.addAll(receta.getIngredientes());
            mostrarIngredientes();
            actualizarSpinnersSustitutos();
        }

        if (receta.getPasos() != null) {
            pasos.addAll(receta.getPasos());
            mostrarPasos();
        }

        if (receta.getAlergenos() != null) {
            alergenosSeleccionados.addAll(receta.getAlergenos());
            mostrarAlergenos();
        }
    }
}