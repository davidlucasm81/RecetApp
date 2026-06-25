package com.david.recetapp.adaptadores;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.LeadingMarginSpan;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.MomentoReceta;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecetaExpandableListCalendarAdapter extends BaseExpandableListAdapter {
    private final Day selectedDay;
    private final Activity activity;
    private List<Receta> listaRecetas;
    private final ExpandableListView expandableListView;
    private final EmptyListListener emptyListListener;
    private final Handler mainHandler;

    public RecetaExpandableListCalendarAdapter(Activity activity, Day selectedDay, 
                                               ExpandableListView expandableListView,
                                               EmptyListListener emptyListListener) {
        this.selectedDay = selectedDay;
        this.activity = activity;
        this.expandableListView = expandableListView;
        this.emptyListListener = emptyListListener;
        this.listaRecetas = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        cargarRecetasAdaptadas();
    }

    private void cargarRecetasAdaptadas() {
        RecetasSrv.cargarListaRecetas(activity, new RecetasSrv.RecetasCallback() {
            @Override
            public void onSuccess(List<Receta> recetas) {
                List<Receta> adaptadas = RecetasSrv.getRecetasAdaptadasCalendario(recetas, selectedDay);
                adaptadas.sort((r1, r2) -> Integer.compare(getMomentoPriority(r1), getMomentoPriority(r2)));
                listaRecetas = adaptadas;
                notifyDataSetChanged();
                if (emptyListListener != null) {
                    emptyListListener.reloadList(listaRecetas.size());
                }
            }
            @Override
            public void onFailure(Exception e) {
                listaRecetas = new ArrayList<>();
                Toast.makeText(activity, activity.getString(R.string.error_cargar_recetas), Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
                if (emptyListListener != null) {
                    emptyListListener.reloadList(0);
                }
            }
        });
    }

    private int getMomentoPriority(Receta r) {
        if (r.getMomentoReceta() == null) return 99;
        return switch (r.getMomentoReceta()) {
            case COMIDA -> 1;
            case AMBOS -> 2;
            case CENA -> 3;
        };
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 9;
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) return null;
        return listaRecetas.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) return null;
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
            case 8 -> receta.getMomentoReceta();
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

    private String getMomentoText(Receta receta) {
        if (receta.getTipoReceta() != null && receta.getTipoReceta() != TipoReceta.PRINCIPAL) {
            return switch (receta.getTipoReceta()) {
                case POSTRE -> activity.getString(R.string.tipo_postre);
                case COCTEL -> activity.getString(R.string.tipo_coctel);
                case SIDE -> activity.getString(R.string.tipo_side);
                default -> "-";
            };
        }
        if (receta.getMomentoReceta() != null) {
            return receta.getMomentoReceta() == MomentoReceta.AMBOS ? activity.getString(R.string.ambos) : receta.getMomentoReceta().name();
        }
        return "-";
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View groupView = convertView;
        if (groupView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            groupView = inflater.inflate(R.layout.list_item_group_calendar, parent, false);
        }

        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) {
            return groupView;
        }

        TextView txtTituloReceta = groupView.findViewById(R.id.txtNombreReceta);
        ImageView warning = groupView.findViewById(R.id.warning);
        ImageView iconoTipo = groupView.findViewById(R.id.imageViewTipoIcono);
        TextView txtMomento = groupView.findViewById(R.id.txtMomento);
        ImageButton btnEliminar = groupView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre());
        txtMomento.setText(getMomentoText(receta));

        iconoTipo.setVisibility(receta.getTipoReceta() == TipoReceta.POSTRE ? View.VISIBLE : View.GONE);
        if (receta.getTipoReceta() == TipoReceta.COCTEL) {
            iconoTipo.setImageResource(R.drawable.ic_baseline_local_bar_24);
            iconoTipo.setVisibility(View.VISIBLE);
        } else if (receta.getTipoReceta() == TipoReceta.SIDE) {
            iconoTipo.setImageResource(R.drawable.ic_baguette);
            iconoTipo.setVisibility(View.VISIBLE);
        } else {
            iconoTipo.setImageResource(R.drawable.postre_icono);
        }

        boolean hasBadScore = receta.getIngredientes() != null &&
                receta.getIngredientes().stream().anyMatch(i -> i.getPuntuacion() < -1);
        boolean invalidPersons = receta.getNumPersonas() <= 0;
        warning.setVisibility((invalidPersons || hasBadScore) ? View.VISIBLE : View.GONE);

        btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.confirmacion))
                    .setMessage(activity.getString(R.string.alerta_eliminar, receta.getNombre()))
                    .setPositiveButton(activity.getString(R.string.aceptar), (dialog, which) -> {
                        // Guardar fecha de calendario actual de la receta para comprobar si es la que borramos
                        boolean esFechaActual = isEsFechaActual(receta);

                        // Eliminar de selectedDay
                        selectedDay.getRecetas().removeIf(rd -> rd.getIdReceta().equals(receta.getId()));
                        
                        // Actualizar en el servidor
                        CalendarioSrv.actualizarDia(activity, selectedDay.getMonth(), selectedDay.getYear(), selectedDay, new CalendarioSrv.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (esFechaActual) {
                                    CalendarioSrv.actualizarFechaCalendario(activity, receta.getId(), selectedDay.getMonth(), selectedDay.getYear());
                                }
                                mainHandler.post(() -> {
                                    cargarRecetasAdaptadas();
                                    if (emptyListListener != null) {
                                        emptyListListener.reloadList(selectedDay.getRecetas().size());
                                    }
                                });
                            }
                            @Override
                            public void onFailure(Exception e) {
                                mainHandler.post(() -> Toast.makeText(activity, activity.getString(R.string.error_eliminar_receta), Toast.LENGTH_SHORT).show());
                            }
                        });
                    })
                    .setNegativeButton(activity.getString(R.string.cancelar), null)
                    .show();
        });

        groupView.setOnClickListener(v -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
            }
        });

        return groupView;
    }

    private boolean isEsFechaActual(Receta receta) {
        Date fechaReceta = receta.getFechaCalendario();
        // Comparar solo Año/Mes/Día para evitar desajustes por zonas horarias/horas
        boolean esFechaActual;
        if (fechaReceta != null) {
            Calendar calReceta = Calendar.getInstance();
            calReceta.setTime(fechaReceta);
            esFechaActual = calReceta.get(Calendar.YEAR) == selectedDay.getYear()
                    && calReceta.get(Calendar.MONTH) == selectedDay.getMonth()
                    && calReceta.get(Calendar.DAY_OF_MONTH) == selectedDay.getDayOfMonth();
        } else {
            esFechaActual = false;
        }
        return esFechaActual;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_child, parent, false);
        }

        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) {
            return view;
        }

        TextView txtTitulo = view.findViewById(R.id.txtTitulo);
        TextView txtInformacion = view.findViewById(R.id.txtInformacion);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        LinearLayout iconosAlergenos = view.findViewById(R.id.iconosAlergenos);

        Receta receta = listaRecetas.get(groupPosition);
        txtInformacion.setMovementMethod(null);

        switch (childPosition) {
            case 0:
                txtTitulo.setText(R.string.temporadas);
                txtInformacion.setVisibility(View.VISIBLE);
                List<String> temps = (receta.getTemporadas() != null)
                        ? receta.getTemporadas().stream().map(Temporada::getStringRes).map(activity::getString).collect(Collectors.toList())
                        : new ArrayList<>();
                txtInformacion.setText(TextUtils.join(", ", temps));
                break;
            case 1:
                txtTitulo.setText(R.string.numero_personas);
                txtInformacion.setVisibility(View.VISIBLE);
                txtInformacion.setText(String.valueOf(receta.getNumPersonas()));
                break;
            case 2:
                txtTitulo.setText(R.string.ingredientes);
                txtInformacion.setVisibility(View.VISIBLE);
                List<Ingrediente> ingredients = receta.getIngredientes();
                if (ingredients == null || ingredients.isEmpty()) {
                    txtInformacion.setText(R.string.sin_ingredientes);
                } else {
                    SpannableStringBuilder sbIngredientes = new SpannableStringBuilder();
                    java.util.Map<String, List<Ingrediente>> grupos = new java.util.LinkedHashMap<>();
                    List<Ingrediente> principales = new ArrayList<>();
                    for (Ingrediente ing : ingredients) {
                        if (ing.getEsSustitutoDe() == null || ing.getEsSustitutoDe().isEmpty()) {
                            principales.add(ing);
                        }
                    }
                    for (Ingrediente principal : principales) grupos.put(principal.getNombre(), new ArrayList<>());
                    for (Ingrediente ing : ingredients) {
                        if (ing.getEsSustitutoDe() != null && !ing.getEsSustitutoDe().isEmpty()) {
                            List<Ingrediente> susts = grupos.get(ing.getEsSustitutoDe());
                            Objects.requireNonNullElse(susts, principales).add(ing);
                        }
                    }
                    for (int i = 0; i < principales.size(); i++) {
                        Ingrediente principal = principales.get(i);
                        appendIngredienteInfo(sbIngredientes, principal, false);
                        List<Ingrediente> sustitutos = grupos.get(principal.getNombre());
                        if (sustitutos != null) {
                            for (Ingrediente sust : sustitutos) {
                                sbIngredientes.append("\n");
                                appendIngredienteInfo(sbIngredientes, sust, true);
                            }
                        }
                        if (i < principales.size() - 1) sbIngredientes.append("\n\n");
                        else sbIngredientes.append("\n");
                    }
                    if (sbIngredientes.length() != 0) {
                        txtInformacion.setMovementMethod(LinkMovementMethod.getInstance());
                        txtInformacion.setText(sbIngredientes);
                    } else txtInformacion.setText(activity.getString(R.string.sin_ingredientes));
                }
                break;
            case 3:
                txtTitulo.setText(R.string.pasos);
                txtInformacion.setVisibility(View.VISIBLE);
                List<com.david.recetapp.negocio.beans.Paso> pasos = receta.getPasos();
                if (pasos == null || pasos.isEmpty()) {
                    txtInformacion.setText(R.string.sin_pasos);
                } else {
                    SpannableStringBuilder sbPasos = new SpannableStringBuilder();
                    int minutosTotales = 0;
                    for (com.david.recetapp.negocio.beans.Paso p : pasos) {
                        try {
                            String[] t = p.getTiempo().split(":");
                            minutosTotales += Integer.parseInt(t[1]) + 60 * Integer.parseInt(t[0]);
                        } catch (Exception ignored) {}
                    }
                    String tiempoTotal = String.format(Locale.getDefault(), "%02d:%02d", minutosTotales / 60, minutosTotales % 60);
                    SpannableStringBuilder sbResaltado = new SpannableStringBuilder(activity.getString(R.string.tiempo_total) + tiempoTotal);
                    sbResaltado.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), sbResaltado.length() - tiempoTotal.length(), sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sbResaltado.setSpan(new UnderlineSpan(), 0, sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sbPasos.append(sbResaltado).append("\n\n");
                    for (int i = 0; i < pasos.size(); i++) {
                        String s = "[" + (pasos.get(i).getTiempo() != null ? pasos.get(i).getTiempo() : "00:00") + "] " + (i + 1) + ") " + (pasos.get(i).getPaso() != null ? pasos.get(i).getPaso() : "");
                        SpannableString sp = new SpannableString(s);
                        int st = s.indexOf("[");
                        int en = s.indexOf("]") + 1;
                        if (st >= 0 && en > st) sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), st, en, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                        sbPasos.append(sp);
                        if (i < pasos.size() - 1) sbPasos.append("\n\n");
                    }
                    txtInformacion.setText(sbPasos);
                }
                break;
            case 4:
                txtTitulo.setText(R.string.alergenos);
                txtInformacion.setVisibility(View.GONE);
                iconosAlergenos.setVisibility(View.VISIBLE);
                iconosAlergenos.removeAllViews();
                List<Alergeno> alergenos = receta.getAlergenos();
                if (alergenos == null || alergenos.isEmpty()) {
                    TextView tv = new TextView(activity);
                    tv.setText(R.string.sin_al_rgenos);
                    iconosAlergenos.addView(tv);
                } else {
                    for (Alergeno a : alergenos) {
                        ImageView iv = new ImageView(activity);
                        iv.setImageResource(AlergenosSrv.obtenerImagen(a.getNumero()));
                        iconosAlergenos.addView(iv);
                    }
                }
                break;
            case 5:
                txtTitulo.setText(R.string.estrellas);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(receta.getEstrellas());
                txtInformacion.setVisibility(View.GONE);
                break;
            case 6:
                txtTitulo.setText(R.string.ultima_fecha_calendario);
                txtInformacion.setVisibility(View.VISIBLE);
                if (receta.getFechaCalendario() != null) {
                    txtInformacion.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(receta.getFechaCalendario()));
                } else txtInformacion.setText("-");
                break;
            case 7:
                txtTitulo.setText(R.string.puntuacion_dada);
                txtInformacion.setVisibility(View.VISIBLE);
                txtInformacion.setText(String.format(Locale.getDefault(), "%.1f", receta.getPuntuacionDada()));
                break;
            case 8:
                txtTitulo.setText(R.string.momento_receta);
                txtInformacion.setVisibility(View.VISIBLE);
                txtInformacion.setText(getMomentoText(receta));
                break;
        }

        if (childPosition != 4) iconosAlergenos.setVisibility(View.GONE);
        if (childPosition != 5) ratingBar.setVisibility(View.GONE);

        return view;
    }

    private void appendIngredienteInfo(SpannableStringBuilder sb, Ingrediente ing, boolean isSustituto) {
        String nombreTraducido = RecetasSrv.getNombreTraducido(ing.getNombre());
        String nombreMostrado = (nombreTraducido != null) ? nombreTraducido : ing.getNombre();
        String prefix = isSustituto ? "  └ " : "- ";
        StringBuilder linea = new StringBuilder();
        linea.append(prefix)
                .append(ing.getCantidad() != null ? ing.getCantidad() : "")
                .append(" ")
                .append(ing.getTipoCantidad() != null ? ing.getTipoCantidad() : "")
                .append(activity.getString(R.string.literal_de))
                .append(nombreMostrado);

        if (ing.isOpcional()) linea.append(" (").append(activity.getString(R.string.opcional).toLowerCase(Locale.getDefault())).append(")");
        if (isSustituto) linea.append(" (").append(activity.getString(R.string.sustituto_de).toLowerCase(Locale.getDefault())).append(" ").append(ing.getEsSustitutoDe()).append(")");
        double p = ing.getPuntuacion();
        if (p >= 0) linea.append(" (").append(activity.getString(R.string.score_label)).append(": ").append(String.format(Locale.getDefault(), "%.1f", p)).append(")");
        else if (p < -1) linea.append(" (").append(activity.getString(R.string.score_no_encontrado)).append(")");

        int start = sb.length();
        sb.append(linea.toString());
        int end = sb.length();

        if (ing.getRecetaId() != null && !ing.getRecetaId().isEmpty()) {
            int linkStart = start + prefix.length();
            sb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) { navegarAReceta(ing.getRecetaId()); }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(activity.getResources().getColor(R.color.colorPrimary, activity.getTheme()));
                }
            }, linkStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isSustituto) {
            TextView tmp = new TextView(activity);
            float w = tmp.getPaint().measureText(prefix);
            sb.setSpan(new LeadingMarginSpan.Standard(0, (int) Math.ceil(w)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void navegarAReceta(String recetaId) {
        for (int i = 0; i < listaRecetas.size(); i++) {
            if (listaRecetas.get(i).getId().equals(recetaId)) {
                expandableListView.collapseGroup(i);
                expandableListView.expandGroup(i);
                expandableListView.setSelectedGroup(i);
                return;
            }
        }
        Toast.makeText(activity, activity.getString(R.string.error_receta_no_encontrada_dia), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface EmptyListListener {
        void reloadList(int count);
    }
}
