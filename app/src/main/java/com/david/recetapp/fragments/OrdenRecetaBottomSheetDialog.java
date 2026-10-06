package com.david.recetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.FiltroRecetas;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class OrdenRecetaBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_CRITERIO = "arg_criterio";

    public interface OnOrdenSeleccionadoListener {
        void onOrdenSeleccionado(FiltroRecetas.CriterioOrden criterio);
    }

    private OnOrdenSeleccionadoListener listener;
    private FiltroRecetas.CriterioOrden criterioActual;

    private TextView txtEstadoNombre, txtEstadoEstrellas, txtEstadoSalud, txtEstadoTiempo, txtEstadoFecha;

    public static OrdenRecetaBottomSheetDialog newInstance(FiltroRecetas.CriterioOrden criterioActual) {
        OrdenRecetaBottomSheetDialog dialog = new OrdenRecetaBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRITERIO, criterioActual);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnOrdenSeleccionadoListener(OnOrdenSeleccionadoListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_orden_recetas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            criterioActual = getArguments().getSerializable(ARG_CRITERIO, FiltroRecetas.CriterioOrden.class);
        }
        if (criterioActual == null) {
            criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC;
        }

        txtEstadoNombre = view.findViewById(R.id.txtEstadoNombre);
        txtEstadoEstrellas = view.findViewById(R.id.txtEstadoEstrellas);
        txtEstadoSalud = view.findViewById(R.id.txtEstadoSalud);
        txtEstadoTiempo = view.findViewById(R.id.txtEstadoTiempo);
        txtEstadoFecha = view.findViewById(R.id.txtEstadoFecha);

        actualizarEstadosUI();

        view.findViewById(R.id.cardRowNombre).setOnClickListener(v -> {
            if (criterioActual == FiltroRecetas.CriterioOrden.NOMBRE_ASC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_DESC;
            } else if (criterioActual == FiltroRecetas.CriterioOrden.NOMBRE_DESC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC; // Off / default
            } else {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC;
            }
            aplicarYSalir();
        });

        view.findViewById(R.id.cardRowEstrellas).setOnClickListener(v -> {
            if (criterioActual == FiltroRecetas.CriterioOrden.ESTRELLAS_DESC) {
                criterioActual = FiltroRecetas.CriterioOrden.ESTRELLAS_ASC;
            } else if (criterioActual == FiltroRecetas.CriterioOrden.ESTRELLAS_ASC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC; // Off
            } else {
                criterioActual = FiltroRecetas.CriterioOrden.ESTRELLAS_DESC;
            }
            aplicarYSalir();
        });

        view.findViewById(R.id.cardRowSalud).setOnClickListener(v -> {
            if (criterioActual == FiltroRecetas.CriterioOrden.SALUD_DESC) {
                criterioActual = FiltroRecetas.CriterioOrden.SALUD_ASC;
            } else if (criterioActual == FiltroRecetas.CriterioOrden.SALUD_ASC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC; // Off
            } else {
                criterioActual = FiltroRecetas.CriterioOrden.SALUD_DESC;
            }
            aplicarYSalir();
        });

        view.findViewById(R.id.cardRowTiempo).setOnClickListener(v -> {
            if (criterioActual == FiltroRecetas.CriterioOrden.TIEMPO_ASC) {
                criterioActual = FiltroRecetas.CriterioOrden.TIEMPO_DESC;
            } else if (criterioActual == FiltroRecetas.CriterioOrden.TIEMPO_DESC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC; // Off
            } else {
                criterioActual = FiltroRecetas.CriterioOrden.TIEMPO_ASC;
            }
            aplicarYSalir();
        });

        view.findViewById(R.id.cardRowFecha).setOnClickListener(v -> {
            if (criterioActual == FiltroRecetas.CriterioOrden.FECHA_DESC) {
                criterioActual = FiltroRecetas.CriterioOrden.FECHA_ASC;
            } else if (criterioActual == FiltroRecetas.CriterioOrden.FECHA_ASC) {
                criterioActual = FiltroRecetas.CriterioOrden.NOMBRE_ASC; // Off
            } else {
                criterioActual = FiltroRecetas.CriterioOrden.FECHA_DESC;
            }
            aplicarYSalir();
        });
    }

    private void actualizarEstadosUI() {
        txtEstadoNombre.setText(criterioActual == FiltroRecetas.CriterioOrden.NOMBRE_ASC ? "A-Z" :
                (criterioActual == FiltroRecetas.CriterioOrden.NOMBRE_DESC ? "Z-A" : "—"));

        txtEstadoEstrellas.setText(criterioActual == FiltroRecetas.CriterioOrden.ESTRELLAS_DESC ? "Mayor a menor" :
                (criterioActual == FiltroRecetas.CriterioOrden.ESTRELLAS_ASC ? "Menor a mayor" : "—"));

        txtEstadoSalud.setText(criterioActual == FiltroRecetas.CriterioOrden.SALUD_DESC ? "Mayor a menor" :
                (criterioActual == FiltroRecetas.CriterioOrden.SALUD_ASC ? "Menor a mayor" : "—"));

        txtEstadoTiempo.setText(criterioActual == FiltroRecetas.CriterioOrden.TIEMPO_ASC ? "Más rápidas" :
                (criterioActual == FiltroRecetas.CriterioOrden.TIEMPO_DESC ? "Más lentas" : "—"));

        txtEstadoFecha.setText(criterioActual == FiltroRecetas.CriterioOrden.FECHA_DESC ? "Más recientes" :
                (criterioActual == FiltroRecetas.CriterioOrden.FECHA_ASC ? "Más antiguas" : "—"));
    }

    private void aplicarYSalir() {
        if (listener != null) {
            listener.onOrdenSeleccionado(criterioActual);
        }
        dismiss();
    }
}
