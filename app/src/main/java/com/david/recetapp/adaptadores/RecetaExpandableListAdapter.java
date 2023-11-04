package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.david.recetapp.R;
import com.david.recetapp.actividades.EditarRecetaActivity;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecetaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Receta> listaRecetas;

    private final ExpandableListView expandableListView;

    private final EmptyListListener emptyListListener;

    private final Map<String, Integer> ingredientMap;

    public RecetaExpandableListAdapter(Context context, List<Receta> listaRecetas, ExpandableListView expandableListView, EmptyListListener emptyListListener) {
        this.context = context;
        this.listaRecetas = listaRecetas;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
        String[] ingredientList = context.getResources().getStringArray(R.array.ingredient_list);

        ingredientMap = new HashMap<>();

        for (String s : ingredientList) {
            // Utilizar una expresión regular para encontrar el número al final
            String regex = "(.+) (\\d+)$";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(s.trim());
            if (matcher.find()) {
                // Agregar el nombre y la puntuación al mapa
                ingredientMap.put(matcher.group(1), Integer.parseInt(Objects.requireNonNull(matcher.group(2))));
            }
        }
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 7; // Hay 4 elementos hijos: Temporadas, Ingredientes, Pasos, Alergenos, Estrellas, Fecha en el calendario y Puntuacion
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
                return receta.getAlergenos();
            case 4:
                return receta.getEstrellas();
            case 5:
                return receta.getFechaCalendario();
            case 6:
                return receta.getPuntuacionDada();
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
        ImageView postre = convertView.findViewById(R.id.imageViewPostreIcono);
        ImageView shared = convertView.findViewById(R.id.imageViewSharedIcono);
        if (receta.isPostre()) {
            postre.setVisibility(View.VISIBLE);
        } else {
            postre.setVisibility(View.GONE);
        }
        if (receta.isShared()) {
            shared.setVisibility(View.VISIBLE);
        } else {
            shared.setVisibility(View.GONE);
        }
        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.confirmacion)).setMessage(context.getString(R.string.alerta_eliminar) + " '" + receta.getNombre() + "' ?").setPositiveButton(context.getString(R.string.aceptar), (dialog, which) -> {
                // Eliminar la receta del JSON y refrescar la pantalla
                RecetasSrv.eliminarReceta(context, groupPosition, listaRecetas);
                if (listaRecetas.isEmpty()) {
                    emptyListListener.onListEmpty();
                }
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
        Receta receta = listaRecetas.get(groupPosition);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);
        ratingBar.setVisibility(View.GONE);
        LinearLayout iconosAlergenos = convertView.findViewById(R.id.iconosAlergenos);
        iconosAlergenos.setVisibility(View.GONE);
        switch (childPosition) {
            case 0:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.temporadas);
                List<String> temporadas = receta.getTemporadas().stream().map(T -> T.getNombre(this.context)).collect(Collectors.toList());
                txtInformacion.setText(TextUtils.join(", ", temporadas));
                break;
            case 1:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                StringBuilder sbIngredientes = new StringBuilder();
                int totalIngredientes = receta.getIngredientes().size();

                for (int i = 0; i < totalIngredientes; i++) {
                    Ingrediente ingrediente = receta.getIngredientes().get(i);
                    float puntuacion = UtilsSrv.obtenerPuntuacion(ingredientMap,ingrediente.getNombre(), -1);
                    sbIngredientes.append("- ").append(ingrediente.getCantidad()).append(" ").append(ingrediente.getTipoCantidad()).append(context.getString(R.string.literal_de)).append(ingrediente.getNombre()).append(" (Score: ").append(puntuacion).append(")");

                    // Agregar dos saltos de línea si no es la última iteración
                    if (i < totalIngredientes - 1) {
                        sbIngredientes.append("\n\n");
                    } else {
                        sbIngredientes.append("\n");
                    }
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                break;
            case 2:
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
                String tiempoTotal = String.format(Locale.getDefault(), "%02d:%02d", horas, minutos);

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
            case 3:
                txtTitulo.setText(R.string.alergenos);
                txtInformacion.setVisibility(View.GONE);
                iconosAlergenos.setVisibility(View.VISIBLE);
                iconosAlergenos.removeAllViews(); // Elimina todas las vistas hijos del LinearLayout
                // Recorremos la lista de drawables
                for (Alergeno alergeno : receta.getAlergenos()) {
                    // Creamos un nuevo ImageView
                    ImageView imageView = new ImageView(context);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    // Asignamos el drawable al ImageView
                    imageView.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));

                    // Agregamos el ImageView al LinearLayout
                    iconosAlergenos.addView(imageView);
                }
                if (receta.getAlergenos().isEmpty()) {
                    TextView textView = new TextView(context);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setText(R.string.sin_al_rgenos);
                    iconosAlergenos.addView(textView);
                }
                break;

            case 4:
                txtTitulo.setText(R.string.estrellas);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(receta.getEstrellas());
                txtInformacion.setVisibility(View.GONE);
                break;
            case 5:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ultima_fecha_calendario);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                txtInformacion.setText(dateFormat.format(receta.getFechaCalendario()));
                break;
            case 6:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.puntuacion_dada);
                txtInformacion.setText(String.valueOf(receta.getPuntuacionDada()));
                break;
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


    public interface EmptyListListener {
        void onListEmpty();
    }
}
