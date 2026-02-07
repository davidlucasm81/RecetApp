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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.Receta;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * RecetasAdapter corregido:
 *  - Busca el Button por id dentro del itemView (no castea itemView directamente)
 *  - DiffUtil.areContentsTheSame compara puntuación y fecha para forzar rebind cuando cambian
 *  - Mejora centrado del texto en el badge
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
        final int sizeDp = 36; // diámetro del badge
        int sizePx = dpToPx(ctx, sizeDp);
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

        // texto centrado (medición más fiable)
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float textSizePx = spToPx(ctx);
        textPaint.setTextSize(textSizePx);

        float textWidth = textPaint.measureText(scoreText);
        // centrar verticalmente con ascent/descent
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

    private static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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

                // Añadimos puntuación y fecha a la comparación para forzar rebind cuando cambien.
                boolean mismoNombre = Objects.equals(oldReceta.getNombre(), newReceta.getNombre());
                boolean mismoPostre = oldReceta.isPostre() == newReceta.isPostre();
                boolean mismaPuntuacion = Double.compare(oldReceta.getPuntuacionDada(), newReceta.getPuntuacionDada()) == 0;
                boolean mismaFecha = Objects.equals(oldReceta.getFechaCalendario(), newReceta.getFechaCalendario());

                return mismoNombre && mismoPostre && mismaPuntuacion && mismaFecha;
            }
        });

        this.recetas = newRecetas != null ? new ArrayList<>(newRecetas) : new ArrayList<>();
        diffResult.dispatchUpdatesTo(this);
    }

    public interface OnRecetaClickListener {
        void onRecetaClick(Receta receta);
    }

    class RecetaViewHolder extends RecyclerView.ViewHolder {
        private final Button button;

        RecetaViewHolder(View itemView) {
            super(itemView);
            // BUSCAR el Button dentro del layout (más seguro que castear itemView)
            Button b = itemView.findViewById(R.id.buttonReceta);
            if (b == null && itemView instanceof Button) {
                b = (Button) itemView; // fallback si el root es un Button
            }
            button = b;
            if (button == null) {
                throw new IllegalStateException("item_receta_button debe contener un Button con id @+id/buttonReceta o ser un Button como root.");
            }

            button.setClickable(true);
            button.setFocusable(true);

            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setCompoundDrawablePadding(dpToPx(button.getContext(), 8));
        }

        void bind(final Receta receta) {
            Context ctx = button.getContext();
            button.setText(receta.getNombre());

            // Background logic (postre / fecha previa / default)
            if (receta.isPostre()) {
                button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.postre_background));
            } else {
                Date f = receta.getFechaCalendario();
                if (f != null && fechaIntervaloPrevio != null && fechaElegida != null
                        && f.after(fechaIntervaloPrevio) && f.before(fechaElegida)) {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.previous_selected_background));
                } else {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.edittext_background));
                }
            }

            double score = receta.getPuntuacionDada();
            String scoreText = String.format(Locale.getDefault(), "%.1f", score);
            Drawable badge = createScoreBadgeDrawable(ctx, scoreText, score);

            // poner a la derecha y forzar re-paint
            button.setCompoundDrawables(null, null, badge, null);
            button.invalidate();
            button.requestLayout();

            button.setContentDescription(receta.getNombre() + ", puntuación " + scoreText);

            button.setOnClickListener(v -> {
                if (listener != null) listener.onRecetaClick(receta);
            });
        }
    }
}
