package com.david.recetapp.adaptadores;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;

import java.text.MessageFormat;
import java.util.List;

public class ListaCompraTodosIngredientesAdapter extends RecyclerView.Adapter<ListaCompraTodosIngredientesAdapter.IngredienteDiaViewHolder> {

    private final List<Ingrediente> ingredientes;

    public ListaCompraTodosIngredientesAdapter(List<Ingrediente> ingredientes) {
        this.ingredientes = ingredientes;
    }

    @NonNull
    @Override
    public IngredienteDiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compra, parent, false);
        return new IngredienteDiaViewHolder(itemView);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull final IngredienteDiaViewHolder holder, int position) {
        final Ingrediente ingredienteDia = ingredientes.get(position);
        holder.textViewNombreIngrediente.setText(MessageFormat.format("- {0} {1} de {2}", ingredienteDia.getCantidad(), ingredienteDia.getTipoCantidad(), ingredienteDia.getNombre()));
    }

    @Override
    public int getItemCount() {
        return ingredientes.size();
    }

    public static class IngredienteDiaViewHolder extends RecyclerView.ViewHolder {
        public final TextView textViewNombreIngrediente;

        public IngredienteDiaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombreIngrediente = itemView.findViewById(R.id.textview_nombre_ingrediente);
        }
    }
}
