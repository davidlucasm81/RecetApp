package com.david.recetapp.actividades.recetas;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;

import java.util.ArrayList;

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

        checkboxInvierno.setChecked(receta.getTemporadas().contains(Temporada.INVIERNO));
        checkboxVerano.setChecked(receta.getTemporadas().contains(Temporada.VERANO));
        checkboxOtonio.setChecked(receta.getTemporadas().contains(Temporada.OTONIO));
        checkboxPrimavera.setChecked(receta.getTemporadas().contains(Temporada.PRIMAVERA));

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