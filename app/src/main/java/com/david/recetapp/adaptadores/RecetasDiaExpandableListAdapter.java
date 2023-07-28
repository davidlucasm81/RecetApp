package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.actividades.CalendarioActivity;
import com.david.recetapp.negocio.beans.CalendarioBean;
import com.david.recetapp.negocio.beans.DiaRecetas;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.util.List;
import java.util.Locale;

public class RecetasDiaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Receta> listaRecetas;

    private final ExpandableListView expandableListView;

    private final int dia;
    public RecetasDiaExpandableListAdapter(Context context, List<Receta> listaRecetas, ExpandableListView expandableListView, int dia) {
        this.context = context;
        this.listaRecetas = listaRecetas;
        this.expandableListView = expandableListView;
        this.dia = dia;
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
        ImageView imageViewRecargar = convertView.findViewById(R.id.imageViewRecargar);

        imageViewRecargar.setOnClickListener(v ->{
            // Mostrar el cuadro de diálogo para preguntar al usuario
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.titulo_recargar_receta));
            builder.setMessage(context.getString(R.string.comprobar_si_recargar_receta));

            builder.setPositiveButton(context.getString(R.string.aceptar), (dialog, which) -> {
                CalendarioBean calendario = CalendarioSrv.cargarCalendario(context);
                assert calendario != null;
                DiaRecetas diaRecetas = calendario.getListaDiaRecetas().get(dia);
                Receta recetaNueva = CalendarioSrv.recargarReceta(context,diaRecetas.getFecha().getTime(),diaRecetas.getRecetas());

                if(recetaNueva == null){
                    UtilsSrv.notificacion(context, context.getString(R.string.no_recargar_receta), Toast.LENGTH_LONG);
                }
                else {
                    listaRecetas.set(groupPosition, recetaNueva);
                    diaRecetas.getRecetas().set(groupPosition,recetaNueva.getId());
                    calendario.getListaDiaRecetas().set(dia,diaRecetas);
                    CalendarioSrv.actualizarCalendario(context,calendario,true);
                    Intent intent = new Intent(context, CalendarioActivity.class);
                    context.startActivity(intent);
                }
            });

            builder.setNegativeButton(context.getString(R.string.cancelar), (dialog, which) -> {
                // No hacer nada si el usuario cancela la acción.
            });

            builder.show();
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
        Receta receta = listaRecetas.get(groupPosition);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);
        ratingBar.setVisibility(View.GONE);
        switch (childPosition) {
            case 0:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                StringBuilder sbIngredientes = new StringBuilder();
                int totalIngredientes = receta.getIngredientes().size();

                for (int i = 0; i < totalIngredientes; i++) {
                    Ingrediente ingrediente = receta.getIngredientes().get(i);
                    sbIngredientes.append("- ").append(ingrediente.getCantidad()).append(" ").append(ingrediente.getTipoCantidad()).append(context.getString(R.string.literal_de)).append(ingrediente.getNombre());

                    // Agregar dos saltos de línea si no es la última iteración
                    if (i < totalIngredientes - 1) {
                        sbIngredientes.append("\n\n");
                    } else {
                        sbIngredientes.append("\n");
                    }
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                break;
            case 1:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.pasos);
                SpannableStringBuilder sbPasos = new SpannableStringBuilder();
                int totalPasos = receta.getPasos().size();
                int minutosTotales = 0;

                for (int i = 0; i < totalPasos; i++) {
                    String tiempoReceta = receta.getPasos().get(i).getTiempo(); // HH:MM
                    String[] tiempos = tiempoReceta.split(":");
                    int horas = Integer.parseInt(tiempos[0]);
                    int minutos = Integer.parseInt(tiempos[1]);
                    minutosTotales += minutos + 60 * horas;
                }

                int horas = minutosTotales / 60;
                int minutos = minutosTotales % 60;

                // Formateamos horas y minutos con 2 dígitos
                String tiempoTotal = String.format(Locale.getDefault(),"%02d:%02d", horas, minutos);

                // Creamos un SpannableStringBuilder para el texto completo
                SpannableStringBuilder sbResaltado = new SpannableStringBuilder(context.getString(R.string.tiempo_total) + tiempoTotal);

                // Aplicamos el estilo negrita solo a la parte de tiempoTotal (HH:MM)
                int startIndex = sbResaltado.length() - tiempoTotal.length();
                int endIndex = sbResaltado.length();
                sbResaltado.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Aplicamos el subrayado a tod0 el texto
                sbResaltado.setSpan(new UnderlineSpan(), 0, sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Agregamos el texto resaltado al sbPasos
                sbPasos.append(sbResaltado);
                sbPasos.append("\n\n");


                for (int i = 0; i < totalPasos; i++) {
                    String tiempo = receta.getPasos().get(i).getTiempo();
                    String paso = receta.getPasos().get(i).getPaso();
                    String pasoFormateado = "[" + tiempo + "] " + (i + 1) + ") " + paso;

                    SpannableString spannablePaso = new SpannableString(pasoFormateado);
                    int startPos = pasoFormateado.indexOf("[");
                    int endPos = pasoFormateado.indexOf("]") + 1;
                    spannablePaso.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startPos, endPos, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                    sbPasos.append(spannablePaso);

                    // Agregar un salto de línea si no es la última iteración
                    if (i < totalPasos - 1) {
                        sbPasos.append("\n\n");
                    }
                }

                txtInformacion.setText(sbPasos);
                break;
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

}
