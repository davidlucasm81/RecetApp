package com.david.recetapp.adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Receta;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecetasAdapter extends RecyclerView.Adapter<RecetasAdapter.RecetaViewHolder> {
    private List<Receta> recetas;
    private final OnRecetaClickListener listener;
    private Date fechaElegida;
    private Date fechaIntervaloPrevio;

    public interface OnRecetaClickListener {
        void onRecetaClick(Receta receta);
    }

    public RecetasAdapter(List<Receta> recetas, OnRecetaClickListener listener) {
        this.recetas = new ArrayList<>(recetas);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receta_button, parent, false);
        return new RecetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaViewHolder holder, int position) {
        holder.bind(recetas.get(position));
    }

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    public void updateRecetas(List<Receta> newRecetas, Date fechaElegida, Date fechaIntervaloPrevio) {
        final List<Receta> oldList = new ArrayList<>(this.recetas);
        this.fechaElegida = fechaElegida;
        this.fechaIntervaloPrevio = fechaIntervaloPrevio;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newRecetas.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getId().equals(
                        newRecetas.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Receta oldReceta = oldList.get(oldItemPosition);
                Receta newReceta = newRecetas.get(newItemPosition);
                return oldReceta.getNombre().equals(newReceta.getNombre()) &&
                       oldReceta.isPostre() == newReceta.isPostre();
            }
        });

        this.recetas = new ArrayList<>(newRecetas);
        diffResult.dispatchUpdatesTo(this);
    }

    class RecetaViewHolder extends RecyclerView.ViewHolder {
        private final Button button;

        RecetaViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView;
            button.setClickable(true);
            button.setFocusable(true);
        }

        void bind(final Receta receta) {
            button.setText(receta.getNombre());
            Context ctx = button.getContext();

            if (receta.isPostre()) {
                button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.postre_background));
            } else {
                Date f = receta.getFechaCalendario();
                if (f != null && fechaIntervaloPrevio != null && fechaElegida != null
                        && f.after(fechaIntervaloPrevio) && f.before(fechaElegida)) {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.previous_selected_background));
                } else {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.edittext_background));
                }
            }

            button.setOnClickListener(v -> {
                if (listener != null) listener.onRecetaClick(receta);
            });
        }

    }
}
