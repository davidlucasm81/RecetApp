package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Paso;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class RecetaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Receta> listaRecetas;

    private ExpandableListView expandableListView;

    public RecetaExpandableListAdapter(Context context, List<Receta> listaRecetas, ExpandableListView expandableListView) {
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
        return 3; // Hay 3 elementos hijos: Temporadas, Ingredientes y Pasos
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
                return receta.getTemporadas();
            case 1:
                return receta.getIngredientes();
            case 2:
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
            convertView = inflater.inflate(R.layout.list_item_group, null);
        }

        TextView txtTituloReceta = convertView.findViewById(R.id.txtNombreReceta);
        Button btnEliminar = convertView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre());

        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirmación").setMessage("¿Está seguro de que desea eliminar la receta?").setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Eliminar la receta del JSON y refrescar la pantalla
                        eliminarReceta(groupPosition);
                        notifyDataSetChanged();
                    }
                }).setNegativeButton("Cancelar", null).show();
            }
        });

        txtTituloReceta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_child, null);
        }

        TextView txtTitulo = convertView.findViewById(R.id.txtTitulo);
        TextView txtInformacion = convertView.findViewById(R.id.txtInformacion);
        TextView txtNumero = convertView.findViewById(R.id.txtNumero);
        Receta receta = listaRecetas.get(groupPosition);
        switch (childPosition) {
            case 0:
                txtTitulo.setText("Temporadas");
                txtInformacion.setText(TextUtils.join(", ", receta.getTemporadas()));
                break;
            case 1:
                txtTitulo.setText("Ingredientes");
                StringBuilder sbIngredientes = new StringBuilder();
                StringBuilder sbNumeroIngrediente = new StringBuilder();
                for (Ingrediente ingrediente : receta.getIngredientes()) {
                    sbIngredientes.append("- ").append(ingrediente.getNombre()).append("\n");
                    sbNumeroIngrediente.append(ingrediente.getCantidad()).append("\n");
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                txtNumero.setText(sbNumeroIngrediente.substring(0, sbNumeroIngrediente.length() - 1));
                break;
            case 2:
                txtTitulo.setText("Pasos");
                StringBuilder sbPasos = new StringBuilder();
                StringBuilder sbNumeroPasos = new StringBuilder();
                for (int i =0; i<receta.getPasos().size();i++) {
                    sbPasos.append(i + 1 +") ").append(receta.getPasos().get(i).getPaso()).append("\n");
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

    private void eliminarReceta(int position) {
        // Eliminar la receta de la lista de recetas y actualizar el archivo JSON
        listaRecetas.remove(position);
        guardarListaRecetas();
    }

    private void guardarListaRecetas() {
        // Guardar la lista de recetas en el archivo JSON
        Gson gson = new Gson();
        String json = gson.toJson(listaRecetas);

        try {
            FileOutputStream fos = context.openFileOutput("lista_recetas.json", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}