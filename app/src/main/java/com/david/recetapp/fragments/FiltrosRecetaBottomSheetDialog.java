package com.david.recetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.FiltroRecetas;
import com.david.recetapp.negocio.beans.MomentoReceta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FiltrosRecetaBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_FILTRO = "arg_filtro";

    public interface OnFiltrosAplicadosListener {
        void onFiltrosAplicados(FiltroRecetas filtro);
    }

    private OnFiltrosAplicadosListener listener;
    private FiltroRecetas filtroWork;

    // UI elements
    private ChipGroup chipGroupTipo;
    private ChipGroup chipGroupMomento;
    private ChipGroup chipGroupTemporadas;
    private Slider sliderEstrellas;
    private TextView txtValEstrellas;
    private Slider sliderSalud;
    private TextView txtValSalud;
    private ChipGroup chipGroupAlergenos;
    private Slider sliderTiempoMax;
    private TextView txtValTiempo;
    private CheckBox cbSoloSinPuntuar;

    public static FiltrosRecetaBottomSheetDialog newInstance(FiltroRecetas filtroActual) {
        FiltrosRecetaBottomSheetDialog dialog = new FiltrosRecetaBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILTRO, filtroActual);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnFiltrosAplicadosListener(OnFiltrosAplicadosListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filtros_recetas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            FiltroRecetas fArg = getArguments().getSerializable(ARG_FILTRO, FiltroRecetas.class);
            filtroWork = new FiltroRecetas(fArg);
        } else {
            filtroWork = new FiltroRecetas();
        }

        initializeViews(view);
        populateViewsFromFilter();
        setupListeners(view);
    }

    private void initializeViews(View view) {
        chipGroupTipo = view.findViewById(R.id.chipGroupTipo);
        chipGroupMomento = view.findViewById(R.id.chipGroupMomento);
        chipGroupTemporadas = view.findViewById(R.id.chipGroupTemporadas);
        sliderEstrellas = view.findViewById(R.id.sliderEstrellas);
        txtValEstrellas = view.findViewById(R.id.txtValEstrellas);
        sliderSalud = view.findViewById(R.id.sliderSalud);
        txtValSalud = view.findViewById(R.id.txtValSalud);
        chipGroupAlergenos = view.findViewById(R.id.chipGroupAlergenos);
        sliderTiempoMax = view.findViewById(R.id.sliderTiempoMax);
        txtValTiempo = view.findViewById(R.id.txtValTiempo);
        cbSoloSinPuntuar = view.findViewById(R.id.cbSoloSinPuntuar);
    }

    private void populateViewsFromFilter() {
        // 1. Tipo
        TipoReceta tr = filtroWork.getTipoReceta();
        int checkTipoId = R.id.chipTipoTodos;
        if (tr == TipoReceta.PRINCIPAL) checkTipoId = R.id.chipTipoPrincipal;
        else if (tr == TipoReceta.POSTRE) checkTipoId = R.id.chipTipoPostre;
        else if (tr == TipoReceta.COCTEL) checkTipoId = R.id.chipTipoCoctel;
        else if (tr == TipoReceta.SIDE) checkTipoId = R.id.chipTipoSide;
        chipGroupTipo.check(checkTipoId);

        // 2. Momento
        MomentoReceta mr = filtroWork.getMomentoReceta();
        int checkMomentoId = R.id.chipMomentoTodos;
        if (mr == MomentoReceta.COMIDA) checkMomentoId = R.id.chipMomentoComida;
        else if (mr == MomentoReceta.CENA) checkMomentoId = R.id.chipMomentoCena;
        chipGroupMomento.check(checkMomentoId);

        // 3. Temporadas
        Set<Temporada> temps = filtroWork.getTemporadas();
        chipGroupTemporadas.clearCheck();
        if (temps != null) {
            if (temps.contains(Temporada.PRIMAVERA)) checkChip(chipGroupTemporadas, R.id.chipTempPrimavera);
            if (temps.contains(Temporada.VERANO)) checkChip(chipGroupTemporadas, R.id.chipTempVerano);
            if (temps.contains(Temporada.OTONIO)) checkChip(chipGroupTemporadas, R.id.chipTempOtonio);
            if (temps.contains(Temporada.INVIERNO)) checkChip(chipGroupTemporadas, R.id.chipTempInvierno);
        }

        // 4. Estrellas (Slider)
        float minEst = filtroWork.getMinEstrellas();
        sliderEstrellas.setValue(Math.min(Math.max(minEst, 0.0f), 5.0f));
        actualizarTextoEstrellas(minEst);

        // 5. Salubridad (Slider)
        float minSalud = (float) filtroWork.getMinPuntuacionSalud();
        sliderSalud.setValue(Math.min(Math.max(minSalud, 0.0f), 10.0f));
        actualizarTextoSalud(minSalud);

        // 6. Alérgenos
        Set<Integer> algs = filtroWork.getAlergenosExcluidos();
        chipGroupAlergenos.clearCheck();
        if (algs != null) {
            if (algs.contains(0)) checkChip(chipGroupAlergenos, R.id.chipAlergenoGluten);
            if (algs.contains(1)) checkChip(chipGroupAlergenos, R.id.chipAlergenoLacteos);
            if (algs.contains(2)) checkChip(chipGroupAlergenos, R.id.chipAlergenoFrutosSecos);
            if (algs.contains(3)) checkChip(chipGroupAlergenos, R.id.chipAlergenoSoja);
            if (algs.contains(4)) checkChip(chipGroupAlergenos, R.id.chipAlergenoPescado);
            if (algs.contains(5)) checkChip(chipGroupAlergenos, R.id.chipAlergenoMariscos);
            if (algs.contains(6)) checkChip(chipGroupAlergenos, R.id.chipAlergenoHuevos);
        }

        // 7. Tiempo Max (Slider)
        int maxT = filtroWork.getMaxTiempoMinutos();
        sliderTiempoMax.setValue(Math.min(Math.max((float) maxT, 0.0f), 120.0f));
        actualizarTextoTiempo(maxT);

        // 8. Solo sin puntuar
        cbSoloSinPuntuar.setChecked(filtroWork.isSoloIngredientesSinPuntuar());
    }

    private void setupListeners(View view) {
        sliderEstrellas.addOnChangeListener((slider, value, fromUser) -> actualizarTextoEstrellas(value));
        sliderSalud.addOnChangeListener((slider, value, fromUser) -> actualizarTextoSalud(value));
        sliderTiempoMax.addOnChangeListener((slider, value, fromUser) -> actualizarTextoTiempo(value));

        view.findViewById(R.id.btnRestablecer).setOnClickListener(v -> {
            filtroWork.reset();
            populateViewsFromFilter();
        });

        view.findViewById(R.id.btnAplicarFiltros).setOnClickListener(v -> {
            recogerValoresUI();
            if (listener != null) {
                listener.onFiltrosAplicados(filtroWork);
            }
            dismiss();
        });
    }

    private void recogerValoresUI() {
        // 1. Tipo
        int tipoId = chipGroupTipo.getCheckedChipId();
        if (tipoId == R.id.chipTipoPrincipal) filtroWork.setTipoReceta(TipoReceta.PRINCIPAL);
        else if (tipoId == R.id.chipTipoPostre) filtroWork.setTipoReceta(TipoReceta.POSTRE);
        else if (tipoId == R.id.chipTipoCoctel) filtroWork.setTipoReceta(TipoReceta.COCTEL);
        else if (tipoId == R.id.chipTipoSide) filtroWork.setTipoReceta(TipoReceta.SIDE);
        else filtroWork.setTipoReceta(null);

        // 2. Momento
        int momentoId = chipGroupMomento.getCheckedChipId();
        if (momentoId == R.id.chipMomentoComida) filtroWork.setMomentoReceta(MomentoReceta.COMIDA);
        else if (momentoId == R.id.chipMomentoCena) filtroWork.setMomentoReceta(MomentoReceta.CENA);
        else filtroWork.setMomentoReceta(null);

        // 3. Temporadas
        Set<Temporada> temps = new HashSet<>();
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempPrimavera)) temps.add(Temporada.PRIMAVERA);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempVerano)) temps.add(Temporada.VERANO);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempOtonio)) temps.add(Temporada.OTONIO);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempInvierno)) temps.add(Temporada.INVIERNO);
        filtroWork.setTemporadas(temps);

        // 4. Estrellas (Slider)
        filtroWork.setMinEstrellas(sliderEstrellas.getValue());

        // 5. Salubridad (Slider)
        filtroWork.setMinPuntuacionSalud(sliderSalud.getValue());

        // 6. Alérgenos
        Set<Integer> algs = new HashSet<>();
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoGluten)) algs.add(0);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoLacteos)) algs.add(1);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoFrutosSecos)) algs.add(2);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoSoja)) algs.add(3);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoPescado)) algs.add(4);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoMariscos)) algs.add(5);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoHuevos)) algs.add(6);
        filtroWork.setAlergenosExcluidos(algs);

        // 7. Tiempo Max (Slider)
        filtroWork.setMaxTiempoMinutos((int) sliderTiempoMax.getValue());

        // 8. Solo sin puntuar
        filtroWork.setSoloIngredientesSinPuntuar(cbSoloSinPuntuar.isChecked());
    }

    private void actualizarTextoEstrellas(float value) {
        if (value <= 0.0f) {
            txtValEstrellas.setText(R.string.cualquiera);
        } else {
            txtValEstrellas.setText(String.format(Locale.getDefault(), "≥ %.0f★", value));
        }
    }

    private void actualizarTextoSalud(float value) {
        if (value <= 0.0f) {
            txtValSalud.setText(R.string.cualquiera);
        } else {
            txtValSalud.setText(String.format(Locale.getDefault(), "≥ %.1f", value));
        }
    }

    private void actualizarTextoTiempo(float value) {
        if (value <= 0.0f) {
            txtValTiempo.setText(R.string.cualquiera);
        } else {
            txtValTiempo.setText(String.format(Locale.getDefault(), "≤ %.0f min", value));
        }
    }

    private void checkChip(ChipGroup group, int chipId) {
        Chip chip = group.findViewById(chipId);
        if (chip != null) chip.setChecked(true);
    }

    private boolean isChipChecked(ChipGroup group, int chipId) {
        Chip chip = group.findViewById(chipId);
        return chip != null && chip.isChecked();
    }
}
