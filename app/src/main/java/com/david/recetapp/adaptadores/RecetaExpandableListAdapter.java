package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

import com.david.recetapp.R;
import com.david.recetapp.actividades.recetas.EditarRecetaActivity;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Adapter optimizado: mantiene lista interna mutable y permite updateData(...) en el main thread.
 */
public class RecetaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final ExpandableListView expandableListView;
    private final EmptyListListener emptyListListener;
    private final Handler mainHandler;

    // Lista interna mutable (hacemos copia defensiva en constructor)
    private List<Receta> listaRecetas;

    public RecetaExpandableListAdapter(Context context, List<Receta> listaRecetas,
                                       ExpandableListView expandableListView, EmptyListListener emptyListListener) {
        this.context = context;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        // Copia defensiva para poder modificar la lista internamente
        this.listaRecetas = (listaRecetas != null) ? new ArrayList<>(listaRecetas) : new ArrayList<>();
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // Mantengo la estructura fija: 8 children tal como estaba (temporadas, personas, ingredientes, pasos, alérgenos, estrellas, fecha, puntuación)
        return 8;
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
        return ((long) groupPosition << 32) | (childPosition & 0xffffffffL);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // --------------------------
    // VIEWS
    // --------------------------

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_group, parent, false);
        }

        TextView txtTituloReceta = convertView.findViewById(R.id.txtNombreReceta);
        ImageButton btnEliminar = convertView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre() != null ? receta.getNombre() : context.getString(R.string.sin_nombre));

        ImageView warning = convertView.findViewById(R.id.warning);
        ImageView postre = convertView.findViewById(R.id.imageViewPostreIcono);
        ImageView shared = convertView.findViewById(R.id.imageViewSharedIcono);

        postre.setVisibility(receta.isPostre() ? View.VISIBLE : View.GONE);
        shared.setVisibility(receta.isShared() ? View.VISIBLE : View.GONE);

        boolean hasBadScore = receta.getIngredientes() != null &&
                receta.getIngredientes().stream().anyMatch(i -> i.getPuntuacion() < -1);
        boolean invalidPersons = receta.getNumPersonas() <= 0;
        warning.setVisibility((invalidPersons || hasBadScore) ? View.VISIBLE : View.GONE);

        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.confirmacion))
                    .setMessage(context.getString(R.string.alerta_eliminar) + " '" + receta.getNombre() + "' ?")
                    .setPositiveButton(context.getString(R.string.aceptar), (dialog, which) -> {
                        // Eliminar la receta del JSON y refrescar la pantalla
                        RecetasSrv.eliminarReceta(groupPosition, listaRecetas, new RecetasSrv.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // siempre en main thread
                                mainHandler.post(() -> {
                                    if (emptyListListener != null) emptyListListener.reloadList(listaRecetas.size());
                                    notifyDataSetChanged();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                mainHandler.post(() -> {
                                    UtilsSrv.notificacion(context, context.getString(R.string.error_eliminar_receta), Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                });
                            }
                        });
                    })
                    .setNegativeButton(context.getString(R.string.cancelar), null)
                    .show();
        });

        ImageButton btnEditar = convertView.findViewById(R.id.btnEditar);
        btnEditar.setOnClickListener(v -> {
            editarReceta(groupPosition);
            // La edición abrirá la actividad; si la actividad edita y vuelve, la lista se actualizará desde la Activity/Fragment que maneja los datos.
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
        Intent intent = new Intent(context, EditarRecetaActivity.class);
        intent.putExtra("listaRecetas", (Serializable) listaRecetas);
        intent.putExtra("position", position);
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
        if (iconosAlergenos != null) iconosAlergenos.setVisibility(View.GONE);

        switch (childPosition) {
            case 0: // Temporadas
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.temporadas);
                List<String> temporadas = (receta.getTemporadas() != null)
                        ? receta.getTemporadas().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>();
                txtInformacion.setText(TextUtils.join(", ", temporadas));
                break;

            case 1: // Numero de personas
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.numero_personas);
                txtInformacion.setText(String.valueOf(receta.getNumPersonas()));
                break;

            case 2: // Ingredientes
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ingredientes);
                List<Ingrediente> ingredientes = receta.getIngredientes();
                if (ingredientes == null || ingredientes.isEmpty()) {
                    txtInformacion.setText(R.string.sin_ingredientes);
                } else {
                    StringBuilder sbIngredientes = new StringBuilder();
                    int totalIngredientes = ingredientes.size();

                    for (int i = 0; i < totalIngredientes; i++) {
                        Ingrediente ingrediente = ingredientes.get(i);
                        sbIngredientes.append("- ")
                                .append(ingrediente.getCantidad() != null ? ingrediente.getCantidad() : "")
                                .append(" ")
                                .append(ingrediente.getTipoCantidad() != null ? ingrediente.getTipoCantidad() : "")
                                .append(context.getString(R.string.literal_de))
                                .append(ingrediente.getNombre() != null ? ingrediente.getNombre() : "");

                        double puntuacion = ingrediente.getPuntuacion();
                        if (puntuacion >= 0) {
                            sbIngredientes.append(" (Score: ").append(puntuacion).append(")");
                        } else if (puntuacion != -1) {
                            sbIngredientes.append(" (Score no encontrado)");
                        }

                        if (i < totalIngredientes - 1) {
                            sbIngredientes.append("\n\n");
                        } else {
                            sbIngredientes.append("\n");
                        }
                    }
                    // proteger caso raro de longitud 0
                    String texto = sbIngredientes.length() > 0 ? sbIngredientes.toString().trim() : context.getString(R.string.sin_ingredientes);
                    txtInformacion.setText(texto);
                }
                break;

            case 3: // Pasos + tiempo total
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.pasos);
                List<com.david.recetapp.negocio.beans.Paso> pasos = receta.getPasos();
                if (pasos == null || pasos.isEmpty()) {
                    txtInformacion.setText(R.string.sin_pasos);
                } else {
                    SpannableStringBuilder sbPasos = new SpannableStringBuilder();
                    int totalPasos = pasos.size();
                    int minutosTotales = 0;

                    for (int i = 0; i < totalPasos; i++) {
                        String tiempoReceta = pasos.get(i).getTiempo(); // esperado "HH:MM"
                        try {
                            String[] tiempos = tiempoReceta.split(":");
                            int horas = Integer.parseInt(tiempos[0]);
                            int minutos = Integer.parseInt(tiempos[1]);
                            minutosTotales += minutos + 60 * horas;
                        } catch (Exception ex) {
                            // Si el formato falla, ignoramos el tiempo de este paso
                        }
                    }

                    int horasTot = minutosTotales / 60;
                    int minutosTot = minutosTotales % 60;
                    String tiempoTotal = String.format(Locale.getDefault(), "%02d:%02d", horasTot, minutosTot);

                    SpannableStringBuilder sbResaltado = new SpannableStringBuilder(context.getString(R.string.tiempo_total) + tiempoTotal);
                    int startIndex = sbResaltado.length() - tiempoTotal.length();
                    int endIndex = sbResaltado.length();
                    sbResaltado.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sbResaltado.setSpan(new UnderlineSpan(), 0, sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    sbPasos.append(sbResaltado);
                    sbPasos.append("\n\n");

                    for (int i = 0; i < totalPasos; i++) {
                        String tiempo = pasos.get(i).getTiempo();
                        String paso = pasos.get(i).getPaso();
                        String pasoFormateado = "[" + (tiempo != null ? tiempo : "00:00") + "] " + (i + 1) + ") " + (paso != null ? paso : "");
                        SpannableString spannablePaso = new SpannableString(pasoFormateado);
                        int startPos = pasoFormateado.indexOf("[");
                        int endPos = pasoFormateado.indexOf("]") + 1;
                        if (startPos >= 0 && endPos > startPos) {
                            spannablePaso.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startPos, endPos, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        sbPasos.append(spannablePaso);
                        if (i < totalPasos - 1) sbPasos.append("\n\n");
                    }

                    txtInformacion.setText(sbPasos);
                }
                break;

            case 4: // Alergenos (icons)
                txtTitulo.setText(R.string.alergenos);
                if (iconosAlergenos != null) {
                    iconosAlergenos.setVisibility(View.VISIBLE);
                    iconosAlergenos.removeAllViews();
                    List<Alergeno> alergenos = receta.getAlergenos();
                    if (alergenos == null || alergenos.isEmpty()) {
                        TextView textView = new TextView(context);
                        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        textView.setText(R.string.sin_al_rgenos);
                        iconosAlergenos.addView(textView);
                    } else {
                        for (Alergeno alergeno : alergenos) {
                            ImageView imageView = new ImageView(context);
                            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            imageView.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));
                            iconosAlergenos.addView(imageView);
                        }
                    }
                }
                txtInformacion.setVisibility(View.GONE);
                break;

            case 5: // Estrellas
                txtTitulo.setText(R.string.estrellas);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(receta.getEstrellas());
                txtInformacion.setVisibility(View.GONE);
                break;

            case 6: // Fecha calendario
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.ultima_fecha_calendario);
                if (receta.getFechaCalendario() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        txtInformacion.setText(dateFormat.format(receta.getFechaCalendario()));
                    } catch (Exception ex) {
                        txtInformacion.setText("-");
                    }
                } else {
                    txtInformacion.setText("-");
                }
                break;

            case 7: // Puntuacion dada
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.puntuacion_dada);
                txtInformacion.setText(String.valueOf(receta.getPuntuacionDada()));
                break;

            default:
                txtInformacion.setVisibility(View.GONE);
                txtTitulo.setText("");
                break;
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    // --------------------------
    // MÉTODOS NUEVOS / ÚTILES
    // --------------------------

    /**
     * Actualiza los datos del adapter de forma segura (siempre en el hilo principal).
     * Llama a notifyDataSetChanged() y notifica al listener si existe.
     */
    public void updateData(final List<Receta> nuevas) {
        mainHandler.post(() -> {
            listaRecetas.clear();
            if (nuevas != null && !nuevas.isEmpty()) {
                listaRecetas.addAll(nuevas);
            }
            notifyDataSetChanged();
            if (emptyListListener != null) {
                emptyListListener.reloadList(listaRecetas.size());
            }
        });
    }

    /**
     * Método helper para actualizar la UI desde callbacks de background si se necesita.
     */
    private void postNotifyChanged() {
        mainHandler.post(this::notifyDataSetChanged);
    }

    public interface EmptyListListener {
        void reloadList(int count);
    }
}
