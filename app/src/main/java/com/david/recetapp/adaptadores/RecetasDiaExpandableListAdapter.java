package com.david.recetapp.adaptadores;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;

import java.util.List;
import java.util.stream.Collectors;

public class RecetasDiaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Receta> listaRecetas;

    private final ExpandableListView expandableListView;


    public RecetasDiaExpandableListAdapter(Context context, List<Receta> listaRecetas, ExpandableListView expandableListView) {
        this.context = context;
        this.listaRecetas = listaRecetas;
        this.expandableListView = expandableListView;
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 2; // Hay 4 elementos hijos: Ingredientes, Pasos
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listaRecetas.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Receta receta = listaRecetas.get(groupPosition);
        switch (childPosition) {
            case 0:
                return receta.getIngredientes();
            case 1:
                return receta.getPasos();
            default:
                return null;
        }
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_group_calendario, parent, false);
        }

        TextView txtTituloReceta = convertView.findViewById(R.id.txtNombreReceta);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre());

        txtTituloReceta.setOnClickListener(v -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                // Cerrar todos los grupos abiertos previamente
                int groupCount = getGroupCount();
                for (int i = 0; i < groupCount; i++) {
                    if (i != groupPosition && expandableListView.isGroupExpanded(i)) {
                        expandableListView.collapseGroup(i);
                    }
                }
                expandableListView.expandGroup(groupPosition);
            }
        });
        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_child, parent, false);
        }

        TextView txtTitulo = convertView.findViewById(R.id.txtTitulo);
        TextView txtInformacion = convertView.findViewById(R.id.txtInformacion);
        TextView txtNumero = convertView.findViewById(R.id.txtNumero);
        Receta receta = listaRecetas.get(groupPosition);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);
        ratingBar.setVisibility(View.GONE);
        switch (childPosition) {
            case 0:
                txtInformacion.setVisibility(View.VISIBLE);
                txtNumero.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                StringBuilder sbIngredientes = new StringBuilder();
                StringBuilder sbNumeroIngrediente = new StringBuilder();
                for (Ingrediente ingrediente : receta.getIngredientes()) {
                    sbIngredientes.append("- ").append(ingrediente.getNombre()).append("\n");
                    sbNumeroIngrediente.append(ingrediente.getCantidad()).append("\n");
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                txtNumero.setText(sbNumeroIngrediente.substring(0, sbNumeroIngrediente.length() - 1));
                break;
            case 1:
                txtInformacion.setVisibility(View.VISIBLE);
                txtNumero.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.pasos);
                StringBuilder sbPasos = new StringBuilder();
                StringBuilder sbNumeroPasos = new StringBuilder();
                for (int i = 0; i < receta.getPasos().size(); i++) {
                    sbPasos.append(i + 1).append(") ").append(receta.getPasos().get(i).getPaso()).append("\n");
                    sbNumeroPasos.append(receta.getPasos().get(i).getTiempo()).append("\n");
                }
                txtInformacion.setText(sbPasos.substring(0, sbPasos.length() - 1));
                txtNumero.setText(sbNumeroPasos.substring(0, sbNumeroPasos.length() - 1));
                break;
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

}
