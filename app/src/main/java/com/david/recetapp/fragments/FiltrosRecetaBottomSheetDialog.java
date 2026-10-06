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
    private ChipGroup chipGroupOrden;
    private ChipGroup chipGroupTipo;
    private ChipGroup chipGroupMomento;
    private ChipGroup chipGroupTemporadas;
    private ChipGroup chipGroupEstrellas;
    private Slider sliderSalud;
    private TextView txtValSalud;
    private ChipGroup chipGroupAlergenos;
    private ChipGroup chipGroupTiempoMax;
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
        chipGroupOrden = view.findViewById(R.id.chipGroupOrden);
        chipGroupTipo = view.findViewById(R.id.chipGroupTipo);
        chipGroupMomento = view.findViewById(R.id.chipGroupMomento);
        chipGroupTemporadas = view.findViewById(R.id.chipGroupTemporadas);
        chipGroupEstrellas = view.findViewById(R.id.chipGroupEstrellas);
        sliderSalud = view.findViewById(R.id.sliderSalud);
        txtValSalud = view.findViewById(R.id.txtValSalud);
        chipGroupAlergenos = view.findViewById(R.id.chipGroupAlergenos);
        chipGroupTiempoMax = view.findViewById(R.id.chipGroupTiempoMax);
        cbSoloSinPuntuar = view.findViewById(R.id.cbSoloSinPuntuar);
    }

    private void populateViewsFromFilter() {
        // 1. Orden
        FiltroRecetas.CriterioOrden co = filtroWork.getCriterioOrden();
        if (co == null) co = FiltroRecetas.CriterioOrden.NOMBRE_ASC;
        int checkOrdenId = switch (co) {
            case NOMBRE_DESC -> R.id.chipOrdenNombreDesc;
            case ESTRELLAS_DESC -> R.id.chipOrdenEstrellasDesc;
            case SALUD_DESC -> R.id.chipOrdenSaludDesc;
            case TIEMPO_ASC -> R.id.chipOrdenTiempoAsc;
            case FECHA_DESC -> R.id.chipOrdenFechaDesc;
            default -> R.id.chipOrdenNombreAsc;
        };
        chipGroupOrden.check(checkOrdenId);

        // 2. Tipo
        TipoReceta tr = filtroWork.getTipoReceta();
        int checkTipoId = R.id.chipTipoTodos;
        if (tr == TipoReceta.PRINCIPAL) checkTipoId = R.id.chipTipoPrincipal;
        else if (tr == TipoReceta.POSTRE) checkTipoId = R.id.chipTipoPostre;
        else if (tr == TipoReceta.COCTEL) checkTipoId = R.id.chipTipoCoctel;
        else if (tr == TipoReceta.SIDE) checkTipoId = R.id.chipTipoSide;
        chipGroupTipo.check(checkTipoId);

        // 3. Momento
        MomentoReceta mr = filtroWork.getMomentoReceta();
        int checkMomentoId = R.id.chipMomentoTodos;
        if (mr == MomentoReceta.COMIDA) checkMomentoId = R.id.chipMomentoComida;
        else if (mr == MomentoReceta.CENA) checkMomentoId = R.id.chipMomentoCena;
        chipGroupMomento.check(checkMomentoId);

        // 4. Temporadas
        Set<Temporada> temps = filtroWork.getTemporadas();
        chipGroupTemporadas.clearCheck();
        if (temps != null) {
            if (temps.contains(Temporada.PRIMAVERA)) checkChip(chipGroupTemporadas, R.id.chipTempPrimavera);
            if (temps.contains(Temporada.VERANO)) checkChip(chipGroupTemporadas, R.id.chipTempVerano);
            if (temps.contains(Temporada.OTONIO)) checkChip(chipGroupTemporadas, R.id.chipTempOtonio);
            if (temps.contains(Temporada.INVIERNO)) checkChip(chipGroupTemporadas, R.id.chipTempInvierno);
        }

        // 5. Estrellas
        float minEst = filtroWork.getMinEstrellas();
        int checkEstId = R.id.chipEstrellas0;
        if (minEst >= 5.0f) checkEstId = R.id.chipEstrellas5;
        else if (minEst >= 4.0f) checkEstId = R.id.chipEstrellas4;
        else if (minEst >= 3.0f) checkEstId = R.id.chipEstrellas3;
        else if (minEst >= 2.0f) checkEstId = R.id.chipEstrellas2;
        else if (minEst >= 1.0f) checkEstId = R.id.chipEstrellas1;
        chipGroupEstrellas.check(checkEstId);

        // 6. Salubridad
        float minSalud = (float) filtroWork.getMinPuntuacionSalud();
        sliderSalud.setValue(Math.min(Math.max(minSalud, 0.0f), 10.0f));
        actualizarTextoSalud(minSalud);

        // 7. Alérgenos
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

        // 8. Tiempo Max
        int maxT = filtroWork.getMaxTiempoMinutos();
        int checkTiempoId = R.id.chipTiempo0;
        if (maxT == 15) checkTiempoId = R.id.chipTiempo15;
        else if (maxT == 30) checkTiempoId = R.id.chipTiempo30;
        else if (maxT == 45) checkTiempoId = R.id.chipTiempo45;
        else if (maxT >= 60) checkTiempoId = R.id.chipTiempo60;
        chipGroupTiempoMax.check(checkTiempoId);

        // 9. Solo sin puntuar
        cbSoloSinPuntuar.setChecked(filtroWork.isSoloIngredientesSinPuntuar());
    }

    private void setupListeners(View view) {
        sliderSalud.addOnChangeListener((slider, value, fromUser) -> actualizarTextoSalud(value));

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
        // 1. Orden
        int ordenId = chipGroupOrden.getCheckedChipId();
        if (ordenId == R.id.chipOrdenNombreDesc) filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.NOMBRE_DESC);
        else if (ordenId == R.id.chipOrdenEstrellasDesc) filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.ESTRELLAS_DESC);
        else if (ordenId == R.id.chipOrdenSaludDesc) filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.SALUD_DESC);
        else if (ordenId == R.id.chipOrdenTiempoAsc) filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.TIEMPO_ASC);
        else if (ordenId == R.id.chipOrdenFechaDesc) filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.FECHA_DESC);
        else filtroWork.setCriterioOrden(FiltroRecetas.CriterioOrden.NOMBRE_ASC);

        // 2. Tipo
        int tipoId = chipGroupTipo.getCheckedChipId();
        if (tipoId == R.id.chipTipoPrincipal) filtroWork.setTipoReceta(TipoReceta.PRINCIPAL);
        else if (tipoId == R.id.chipTipoPostre) filtroWork.setTipoReceta(TipoReceta.POSTRE);
        else if (tipoId == R.id.chipTipoCoctel) filtroWork.setTipoReceta(TipoReceta.COCTEL);
        else if (tipoId == R.id.chipTipoSide) filtroWork.setTipoReceta(TipoReceta.SIDE);
        else filtroWork.setTipoReceta(null);

        // 3. Momento
        int momentoId = chipGroupMomento.getCheckedChipId();
        if (momentoId == R.id.chipMomentoComida) filtroWork.setMomentoReceta(MomentoReceta.COMIDA);
        else if (momentoId == R.id.chipMomentoCena) filtroWork.setMomentoReceta(MomentoReceta.CENA);
        else filtroWork.setMomentoReceta(null);

        // 4. Temporadas
        Set<Temporada> temps = new HashSet<>();
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempPrimavera)) temps.add(Temporada.PRIMAVERA);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempVerano)) temps.add(Temporada.VERANO);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempOtonio)) temps.add(Temporada.OTONIO);
        if (isChipChecked(chipGroupTemporadas, R.id.chipTempInvierno)) temps.add(Temporada.INVIERNO);
        filtroWork.setTemporadas(temps);

        // 5. Estrellas
        int estId = chipGroupEstrellas.getCheckedChipId();
        if (estId == R.id.chipEstrellas1) filtroWork.setMinEstrellas(1.0f);
        else if (estId == R.id.chipEstrellas2) filtroWork.setMinEstrellas(2.0f);
        else if (estId == R.id.chipEstrellas3) filtroWork.setMinEstrellas(3.0f);
        else if (estId == R.id.chipEstrellas4) filtroWork.setMinEstrellas(4.0f);
        else if (estId == R.id.chipEstrellas5) filtroWork.setMinEstrellas(5.0f);
        else filtroWork.setMinEstrellas(0.0f);

        // 6. Salubridad
        filtroWork.setMinPuntuacionSalud(sliderSalud.getValue());

        // 7. Alérgenos
        Set<Integer> algs = new HashSet<>();
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoGluten)) algs.add(0);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoLacteos)) algs.add(1);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoFrutosSecos)) algs.add(2);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoSoja)) algs.add(3);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoPescado)) algs.add(4);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoMariscos)) algs.add(5);
        if (isChipChecked(chipGroupAlergenos, R.id.chipAlergenoHuevos)) algs.add(6);
        filtroWork.setAlergenosExcluidos(algs);

        // 8. Tiempo
        int tiempoId = chipGroupTiempoMax.getCheckedChipId();
        if (tiempoId == R.id.chipTiempo15) filtroWork.setMaxTiempoMinutos(15);
        else if (tiempoId == R.id.chipTiempo30) filtroWork.setMaxTiempoMinutos(30);
        else if (tiempoId == R.id.chipTiempo45) filtroWork.setMaxTiempoMinutos(45);
        else if (tiempoId == R.id.chipTiempo60) filtroWork.setMaxTiempoMinutos(60);
        else filtroWork.setMaxTiempoMinutos(0);

        // 9. Solo sin puntuar
        filtroWork.setSoloIngredientesSinPuntuar(cbSoloSinPuntuar.isChecked());
    }

    private void actualizarTextoSalud(float value) {
        if (value <= 0.0f) {
            txtValSalud.setText(R.string.cualquiera);
        } else {
            txtValSalud.setText(String.format(Locale.getDefault(), "≥ %.1f", value));
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
