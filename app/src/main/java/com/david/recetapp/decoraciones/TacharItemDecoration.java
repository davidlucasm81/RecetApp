package com.david.recetapp.decoraciones;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.adaptadores.IngredienteDiaAdapter;

public class TacharItemDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint;
    private final int lineHeight;

    public TacharItemDecoration(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(ContextCompat.getColor(context, R.color.colorGrisTextoFondo));
        lineHeight = context.getResources().getDimensionPixelSize(R.dimen.line_height); // Definir la altura de la línea
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(child);
            if (viewHolder instanceof IngredienteDiaAdapter.IngredienteDiaViewHolder) {
                IngredienteDiaAdapter.IngredienteDiaViewHolder holder = (IngredienteDiaAdapter.IngredienteDiaViewHolder) viewHolder;
                if (holder.checkBoxIngrediente.isChecked()) {
                    int top = child.getTop() + child.getHeight() / 2;
                    int left = child.getLeft();
                    int right = child.getRight();
                    int bottom = top + lineHeight;
                    // Ajustamos la posición de la línea para que se detenga antes del CheckBox
                    int checkBoxWidth = holder.checkBoxIngrediente.getWidth();
                    canvas.drawRect(checkBoxWidth - left, top, right, bottom, paint); // Usamos drawRect para dibujar una línea recta
                }
            }
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.set(0, 0, 0, lineHeight); // Añadir una línea de altura a la parte inferior de cada elemento
    }
}