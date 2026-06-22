package com.david.recetapp.adaptadores;

import android.app.Activity;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import com.david.recetapp.negocio.servicios.UtilsSrv;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecetaExpandableListCalendarAdapter extends BaseExpandableListAdapter {
    private final Day selectedDay;
    private final Activity activity;
    private final ViewGroup anchorContainer;
    private List<Receta> listaRecetas;
    private final ExpandableListView expandableListView;
    private final EmptyListListener emptyListListener;
    private final Handler mainHandler;

    // Singleton del reproductor para toda la lista
    private YouTubePlayerView sharedYouTubePlayerView;
    private YouTubePlayer activePlayer;
    private String currentVideoId;

    public RecetaExpandableListCalendarAdapter(Activity activity, Day selectedDay, 
                                               ExpandableListView expandableListView, 
                                               ViewGroup anchorContainer,
                                               EmptyListListener emptyListListener) {
        this.selectedDay = selectedDay;
        this.activity = activity;
        this.anchorContainer = anchorContainer;
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
                List<Receta> adaptadas = RecetasSrv.getRecetasAdaptadasCalendario(recetas, selectedDay);
                // Ordenar por momento: COMIDA < AMBOS < CENA < Otros
                adaptadas.sort((r1, r2) -> Integer.compare(getMomentoPriority(r1), getMomentoPriority(r2)));
                listaRecetas = adaptadas;
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
        if (groupPosition >= listaRecetas.size()) return 0;
        Receta receta = listaRecetas.get(groupPosition);
        boolean hasYoutube = receta.getYoutubeUrl() != null && !receta.getYoutubeUrl().trim().isEmpty();
        return hasYoutube ? 10 : 9;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listaRecetas.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (groupPosition >= listaRecetas.size()) return null;
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
    public int getChildTypeCount() {
        return 2; // 0: Normal, 1: YouTube
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return (childPosition == 9) ? 1 : 0;
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
        View groupView = convertView;
        if (groupView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            groupView = inflater.inflate(R.layout.list_item_group, parent, false);
        }

        TextView txtTituloReceta = groupView.findViewById(R.id.txtNombreReceta);
        ImageButton btnEliminar = groupView.findViewById(R.id.btnEliminar);

        Receta receta = listaRecetas.get(groupPosition);
        txtTituloReceta.setText(receta.getNombre() != null ? receta.getNombre() : activity.getString(R.string.sin_nombre));

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
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.confirmacion))
                    .setMessage(activity.getString(R.string.alerta_eliminar, receta.getNombre()))
                    .setPositiveButton(activity.getString(R.string.aceptar), (dialog, which) -> {
                        // Eliminar la receta del calendario y refrescar la pantalla
                        // Buscar de forma segura la posición por ID antes de eliminar, ya que la lista
                        // puede haber cambiado entre la creación de la vista y el click.
                        String idToRemove = receta.getId();
                        int indexToRemove = findIndexById(idToRemove);
                        if (indexToRemove == -1) {
                            // No encontramos la receta en la lista actual: informar y cancelar
                            UtilsSrv.notificacion(activity, activity.getString(R.string.error_eliminar_receta), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Receta eliminada = listaRecetas.remove(indexToRemove);
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
                                 // Restaurar la receta si falla (insertar en la misma posición si es posible)
                                 int restoreIndex = Math.max(0, Math.min(indexToRemove, listaRecetas.size()));
                                 listaRecetas.add(restoreIndex, eliminada);
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

        ImageButton btnEditar = groupView.findViewById(R.id.btnEditar);
        btnEditar.setVisibility(View.GONE);

        txtTituloReceta.setOnClickListener(vParam -> {
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
        return groupView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        int type = getChildType(groupPosition, childPosition);
        View view = convertView;
        
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (type == 1) {
                view = inflater.inflate(R.layout.list_item_child_youtube, parent, false);
            } else {
                view = inflater.inflate(R.layout.list_item_child, parent, false);
            }
        }

        TextView txtTitulo = view.findViewById(R.id.txtTitulo);
        TextView txtInformacion = view.findViewById(R.id.txtInformacion);
        Receta receta = listaRecetas.get(groupPosition);

        if (type == 0) {
            // Layout normal
            RatingBar ratingBar = view.findViewById(R.id.ratingBar);
            ratingBar.setVisibility(View.GONE);
            LinearLayout iconosAlergenos = view.findViewById(R.id.iconosAlergenos);
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

                case 1: // Número de personas
                    txtInformacion.setVisibility(View.VISIBLE);
                    txtTitulo.setText(R.string.numero_personas);
                    txtInformacion.setText(String.valueOf(receta.getNumPersonas()));
                    break;

                case 2: // Ingredientes
                    txtTitulo.setText(R.string.ingredientes);
                    List<Ingrediente> ingredientes = receta.getIngredientes();
                    if (ingredientes == null || ingredientes.isEmpty()) {
                        txtInformacion.setText(R.string.sin_ingredientes);
                    } else {
                        StringBuilder sbIngredientes = new StringBuilder();

                        // Agrupar por principal (mismo algoritmo que en el otro adapter)
                        java.util.Map<String, java.util.List<Ingrediente>> grupos = new java.util.LinkedHashMap<>();
                        java.util.List<Ingrediente> principales = new java.util.ArrayList<>();

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
                                java.util.List<Ingrediente> susts = grupos.get(ing.getEsSustitutoDe());
                                Objects.requireNonNullElse(susts, principales).add(ing);
                            }
                        }

                        for (int i = 0; i < principales.size(); i++) {
                            Ingrediente principal = principales.get(i);
                            appendIngredienteInfo(sbIngredientes, principal, false);

                            java.util.List<Ingrediente> sustitutos = grupos.get(principal.getNombre());
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

                case 8: // Momento de la receta
                    txtTitulo.setText(R.string.momento_receta);
                    if (receta.getTipoReceta() == TipoReceta.PRINCIPAL) {
                        txtInformacion.setVisibility(View.VISIBLE);
                        MomentoReceta mr = receta.getMomentoReceta();
                        txtInformacion.setText(mr != null ? activity.getString(mr.getStringRes()) : activity.getString(R.string.ambos));
                    } else {
                        txtInformacion.setVisibility(View.GONE);
                        txtTitulo.setText("");
                    }
                    break;
            }
        } else {
            // Layout YouTube compartido
            FrameLayout placeholder = view.findViewById(R.id.youtube_placeholder);
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
                            activity.startActivity(intent);
                        });
                    }
                }
            }
        }

        return view;
    }

    private void ensureSharedPlayerInitialized() {
        if (sharedYouTubePlayerView == null) {
            sharedYouTubePlayerView = new YouTubePlayerView(activity);
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


    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private int findIndexById(String id) {
        if (id == null || listaRecetas == null) return -1;
        for (int i = 0; i < listaRecetas.size(); i++) {
            Receta r = listaRecetas.get(i);
            if (r != null && id.equals(r.getId())) return i;
        }
        return -1;
    }

    private int getMomentoPriority(Receta r) {
        if (r.getTipoReceta() != TipoReceta.PRINCIPAL) {
            return 10 + r.getTipoReceta().ordinal();
        }
        MomentoReceta mr = r.getMomentoReceta();
        if (mr == null) return 1; // Default to AMBOS
        return switch (mr) {
            case COMIDA -> 0;
            case AMBOS -> 1;
            case CENA -> 2;
        };
    }

    private void appendIngredienteInfo(StringBuilder sb, Ingrediente ing, boolean isSustituto) {
        String nombreMostrado = RecetasSrv.getNombreTraducido(ing.getNombre());
        sb.append(isSustituto ? "  └ " : "- ")
                .append(ing.getCantidad() != null ? ing.getCantidad() : "")
                .append(" ")
                .append(ing.getTipoCantidad() != null ? ing.getTipoCantidad() : "")
                .append(activity.getString(R.string.literal_de))
                .append(nombreMostrado != null ? nombreMostrado : "");

        if (ing.isOpcional()) {
            sb.append(" (").append(activity.getString(R.string.opcional).toLowerCase(java.util.Locale.getDefault())).append(")");
        }

        if (isSustituto) {
            sb.append(" (").append(activity.getString(R.string.sustituto_de).toLowerCase(java.util.Locale.getDefault()))
                    .append(" ").append(ing.getEsSustitutoDe()).append(")");
        }

        double puntuacion = ing.getPuntuacion();
        if (puntuacion >= 0) {
            sb.append(" (").append(activity.getString(R.string.score_label)).append(": ").append(puntuacion).append(")");
        } else if (puntuacion != -1) {
            sb.append(" (").append(activity.getString(R.string.score_no_encontrado)).append(")");
        }
    }

    public interface EmptyListListener {
        void onListEmpty();
        void onListSize();
    }
}