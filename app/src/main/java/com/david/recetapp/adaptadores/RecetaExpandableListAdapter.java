package com.david.recetapp.adaptadores;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.david.recetapp.R;
import com.david.recetapp.actividades.recetas.EditarRecetaActivity;
import com.david.recetapp.negocio.beans.Alergeno;
import com.david.recetapp.negocio.beans.Ingrediente;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.Temporada;
import com.david.recetapp.negocio.beans.TipoReceta;
import com.david.recetapp.negocio.servicios.AlergenosSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecetaExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final ExpandableListView expandableListView;
    private final EmptyListListener emptyListListener;
    private final Handler mainHandler;
    private final ViewGroup anchorContainer;
    private OnNavigateToRecipeListener navigateListener;

    // Singleton del reproductor para toda la lista
    private YouTubePlayerView sharedYouTubePlayerView;
    private YouTubePlayer activePlayer;
    private String currentVideoId;

    private final List<Receta> listaRecetas;

    public interface OnNavigateToRecipeListener {
        void onNavigate(String recetaId);
    }

    public void setOnNavigateToRecipeListener(OnNavigateToRecipeListener listener) {
        this.navigateListener = listener;
    }

    public RecetaExpandableListAdapter(Context context, List<Receta> listaRecetas,
                                       ExpandableListView expandableListView,
                                       ViewGroup anchorContainer,
                                       EmptyListListener emptyListListener) {
        this.context = context;
        this.expandableListView = expandableListView;
        this.anchorContainer = anchorContainer;
        this.emptyListListener = emptyListListener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.listaRecetas = (listaRecetas != null) ? new ArrayList<>(listaRecetas) : new ArrayList<>();
    }

    @Override
    public int getGroupCount() {
        return listaRecetas.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) return 0;
        Receta receta = listaRecetas.get(groupPosition);
        boolean hasYoutube = receta.getYoutubeUrl() != null && !receta.getYoutubeUrl().trim().isEmpty();
        return hasYoutube ? 10 : 9;
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
            case 9 -> receta.getYoutubeUrl();
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
    public int getChildTypeCount() {
        return 2;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return (childPosition == 9) ? 1 : 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View groupView = convertView;
        if (groupView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            groupView = inflater.inflate(R.layout.list_item_group, parent, false);
        }

        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) return groupView;

        TextView txtTituloReceta = groupView.findViewById(R.id.txtNombreReceta);
        ImageButton btnEliminar = groupView.findViewById(R.id.btnEliminar);
        ImageButton btnEditar = groupView.findViewById(R.id.btnEditar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre() != null ? receta.getNombre() : context.getString(R.string.sin_nombre));

        ImageView warning = groupView.findViewById(R.id.warning);
        ImageView iconoTipo = groupView.findViewById(R.id.imageViewTipoIcono);
        ImageView shared = groupView.findViewById(R.id.imageViewSharedIcono);

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

        shared.setVisibility(receta.isShared() ? View.VISIBLE : View.GONE);

        boolean hasBadScore = receta.getIngredientes() != null &&
                receta.getIngredientes().stream().anyMatch(i -> i.getPuntuacion() < -1);
        boolean invalidPersons = receta.getNumPersonas() <= 0;
        warning.setVisibility((invalidPersons || hasBadScore) ? View.VISIBLE : View.GONE);

        btnEliminar.setOnClickListener(vParam -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.confirmacion))
                    .setMessage(context.getString(R.string.alerta_eliminar, receta.getNombre()))
                    .setPositiveButton(context.getString(R.string.aceptar), (dialog, which) -> RecetasSrv.eliminarReceta(groupPosition, listaRecetas, new RecetasSrv.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            mainHandler.post(() -> {
                                if (emptyListListener != null) emptyListListener.reloadList(listaRecetas.size());
                                notifyDataSetChanged();
                            });
                        }
                        @Override
                        public void onFailure(Exception e) {
                            mainHandler.post(() -> Toast.makeText(context, context.getString(R.string.error_eliminar_receta), Toast.LENGTH_SHORT).show());
                        }
                    }))
                    .setNegativeButton(context.getString(R.string.cancelar), null)
                    .show();
        });

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditarRecetaActivity.class);
            intent.putExtra("position", groupPosition);
            intent.putExtra("listaRecetas", (Serializable) listaRecetas);
            context.startActivity(intent);
        });

        groupView.setOnClickListener(v -> {
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                // Cerrar todos los grupos abiertos previamente para optimizar el player compartido
                int groupCount = getGroupCount();
                for (int i = 0; i < groupCount; i++) {
                    if (i != groupPosition && expandableListView.isGroupExpanded(i)) {
                        expandableListView.collapseGroup(i);
                    }
                }
                expandableListView.expandGroup(groupPosition);
            }
        });

        return groupView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        int type = getChildType(groupPosition, childPosition);
        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) {
            return (convertView != null) ? convertView : new View(context);
        }

        if (type == 1) {
            return getYouTubeChildView(groupPosition, convertView, parent);
        }

        View childView = convertView;
        if (childView == null || childView.findViewById(R.id.youtube_placeholder) != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            childView = inflater.inflate(R.layout.list_item_child, parent, false);
        }

        TextView txtTitulo = childView.findViewById(R.id.txtTitulo);
        TextView txtInformacion = childView.findViewById(R.id.txtInformacion);
        LinearLayout iconosAlergenos = childView.findViewById(R.id.iconosAlergenos);
        RatingBar ratingBar = childView.findViewById(R.id.ratingBar);

        Receta receta = listaRecetas.get(groupPosition);
        txtInformacion.setMovementMethod(null);

        switch (childPosition) {
            case 0:
                txtTitulo.setText(R.string.temporadas);
                txtInformacion.setVisibility(View.VISIBLE);
                List<String> temps = (receta.getTemporadas() != null)
                        ? receta.getTemporadas().stream().map(Temporada::getStringRes).map(context::getString).collect(Collectors.toList())
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
                List<Ingrediente> ingredientes = receta.getIngredientes();
                if (ingredientes == null || ingredientes.isEmpty()) {
                    txtInformacion.setText(context.getString(R.string.sin_ingredientes));
                } else {
                    SpannableStringBuilder sbIngredientes = new SpannableStringBuilder();
                    Map<String, List<Ingrediente>> grupos = new java.util.HashMap<>();
                    List<Ingrediente> principales = new ArrayList<>();

                    for (Ingrediente ing : ingredientes) {
                        if (ing.getEsSustitutoDe() == null || ing.getEsSustitutoDe().isEmpty()) {
                            principales.add(ing);
                        }
                    }

                    for (Ingrediente principal : principales) {
                        grupos.put(principal.getNombre(), new java.util.ArrayList<>());
                    }

                    for (Ingrediente ing : ingredientes) {
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

                        if (i < principales.size() - 1) {
                            sbIngredientes.append("\n\n");
                        } else {
                            sbIngredientes.append("\n");
                        }
                    }
                    if (sbIngredientes.length() != 0) {
                        txtInformacion.setMovementMethod(LinkMovementMethod.getInstance());
                        txtInformacion.setText(sbIngredientes);
                    } else {
                        txtInformacion.setText(context.getString(R.string.sin_ingredientes));
                    }
                }
                break;
            case 3:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.pasos);
                List<com.david.recetapp.negocio.beans.Paso> pasos = receta.getPasos();
                if (pasos == null || pasos.isEmpty()) {
                    txtInformacion.setText(R.string.sin_pasos);
                } else {
                    SpannableStringBuilder sbPasos = new SpannableStringBuilder();
                    int minutosTotales = 0;
                    for (com.david.recetapp.negocio.beans.Paso p : pasos) {
                        try {
                            String[] tiempos = p.getTiempo().split(":");
                            minutosTotales += Integer.parseInt(tiempos[1]) + 60 * Integer.parseInt(tiempos[0]);
                        } catch (Exception ignored) {}
                    }
                    String tiempoTotal = String.format(Locale.getDefault(), "%02d:%02d", minutosTotales / 60, minutosTotales % 60);
                    SpannableStringBuilder sbResaltado = new SpannableStringBuilder(context.getString(R.string.tiempo_total) + tiempoTotal);
                    sbResaltado.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), sbResaltado.length() - tiempoTotal.length(), sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sbResaltado.setSpan(new UnderlineSpan(), 0, sbResaltado.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sbPasos.append(sbResaltado).append("\n\n");

                    for (int i = 0; i < pasos.size(); i++) {
                        String passoStr = "[" + (pasos.get(i).getTiempo() != null ? pasos.get(i).getTiempo() : "00:00") + "] " + (i + 1) + ") " + (pasos.get(i).getPaso() != null ? pasos.get(i).getPaso() : "");
                        SpannableString sp = new SpannableString(passoStr);
                        int startPos = passoStr.indexOf("[");
                        int endPos = passoStr.indexOf("]") + 1;
                        if (startPos >= 0 && endPos > startPos) sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startPos, endPos, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                        sbPasos.append(sp);
                        if (i < pasos.size() - 1) sbPasos.append("\n\n");
                    }
                    txtInformacion.setText(sbPasos);
                }
                break;
            case 4:
                txtTitulo.setText(R.string.alergenos);
                iconosAlergenos.setVisibility(View.VISIBLE);
                iconosAlergenos.removeAllViews();
                List<Alergeno> alergenos = receta.getAlergenos();
                if (alergenos == null || alergenos.isEmpty()) {
                    TextView tv = new TextView(context);
                    tv.setText(R.string.sin_al_rgenos);
                    iconosAlergenos.addView(tv);
                } else {
                    for (Alergeno a : alergenos) {
                        ImageView iv = new ImageView(context);
                        iv.setImageResource(AlergenosSrv.obtenerImagen(a.getNumero()));
                        iconosAlergenos.addView(iv);
                    }
                }
                txtInformacion.setVisibility(View.GONE);
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
                if (receta.getFechaCalendario() != null) {
                    txtInformacion.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(receta.getFechaCalendario()));
                } else {
                    txtInformacion.setText("-");
                }
                break;
            case 7:
                txtInformacion.setVisibility(View.VISIBLE);
                txtTitulo.setText(R.string.puntuacion_dada);
                txtInformacion.setText(String.format(Locale.getDefault(), "%.1f", receta.getPuntuacionDada()));
                break;
            case 8:
                txtTitulo.setText(R.string.momento_receta);
                txtInformacion.setVisibility(View.VISIBLE);
                if (receta.getTipoReceta() == TipoReceta.PRINCIPAL) {
                    txtInformacion.setText(receta.getMomentoReceta() != null ? context.getString(receta.getMomentoReceta().getStringRes()) : context.getString(R.string.ambos));
                } else {
                    txtInformacion.setVisibility(View.GONE);
                    txtTitulo.setText("");
                }
                break;
        }

        if (childPosition != 4) iconosAlergenos.setVisibility(View.GONE);
        if (childPosition != 5) ratingBar.setVisibility(View.GONE);

        return childView;
    }

    private View getYouTubeChildView(int groupPosition, View convertView, ViewGroup parent) {
        if (groupPosition < 0 || groupPosition >= listaRecetas.size()) {
            return (convertView != null) ? convertView : new View(context);
        }

        View view = convertView;
        if (view == null || view.findViewById(R.id.youtube_placeholder) == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_child_youtube, parent, false);
        }

        FrameLayout placeholder = view.findViewById(R.id.youtube_placeholder);
        TextView txtTitulo = view.findViewById(R.id.txtTitulo);
        TextView txtInformacion = view.findViewById(R.id.txtInformacion);
        
        Receta receta = listaRecetas.get(groupPosition);
        String youtubeUrl = receta.getYoutubeUrl();
        boolean hasUrl = youtubeUrl != null && !youtubeUrl.trim().isEmpty();

        if (!hasUrl) {
            txtTitulo.setText("");
            txtInformacion.setVisibility(View.GONE);
            if (placeholder != null) placeholder.setVisibility(View.GONE);
        } else {
            txtTitulo.setText(R.string.ver_video_youtube);
            txtInformacion.setVisibility(View.GONE);
            
            if (placeholder != null) {
                placeholder.setVisibility(View.VISIBLE);
                String videoId = UtilsSrv.extraerVideoId(youtubeUrl);
                
                if (videoId != null) {
                    ensureSharedPlayerInitialized();
                    
                    // Mover el reproductor al contenedor actual
                    ViewGroup currentParent = (ViewGroup) sharedYouTubePlayerView.getParent();
                    if (currentParent != placeholder) {
                        if (currentParent != null) currentParent.removeView(sharedYouTubePlayerView);
                        placeholder.addView(sharedYouTubePlayerView);
                    }

                    // Solo cargar si el video es distinto
                    if (!videoId.equals(currentVideoId)) {
                        currentVideoId = videoId;
                        if (activePlayer != null) {
                            activePlayer.cueVideo(videoId, 0f);
                        }
                    }

                    // Configurar el anclaje cuando la vista se desvincule
                    placeholder.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(@NonNull View v) { }

                        @Override
                        public void onViewDetachedFromWindow(@NonNull View v) {
                            // Si se desvincula y el player sigue en este placeholder, lo movemos al anclaje
                            if (sharedYouTubePlayerView != null && sharedYouTubePlayerView.getParent() == placeholder) {
                                placeholder.removeView(sharedYouTubePlayerView);
                                if (anchorContainer != null) {
                                    anchorContainer.addView(sharedYouTubePlayerView);
                                }
                            }
                            placeholder.removeOnAttachStateChangeListener(this);
                        }
                    });
                } else {
                    // Fallback: Si no podemos extraer el ID, intentamos abrir la URL
                    if (sharedYouTubePlayerView != null && sharedYouTubePlayerView.getParent() == placeholder) {
                        placeholder.removeView(sharedYouTubePlayerView);
                    }
                    txtInformacion.setVisibility(View.VISIBLE);
                    txtInformacion.setText(youtubeUrl);
                    txtInformacion.setOnClickListener(vLink -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(youtubeUrl));
                        context.startActivity(intent);
                    });
                }
            }
        }
        return view;
    }

    private void ensureSharedPlayerInitialized() {
        if (sharedYouTubePlayerView == null) {
            sharedYouTubePlayerView = new YouTubePlayerView(context);
            sharedYouTubePlayerView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
            sharedYouTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    activePlayer = youTubePlayer;
                    if (currentVideoId != null) {
                        youTubePlayer.cueVideo(currentVideoId, 0f);
                    }
                }
            });
        }
    }

    public void release() {
        if (sharedYouTubePlayerView != null) {
            sharedYouTubePlayerView.release();
            sharedYouTubePlayerView = null;
            activePlayer = null;
        }
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
                .append(context.getString(R.string.literal_de))
                .append(nombreMostrado);

        if (ing.isOpcional()) {
            linea.append(" (").append(context.getString(R.string.opcional).toLowerCase(Locale.getDefault())).append(")");
        }

        if (isSustituto) {
            linea.append(" (").append(context.getString(R.string.sustituto_de).toLowerCase(Locale.getDefault()))
                    .append(" ").append(ing.getEsSustitutoDe()).append(")");
        }

        double puntuacion = ing.getPuntuacion();
        if (puntuacion >= 0) {
            linea.append(" (").append(context.getString(R.string.score_label)).append(": ").append(String.format(Locale.getDefault(), "%.1f", puntuacion)).append(")");
        } else if (puntuacion < -1) {
            linea.append(" (").append(context.getString(R.string.score_no_encontrado)).append(")");
        }

        int start = sb.length();
        sb.append(linea.toString());
        int end = sb.length();

        if (ing.getRecetaId() != null && !ing.getRecetaId().isEmpty()) {
            int linkStart = start + prefix.length();
            sb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    navegarAReceta(ing.getRecetaId());
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(context.getResources().getColor(R.color.colorPrimary, context.getTheme()));
                }
            }, linkStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (isSustituto) {
            TextView tmp = new TextView(context);
            float prefixWidth = tmp.getPaint().measureText(prefix);
            sb.setSpan(new LeadingMarginSpan.Standard(0, (int) Math.ceil(prefixWidth)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void navegarAReceta(String recetaId) {
        // Buscar en la lista actual (la que está viendo el usuario, que puede estar filtrada)
        for (int i = 0; i < listaRecetas.size(); i++) {
            if (listaRecetas.get(i).getId().equals(recetaId)) {
                expandableListView.collapseGroup(i);
                expandableListView.expandGroup(i);
                expandableListView.setSelectedGroup(i);
                return;
            }
        }
        
        // Si no está en la lista actual (por filtros), pedir al fragment que limpie filtros y navegue
        if (navigateListener != null) {
            navigateListener.onNavigate(recetaId);
            return;
        }

        Toast.makeText(context, context.getString(R.string.error_receta_no_encontrada), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void updateData(List<Receta> nuevas) {
        mainHandler.post(() -> {
            listaRecetas.clear();
            if (nuevas != null) listaRecetas.addAll(nuevas);
            notifyDataSetChanged();
            if (emptyListListener != null) emptyListListener.reloadList(listaRecetas.size());
        });
    }

    public interface EmptyListListener {
        void reloadList(int count);
    }
}
