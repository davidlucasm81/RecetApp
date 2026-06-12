package com.david.recetapp.adaptadores;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Receta;
import com.david.recetapp.negocio.beans.TipoReceta;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * RecetasAdapter optimizado para AddRecetaDiaActivity:
 *  - Muestra iconos en lugar de cambiar colores de fondo.
 *  - Soporta icono de reloj para recetas recientes.
 *  - Coherencia visual con RecetaExpandableListAdapter.
 */
public class RecetasAdapter extends RecyclerView.Adapter<RecetasAdapter.RecetaViewHolder> {
    private final OnRecetaClickListener listener;
    private List<Receta> recetas;
    private Date fechaElegida;
    private Date fechaIntervaloPrevio;

    public RecetasAdapter(List<Receta> recetas, OnRecetaClickListener listener) {
        this.recetas = recetas != null ? new ArrayList<>(recetas) : new ArrayList<>();
        this.listener = listener;
    }

    private static Drawable createScoreBadgeDrawable(Context ctx, String scoreText, double score) {
        int sizePx = dpToPx(ctx);
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // fondo
        int bgColor = colorByScore(score);
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(bgColor);
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;
        float radius = sizePx / 2f;
        canvas.drawCircle(cx, cy, radius, circlePaint);

        // texto centrado
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float textSizePx = spToPx(ctx);
        textPaint.setTextSize(textSizePx);

        float textWidth = textPaint.measureText(scoreText);
        float textHeightOffset = (textPaint.descent() + textPaint.ascent()) / 2f;

        float tx = cx - (textWidth / 2f);
        float ty = cy - textHeightOffset;

        canvas.drawText(scoreText, tx, ty, textPaint);

        BitmapDrawable drawable = new BitmapDrawable(ctx.getResources(), bmp);
        drawable.setBounds(0, 0, sizePx, sizePx);
        return drawable;
    }

    private static int colorByScore(double score) {
        if (score >= 8.1) {
            return Color.parseColor("#4CAF50"); // verde
        } else if (score >= 6.1) {
            return Color.parseColor("#8BC34A"); // verde medio
        } else if (score >= 3.1) {
            return Color.parseColor("#FFC107"); // ámbar/naranja
        } else {
            return Color.parseColor("#F44336"); // rojo
        }
    }

    private static int dpToPx(Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(36 * density);
    }

    private static int spToPx(Context ctx) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                12,
                ctx.getResources().getDisplayMetrics()
        ));
    }

    @NonNull
    @Override
    public RecetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receta_button, parent, false);
        return new RecetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaViewHolder holder, int position) {
        holder.bind(recetas.get(position));
    }

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    public void updateRecetas(List<Receta> newRecetas, Date fechaElegida, Date fechaIntervaloPrevio) {
        final List<Receta> oldList = new ArrayList<>(this.recetas);
        this.fechaElegida = fechaElegida;
        this.fechaIntervaloPrevio = fechaIntervaloPrevio;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return newRecetas != null ? newRecetas.size() : 0; }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition >= oldList.size() || newItemPosition >= newRecetas.size())
                    return false;
                String oldId = oldList.get(oldItemPosition).getId();
                String newId = newRecetas.get(newItemPosition).getId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Receta oldReceta = oldList.get(oldItemPosition);
                Receta newReceta = newRecetas.get(newItemPosition);

                boolean mismoNombre = Objects.equals(oldReceta.getNombre(), newReceta.getNombre());
                boolean mismoTipo = oldReceta.getTipoReceta() == newReceta.getTipoReceta();
                boolean mismaPuntuacion = Double.compare(oldReceta.getPuntuacionDada(), newReceta.getPuntuacionDada()) == 0;
                boolean mismaFecha = Objects.equals(oldReceta.getFechaCalendario(), newReceta.getFechaCalendario());

                return mismoNombre && mismoTipo && mismaPuntuacion && mismaFecha;
            }
        });

        this.recetas = newRecetas != null ? new ArrayList<>(newRecetas) : new ArrayList<>();
        diffResult.dispatchUpdatesTo(this);
    }

    public interface OnRecetaClickListener {
        void onRecetaClick(Receta receta);
    }

    class RecetaViewHolder extends RecyclerView.ViewHolder {
        private final View layout;
        private final TextView txtNombre;
        private final ImageView imgRecent;
        private final ImageView imgType;
        private final ImageView imgWarning;
        private final ImageView imgBadge;

        RecetaViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layoutReceta);
            txtNombre = itemView.findViewById(R.id.txtNombreReceta);
            imgRecent = itemView.findViewById(R.id.imgRecent);
            imgType = itemView.findViewById(R.id.imgType);
            imgWarning = itemView.findViewById(R.id.imgWarning);
            imgBadge = itemView.findViewById(R.id.imgBadge);
        }

        void bind(final Receta receta) {
            Context ctx = itemView.getContext();
            txtNombre.setText(receta.getNombre());

            // 1. Recientemente añadida (reloj)
            Date f = receta.getFechaCalendario();
            boolean esReciente = f != null && fechaIntervaloPrevio != null && fechaElegida != null
                    && f.after(fechaIntervaloPrevio) && f.before(fechaElegida);
            imgRecent.setVisibility(esReciente ? View.VISIBLE : View.GONE);

            // 2. Tipo de receta
            if (receta.getTipoReceta() == TipoReceta.POSTRE) {
                imgType.setImageResource(R.drawable.postre_icono);
                imgType.setVisibility(View.VISIBLE);
            } else if (receta.getTipoReceta() == TipoReceta.COCTEL) {
                imgType.setImageResource(R.drawable.ic_baseline_local_bar_24);
                imgType.setVisibility(View.VISIBLE);
            } else if (receta.getTipoReceta() == TipoReceta.SIDE) {
                imgType.setImageResource(R.drawable.ic_baguette);
                imgType.setVisibility(View.VISIBLE);
            } else {
                imgType.setVisibility(View.GONE);
            }

            // 3. Warning (ingredientes mal puntuados o personas <= 0)
            boolean hasBadScore = receta.getIngredientes() != null &&
                    receta.getIngredientes().stream().anyMatch(i -> i.getPuntuacion() < -1);
            boolean invalidPersons = receta.getNumPersonas() <= 0;
            imgWarning.setVisibility((invalidPersons || hasBadScore) ? View.VISIBLE : View.GONE);

            // 4. Score Badge
            double score = receta.getPuntuacionDada();
            String scoreText = String.format(Locale.getDefault(), "%.1f", score);
            Drawable badge = createScoreBadgeDrawable(ctx, scoreText, score);
            imgBadge.setImageDrawable(badge);

            layout.setOnClickListener(v -> {
                if (listener != null) listener.onRecetaClick(receta);
            });
            
            layout.setContentDescription(receta.getNombre() + ", " + ctx.getString(R.string.puntuacion) + " " + scoreText);
        }
    }
}
