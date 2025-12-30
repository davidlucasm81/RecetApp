package com.david.recetapp.adaptadores;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

/**
 * RecetasAdapter actualizado para pintar un badge con la puntuación a la derecha del AppCompatButton
 * (sin crear/editar layouts: reutiliza item_receta_button).
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

    /**
     * Crea un BitmapDrawable circular con el número (scoreText) centrado.
     * Color por rango: >=4.0 verde, >=3.0 ámbar, <3.0 rojo.
     */
    private static Drawable createScoreBadgeDrawable(Context ctx, String scoreText, double score) {
        final int sizeDp = 36; // diámetro del badge

        int sizePx = dpToPx(ctx, sizeDp);
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // color del fondo según score
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
        textPaint.setTextSize(spToPx(ctx));
        Rect textBounds = new Rect();
        textPaint.getTextBounds(scoreText, 0, scoreText.length(), textBounds);
        float tx = cx - textBounds.width() / 2f - textBounds.left;
        float ty = cy + textBounds.height() / 2f - textBounds.bottom;
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

    // --- Helpers ---

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    /**
     * Actualiza la lista (manteniendo DiffUtil) y guarda fechas.
     */
    public void updateRecetas(List<Receta> newRecetas, Date fechaElegida, Date fechaIntervaloPrevio) {
        final List<Receta> oldList = new ArrayList<>(this.recetas);
        this.fechaElegida = fechaElegida;
        this.fechaIntervaloPrevio = fechaIntervaloPrevio;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newRecetas != null ? newRecetas.size() : 0;
            }

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

                return oldReceta.getNombre().equals(newReceta.getNombre()) && oldReceta.isPostre() == newReceta.isPostre();
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
            button = (Button) itemView;
            button.setClickable(true);
            button.setFocusable(true);

            // Mejorar layout en runtime: texto a la izquierda, badge a la derecha
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setCompoundDrawablePadding(dpToPx(button.getContext(), 8));
        }

        void bind(final Receta receta) {
            Context ctx = button.getContext();
            button.setText(receta.getNombre());

            // Background logic existente (postre / fecha previa / default)
            if (receta.isPostre()) {
                button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.postre_background));
            } else {
                Date f = receta.getFechaCalendario();
                if (f != null && fechaIntervaloPrevio != null && fechaElegida != null && f.after(fechaIntervaloPrevio) && f.before(fechaElegida)) {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.previous_selected_background));
                } else {
                    button.setBackground(ContextCompat.getDrawable(ctx, R.drawable.edittext_background));
                }
            }

            double score = receta.getPuntuacionDada();


            String scoreText = String.format(Locale.getDefault(), "%.1f", score);
            Drawable badge = createScoreBadgeDrawable(ctx, scoreText, score);
            // Colocar a la derecha
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, badge, null);
            // Accesibilidad
            button.setContentDescription(receta.getNombre() + ", puntuación " + scoreText);


            button.setOnClickListener(v -> {
                if (listener != null) listener.onRecetaClick(receta);
            });
        }
    }
}
