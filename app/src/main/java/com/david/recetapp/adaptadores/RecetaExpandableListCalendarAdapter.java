package com.david.recetapp.adaptadores;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecetaExpandableListCalendarAdapter extends BaseExpandableListAdapter {
    private final Day selectedDay;
    private final Activity activity;
    private final List<Receta> listaRecetas;
    private final ExpandableListView expandableListView;

    private final EmptyListListener emptyListListener;

    public RecetaExpandableListCalendarAdapter(Activity activity, Day selectedDay, ExpandableListView expandableListView, EmptyListListener emptyListListener) {
        this.selectedDay = selectedDay;
        this.activity = activity;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
        this.listaRecetas = RecetasSrv.getRecetasAdaptadasCalendario(RecetasSrv.cargarListaRecetas(activity), selectedDay);
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 8; // Temporadas, Ingredientes, Pasos, Alergenos, Estrellas, Fecha en el calendario y Puntuacion
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listaRecetas.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Receta receta = listaRecetas.get(groupPosition);
        return switch (childPosition) {
            case 0 -> receta.getTemporadas();
            case 1 -> receta.getNumPersonas();
            case 2 -> receta.getIngredientes();
            case 3 -> receta.getPasos();
            case 4 -> receta.getAlergenos();
            case 5 -> receta.getEstrellas();
            case 6 -> receta.getFechaCalendario();
            case 7 -> receta.getPuntuacionDada();
            default -> null;
        };
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
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_group, parent, false);
        }

        TextView txtTituloReceta = convertView.findViewById(R.id.txtNombreReceta);
        ImageButton btnEliminar = convertView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre());
        ImageView postre = convertView.findViewById(R.id.imageViewPostreIcono);
        ImageView shared = convertView.findViewById(R.id.imageViewSharedIcono);

        postre.setVisibility(View.GONE);
        shared.setVisibility(View.GONE);

        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.confirmacion)).setMessage(activity.getString(R.string.alerta_eliminar) + " '" + receta.getNombre() + "' ?").setPositiveButton(activity.getString(R.string.aceptar), (dialog, which) -> {
                // Eliminar la receta del calendario y refrescar la pantalla
                Receta eliminada = listaRecetas.remove(groupPosition);
                selectedDay.removeReceta(eliminada.getId());
                CalendarioSrv.actualizarDia(activity, selectedDay);
                CalendarioSrv.actualizarFechaCalendario(activity, eliminada.getId());
                if (listaRecetas.size() < 2) {
                    emptyListListener.onListSize();
                }
                if (listaRecetas.isEmpty()) {
                    emptyListListener.onListEmpty();
                }
                notifyDataSetChanged();
            }).setNegativeButton(activity.getString(R.string.cancelar), null).show();
        });

        ImageButton btnEditar = convertView.findViewById(R.id.btnEditar);
        btnEditar.setVisibility(View.GONE);

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
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                List<String> temporadas = receta.getTemporadas().stream().map(T -> T.getNombre(this.activity)).collect(Collectors.toList());
                txtInformacion.setText(TextUtils.join(", ", temporadas));
                break;
            case 1:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.numero_personas);
                txtInformacion.setText(String.valueOf(receta.getNumPersonas()));
                break;
            case 2:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                StringBuilder sbIngredientes = new StringBuilder();
                int totalIngredientes = receta.getIngredientes().size();

                for (int i = 0; i < totalIngredientes; i++) {
                    Ingrediente ingrediente = receta.getIngredientes().get(i);
                    sbIngredientes.append("- ").append(ingrediente.getCantidad()).append(" ").append(ingrediente.getTipoCantidad()).append(activity.getString(R.string.literal_de)).append(ingrediente.getNombre()).append(ingrediente.getPuntuacion()>=0? " (Score: "+ingrediente.getPuntuacion()+")" : "");

                    // Agregar dos saltos de línea si no es la última iteración
                    if (i < totalIngredientes - 1) {
                        sbIngredientes.append("\n\n");
                    } else {
                        sbIngredientes.append("\n");
                    }
                }
                txtInformacion.setText(sbIngredientes.substring(0, sbIngredientes.length() - 1));
                break;
            case 3:
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
                SpannableStringBuilder sbResaltado = new SpannableStringBuilder(activity.getString(R.string.tiempo_total) + tiempoTotal);

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
            case 4:
                txtTitulo.setText(R.string.alergenos);
                txtInformacion.setVisibility(View.GONE);
                iconosAlergenos.setVisibility(View.VISIBLE);
                iconosAlergenos.removeAllViews(); // Elimina todas las vistas hijos del LinearLayout
                // Recorremos la lista de drawables
                for (Alergeno alergeno : receta.getAlergenos()) {
                    // Creamos un nuevo ImageView
                    ImageView imageView = new ImageView(activity);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    // Asignamos el drawable al ImageView
                    imageView.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));

                    // Agregamos el ImageView al LinearLayout
                    iconosAlergenos.addView(imageView);
                }
                if (receta.getAlergenos().isEmpty()) {
                    TextView textView = new TextView(activity);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setText(R.string.sin_al_rgenos);
                    iconosAlergenos.addView(textView);
                }
                break;

            case 5:
                txtTitulo.setText(R.string.estrellas);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(receta.getEstrellas());
                txtInformacion.setVisibility(View.GONE);
                break;
            case 6:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ultima_fecha_calendario);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                txtInformacion.setText(dateFormat.format(receta.getFechaCalendario()));
                break;
            case 7:
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

        void onListSize();
    }
}
