package com.david.recetapp.adaptadores;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecetaExpandableListCalendarAdapter extends BaseExpandableListAdapter {
    private final Day selectedDay;
    private final Activity activity;
    private List<Receta> listaRecetas;
    private final ExpandableListView expandableListView;
    private final EmptyListListener emptyListListener;
    private final Handler mainHandler;

    public RecetaExpandableListCalendarAdapter(Activity activity, Day selectedDay, ExpandableListView expandableListView, EmptyListListener emptyListListener) {
        this.selectedDay = selectedDay;
        this.activity = activity;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
        this.listaRecetas = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Cargar recetas adaptadas
        cargarRecetasAdaptadas();
    }

    private void cargarRecetasAdaptadas() {
        RecetasSrv.cargarListaRecetas(activity, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                listaRecetas = RecetasSrv.getRecetasAdaptadasCalendario(recetas, selectedDay);
                mainHandler.post(() -> notifyDataSetChanged());
            }

            @Override
            public void onFailure(Exception e) {
                listaRecetas = new ArrayList<>();
                mainHandler.post(() -> {
                    UtilsSrv.notificacion(activity, activity.getString(R.string.error_cargar_recetas), Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                });
            }
        });
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
        return ((long) groupPosition << 32) | (childPosition & 0xffffffffL);
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
        txtTituloReceta.setText(receta.getNombre() != null ? receta.getNombre() : activity.getString(R.string.sin_nombre));

        ImageView warning = convertView.findViewById(R.id.warning);
        ImageView iconoTipo = convertView.findViewById(R.id.imageViewTipoIcono);
        ImageView shared = convertView.findViewById(R.id.imageViewSharedIcono);

        iconoTipo.setVisibility(receta.getTipoReceta() == TipoReceta.POSTRE ? View.VISIBLE : View.GONE);

        if (receta.getTipoReceta() == TipoReceta.COCTEL) {
            iconoTipo.setImageResource(R.drawable.ic_baseline_local_bar_24);
            iconoTipo.setVisibility(View.VISIBLE);
        } else if (receta.getTipoReceta() == TipoReceta.SIDE) {
            iconoTipo.setImageResource(R.drawable.ic_baseline_flatware_24);
            iconoTipo.setVisibility(View.VISIBLE);
        } else {
            iconoTipo.setImageResource(R.drawable.postre_icono);
        }

        shared.setVisibility(receta.isShared() ? View.VISIBLE : View.GONE);

        boolean hasBadScore = receta.getIngredientes() != null &&
                receta.getIngredientes().stream().anyMatch(i -> i.getPuntuacion() < -1);
        boolean invalidPersons = receta.getNumPersonas() <= 0;
        warning.setVisibility((invalidPersons || hasBadScore) ? View.VISIBLE : View.GONE);

        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.confirmacion))
                    .setMessage(activity.getString(R.string.alerta_eliminar) + " '" + receta.getNombre() + "' ?")
                    .setPositiveButton(activity.getString(R.string.aceptar), (dialog, which) -> {
                        // Eliminar la receta del calendario y refrescar la pantalla
                        Receta eliminada = listaRecetas.remove(groupPosition);
                        selectedDay.removeReceta(eliminada.getId());

                        CalendarioSrv.actualizarDia(activity, selectedDay.getMonth(), selectedDay.getYear(), selectedDay, new CalendarioSrv.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // Aquí no podemos actualizarFechaCalendario fácilmente sin el mes/año
                                // Pero actualizarFechaCalendario ya busca el día más reciente
                                CalendarioSrv.actualizarFechaCalendario(activity, eliminada.getId());
                                mainHandler.post(() -> {
                                    if (listaRecetas.size() < 2) {
                                        emptyListListener.onListSize();
                                    }
                                    if (listaRecetas.isEmpty()) {
                                        emptyListListener.onListEmpty();
                                    }
                                    notifyDataSetChanged();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Restaurar la receta si falla
                                listaRecetas.add(groupPosition, eliminada);
                                selectedDay.getRecetas().add(new com.david.recetapp.negocio.beans.RecetaDia(
                                        eliminada.getId(), eliminada.getNumPersonas()));
                                mainHandler.post(() -> {
                                    UtilsSrv.notificacion(activity, activity.getString(R.string.error_eliminar_receta), Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                });
                            }
                        });
                    })
                    .setNegativeButton(activity.getString(R.string.cancelar), null)
                    .show();
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
        if (iconosAlergenos != null) iconosAlergenos.setVisibility(View.GONE);

        switch (childPosition) {
            case 0: // Temporadas
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.temporadas);
                List<String> temporadas = (receta.getTemporadas() != null)
                        ? receta.getTemporadas().stream().map(Temporada::getStringRes).map(activity::getString).collect(Collectors.toList())
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
                                .append(activity.getString(R.string.literal_de))
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
                    String texto = sbIngredientes.length() != 0 ? sbIngredientes.toString().trim() : activity.getString(R.string.sin_ingredientes);
                    txtInformacion.setText(texto);
                }
                break;

            case 3: // Pasos
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
                        String tiempoReceta = pasos.get(i).getTiempo(); // HH:MM
                        try {
                            String[] tiempos = tiempoReceta.split(":");
                            int h = Integer.parseInt(tiempos[0]);
                            int m = Integer.parseInt(tiempos[1]);
                            minutosTotales += m + 60 * h;
                        } catch (Exception ignored) {}
                    }

                    int hTotal = minutosTotales / 60;
                    int mTotal = minutosTotales % 60;
                    String tiempoTotal = String.format(Locale.getDefault(), "%02d:%02d", hTotal, mTotal);

                    SpannableStringBuilder sbResaltado = new SpannableStringBuilder(activity.getString(R.string.tiempo_total) + tiempoTotal);
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

            case 4: // Alergenos
                txtTitulo.setText(R.string.alergenos);
                txtInformacion.setVisibility(View.GONE);
                if (iconosAlergenos != null) {
                    iconosAlergenos.setVisibility(View.VISIBLE);
                    iconosAlergenos.removeAllViews();
                    List<Alergeno> alergenos = receta.getAlergenos();
                    if (alergenos == null || alergenos.isEmpty()) {
                        TextView textView = new TextView(activity);
                        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        textView.setText(R.string.sin_al_rgenos);
                        iconosAlergenos.addView(textView);
                    } else {
                        for (Alergeno alergeno : alergenos) {
                            ImageView imageView = new ImageView(activity);
                            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            imageView.setImageResource(AlergenosSrv.obtenerImagen(alergeno.getNumero()));
                            iconosAlergenos.addView(imageView);
                        }
                    }
                }
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
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
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