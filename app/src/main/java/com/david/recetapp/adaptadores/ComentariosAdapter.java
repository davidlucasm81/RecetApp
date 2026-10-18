package com.david.recetapp.adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Comentario;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComentariosAdapter extends RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder> {

    public interface OnComentarioLongClickListener {
        void onLongClick(Comentario comentario);
    }

    private final Context context;
    private final List<Comentario> listaComentarios = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private OnComentarioLongClickListener longClickListener;

    public ComentariosAdapter(Context context) {
        this.context = context;
    }

    public void setOnComentarioLongClickListener(OnComentarioLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setComentarios(List<Comentario> nuevosComentarios) {
        this.listaComentarios.clear();
        if (nuevosComentarios != null) {
            this.listaComentarios.addAll(nuevosComentarios);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new ComentarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        Comentario comentario = listaComentarios.get(position);

        holder.txtTexto.setText(comentario.getTexto() != null ? comentario.getTexto() : "");

        StringBuilder fechaStr = new StringBuilder();
        if (comentario.getFecha() != null) {
            fechaStr.append(dateFormat.format(comentario.getFecha()));
        }

        if (comentario.getFechaEdicion() != null) {
            fechaStr.append(" ").append(context.getString(R.string.comentario_editado_tag));
        }

        holder.txtFecha.setText(fechaStr.toString());

        holder.cardComentario.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(comentario);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    static class ComentarioViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardComentario;
        TextView txtTexto;
        TextView txtFecha;

        public ComentarioViewHolder(@NonNull View itemView) {
            super(itemView);
            cardComentario = itemView.findViewById(R.id.cardComentario);
            txtTexto = itemView.findViewById(R.id.txtTextoComentario);
            txtFecha = itemView.findViewById(R.id.txtFechaComentario);
        }
    }
}
