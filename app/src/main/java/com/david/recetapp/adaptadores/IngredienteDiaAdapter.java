package com.david.recetapp.adaptadores;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.IngredienteDia;
import com.david.recetapp.negocio.servicios.CalendarioSrv;

import java.text.MessageFormat;
import java.util.List;

public class IngredienteDiaAdapter extends RecyclerView.Adapter<IngredienteDiaAdapter.IngredienteDiaViewHolder> {

    private final List<IngredienteDia> ingredientesDias;
    private final CalendarioBean calendarioBean;

    public IngredienteDiaAdapter(List<IngredienteDia> ingredientesDias, CalendarioBean calendarioBean) {
        this.ingredientesDias = ingredientesDias;
        this.calendarioBean = calendarioBean;
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
        final IngredienteDia ingredienteDia = ingredientesDias.get(position);
        holder.checkBoxIngrediente.setChecked(ingredienteDia.isComprado());
        holder.textViewNombreIngrediente.setText(MessageFormat.format("{0} {1} de {2}", ingredienteDia.getIngrediente().getCantidad(), ingredienteDia.getIngrediente().getTipoCantidad(), ingredienteDia.getIngrediente().getNombre()));

        if (ingredienteDia.isComprado()) {
            holder.textViewNombreIngrediente.setPaintFlags(holder.textViewNombreIngrediente.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.textViewNombreIngrediente.setPaintFlags(holder.textViewNombreIngrediente.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.checkBoxIngrediente.setOnClickListener(view -> {
            ingredienteDia.setComprado(holder.checkBoxIngrediente.isChecked());
            CalendarioSrv.actualizarCalendario(view.getContext(), calendarioBean, false);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return ingredientesDias.size();
    }

    public static class IngredienteDiaViewHolder extends RecyclerView.ViewHolder {
        public final CheckBox checkBoxIngrediente;
        public final TextView textViewNombreIngrediente;

        public IngredienteDiaViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxIngrediente = itemView.findViewById(R.id.checkbox_ingrediente);
            textViewNombreIngrediente = itemView.findViewById(R.id.textview_nombre_ingrediente);
        }
    }
}
