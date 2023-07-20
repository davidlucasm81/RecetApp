package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import com.david.recetapp.R;
import com.david.recetapp.actividades.EditarRecetaActivity;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class RecetaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Receta> listaRecetas;

    private final ExpandableListView expandableListView;

    private final EmptyListListener emptyListListener;


    public RecetaExpandableListAdapter(Context context, List<Receta> listaRecetas, ExpandableListView expandableListView, EmptyListListener emptyListListener) {
        this.context = context;
        this.listaRecetas = listaRecetas;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 5; // Hay 4 elementos hijos: Temporadas, Ingredientes, Pasos, Estrellas, y Fecha en el calendario
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
            case 3:
                return receta.getEstrellas();
            case 4:
                return receta.getFechaCalendario();
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
            convertView = inflater.inflate(R.layout.list_item_group, parent, false);
        }

        TextView txtTituloReceta = convertView.findViewById(R.id.txtNombreReceta);
        ImageButton btnEliminar = convertView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre());

        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.confirmacion)).setMessage(context.getString(R.string.alerta_eliminar)).setPositiveButton(context.getString(R.string.aceptar), (dialog, which) -> {
                // Eliminar la receta del JSON y refrescar la pantalla
                eliminarReceta(groupPosition);
                notifyDataSetChanged();
            }).setNegativeButton(context.getString(R.string.cancelar), null).show();
        });

        ImageButton btnEditar = convertView.findViewById(R.id.btnEditar);
        btnEditar.setOnClickListener(v -> {
            // Muestra actividad de editar receta
            editarReceta(groupPosition);
            notifyDataSetChanged();
        });

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

    private void editarReceta(int position) {
        // Crea un Intent para abrir la actividad EditarRecetaActivity
        Intent intent = new Intent(context, EditarRecetaActivity.class);
        // Pasa parametros
        intent.putExtra("listaRecetas", (Serializable) listaRecetas);
        intent.putExtra("position", position);
        // Inicia la actividad EditarRecetaActivity
        context.startActivity(intent);
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
                txtNumero.setVisibility(View.GONE);
                txtTitulo.setText(R.string.temporadas);
                List<String> temporadas = receta.getTemporadas().stream().map(T -> T.getNombre(this.context)).collect(Collectors.toList());
                txtInformacion.setText(TextUtils.join(", ", temporadas));
                break;
            case 1:
                txtInformacion.setVisibility(View.VISIBLE);
                txtNumero.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                StringBuilder sbIngredientes = new StringBuilder();
                StringBuilder sbNumeroIngrediente = new StringBuilder();
                for (Ingrediente ingrediente : receta.getIngredientes()) {
                    sbIngredientes.append("- ").append(ingrediente.getNombre()).append("\n");
                    sbNumeroIngrediente.append(ingrediente.getCantidad()).append(" ").append(ingrediente.getTipoCantidad()).append("\n");
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                txtNumero.setText(sbNumeroIngrediente.substring(0, sbNumeroIngrediente.length() - 1));
                break;
            case 2:
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
            case 3:
                txtTitulo.setText(R.string.estrellas);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(receta.getEstrellas());
                txtInformacion.setVisibility(View.GONE);
                txtNumero.setVisibility(View.GONE);
                break;
            case 4:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ultima_fecha_calendario);
                txtInformacion.setText(receta.getFechaCalendario().toString());
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
        if (listaRecetas.isEmpty()) {
            emptyListListener.onListEmpty();
        }
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

    public interface EmptyListListener {
        void onListEmpty();
    }
}
