package com.david.recetapp.adaptadores;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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
import java.util.Map;

public class ListaCompraPorDiaIngredientesAdapter extends RecyclerView.Adapter<ListaCompraPorDiaIngredientesAdapter.IngredienteDiaViewHolder> {

    private final Map<String, List<Ingrediente>> ingredientes;

    public ListaCompraPorDiaIngredientesAdapter(Map<String, List<Ingrediente>> ingredientes) {
        this.ingredientes = ingredientes;
    }

    @NonNull
    @Override
    public IngredienteDiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compra, parent, false);
        return new IngredienteDiaViewHolder(itemView);
    }

    private CharSequence formatText(Map.Entry<String, List<Ingrediente>> entry) {
        String clave = entry.getKey();
        List<Ingrediente> ingredientesLista = entry.getValue();

        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Agregar la clave en negrita
        int start = builder.length();
        builder.append(clave).append("\n");
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Agregar los ingredientes de la lista en texto normal
        for (Ingrediente ingredienteDia : ingredientesLista) {
            builder.append("\n").append(MessageFormat.format("- {0} {1} de {2}", ingredienteDia.getCantidad(), ingredienteDia.getTipoCantidad(), ingredienteDia.getNombre()));
        }
        builder.append("\n");
        return builder;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull final IngredienteDiaViewHolder holder, int position) {
        // Obtener la entrada (clave y lista de ingredientes) del mapa
        Map.Entry<String, List<Ingrediente>> entry = ingredientes.entrySet().stream().skip(position).findFirst().orElse(null);
        if (entry != null) {
            // Formatear el texto y establecerlo en el TextView
            CharSequence formattedText = formatText(entry);
            holder.textViewNombreIngrediente.setText(formattedText);

        }
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