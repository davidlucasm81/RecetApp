package com.david.recetapp.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.ComentariosAdapter;
import com.david.recetapp.negocio.beans.Comentario;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;

public class ComentariosBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_RECETA = "arg_receta";

    private Receta receta;
    private ComentariosAdapter adapter;
    private ListenerRegistration listenerRegistration;

    private TextView txtNombreReceta;
    private RecyclerView rvComentarios;
    private TextView txtSinComentarios;
    private ProgressBar pbComentarios;
    private EditText etNuevoComentario;
    private ImageButton btnEnviar;

    public static ComentariosBottomSheetDialog newInstance(Receta receta) {
        ComentariosBottomSheetDialog fragment = new ComentariosBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECETA, receta);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            receta = getArguments().getSerializable(ARG_RECETA, Receta.class);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_comentarios_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtNombreReceta = view.findViewById(R.id.txtNombreRecetaComentarios);
        ImageButton btnClose = view.findViewById(R.id.btnCloseComentarios);
        rvComentarios = view.findViewById(R.id.rvComentarios);
        txtSinComentarios = view.findViewById(R.id.txtSinComentarios);
        pbComentarios = view.findViewById(R.id.pbComentarios);
        etNuevoComentario = view.findViewById(R.id.etNuevoComentario);
        btnEnviar = view.findViewById(R.id.btnEnviarComentario);

        if (receta != null) {
            txtNombreReceta.setText(receta.getNombre());
        }

        btnClose.setOnClickListener(v -> dismiss());

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvComentarios.setLayoutManager(layoutManager);

        adapter = new ComentariosAdapter(requireContext());
        adapter.setOnComentarioLongClickListener(this::mostrarOpcionesComentario);
        rvComentarios.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviarComentario());

        cargarYEscucharComentarios();
    }

    private void cargarYEscucharComentarios() {
        if (receta == null || receta.getId() == null || receta.getId().isEmpty()) {
            txtSinComentarios.setVisibility(View.VISIBLE);
            return;
        }

        pbComentarios.setVisibility(View.VISIBLE);

        listenerRegistration = RecetasSrv.escucharComentarios(receta.getId(), new RecetasSrv.ComentariosCallback() {
            @Override
            public void onSuccess(List<Comentario> comentarios) {
                if (!isAdded()) return;
                pbComentarios.setVisibility(View.GONE);

                if (comentarios == null || comentarios.isEmpty()) {
                    txtSinComentarios.setVisibility(View.VISIBLE);
                    adapter.setComentarios(comentarios);
                } else {
                    txtSinComentarios.setVisibility(View.GONE);
                    adapter.setComentarios(comentarios);
                    rvComentarios.post(() -> {
                        if (isAdded() && adapter.getItemCount() > 0) {
                            rvComentarios.scrollToPosition(adapter.getItemCount() - 1);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                pbComentarios.setVisibility(View.GONE);
                Toast.makeText(requireContext(), getString(R.string.sin_comentarios), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enviarComentario() {
        String texto = etNuevoComentario.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) {
            Toast.makeText(requireContext(), getString(R.string.error_comentario_vacio), Toast.LENGTH_SHORT).show();
            return;
        }

        if (receta == null || receta.getId() == null) return;

        Comentario comentario = new Comentario();
        comentario.setTexto(texto);
        comentario.setFecha(new Date());

        btnEnviar.setEnabled(false);
        RecetasSrv.agregarComentario(receta.getId(), comentario, new RecetasSrv.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                btnEnviar.setEnabled(true);
                etNuevoComentario.setText("");
                UtilsSrv.notificacion(requireContext(), getString(R.string.comentario_anadido), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                btnEnviar.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.error_comentario_vacio), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarOpcionesComentario(Comentario comentario) {
        String[] opciones = {getString(R.string.editar_comentario), getString(R.string.eliminar_comentario)};

        new AlertDialog.Builder(requireContext())
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        mostrarDialogoEditarComentario(comentario);
                    } else if (which == 1) {
                        mostrarDialogoEliminarComentario(comentario);
                    }
                })
                .show();
    }

    private void mostrarDialogoEditarComentario(Comentario comentario) {
        final EditText etEditar = new EditText(requireContext());
        etEditar.setText(comentario.getTexto());
        etEditar.setSelection(comentario.getTexto() != null ? comentario.getTexto().length() : 0);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etEditar.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.editar_comentario)
                .setView(etEditar)
                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    String nuevoTexto = etEditar.getText().toString().trim();
                    if (TextUtils.isEmpty(nuevoTexto)) {
                        Toast.makeText(requireContext(), getString(R.string.error_comentario_vacio), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    comentario.setTexto(nuevoTexto);
                    RecetasSrv.actualizarComentario(receta.getId(), comentario, new RecetasSrv.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                UtilsSrv.notificacion(requireContext(), getString(R.string.comentario_actualizado), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), getString(R.string.error_comentario_vacio), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void mostrarDialogoEliminarComentario(Comentario comentario) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.eliminar_comentario)
                .setMessage(R.string.confirmar_eliminar_comentario)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    RecetasSrv.eliminarComentario(receta.getId(), comentario.getId(), new RecetasSrv.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                UtilsSrv.notificacion(requireContext(), getString(R.string.comentario_eliminado), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), getString(R.string.error_comentario_vacio), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
