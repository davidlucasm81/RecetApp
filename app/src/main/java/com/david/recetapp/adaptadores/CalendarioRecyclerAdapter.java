package com.david.recetapp.adaptadores;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.actividades.DetalleDiaActivity;
import com.david.recetapp.negocio.beans.Day;
import com.david.recetapp.negocio.beans.RecetaDia;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarioRecyclerAdapter extends ListAdapter<Day, CalendarioRecyclerAdapter.VH> {

    static final DiffUtil.ItemCallback<Day> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Day oldItem, @NonNull Day newItem) {
            return oldItem.getDayOfMonth() == newItem.getDayOfMonth();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Day oldItem, @NonNull Day newItem) {
            return oldItem.equals(newItem);
        }
    };
    private final Context context;
    private final int colorIcono;
    private final int bgDefaultResId = R.drawable.button_background;
    private final int bgTodayResId = R.drawable.button_background_today;
    private final int todayDay;

    public CalendarioRecyclerAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context.getApplicationContext();
        this.colorIcono = ContextCompat.getColor(context, R.color.colorIcono);
        Calendar cal = Calendar.getInstance();
        this.todayDay = cal.get(Calendar.DAY_OF_MONTH);
        setHasStableIds(true);
    }

    /**
     * Envía la lista de días y los huecos en blanco (uso de placeholders no-null)
     */
    public void submitDays(List<Day> days, int numeroEnBlanco) {
        List<Day> fullList = new ArrayList<>();
        for (int i = 0; i < numeroEnBlanco; i++) {
            // placeholder con dayOfMonth negativo para evitar nulls
            fullList.add(new Day(-(i + 1), new ArrayList<>()));
        }
        fullList.addAll(days);
        submitList(fullList);
    }

    @Override
    public long getItemId(int position) {
        Day day = getItem(position);
        if (day == null) return RecyclerView.NO_ID;
        // devolver un id estable; placeholders tienen dayOfMonth negativo
        return (long) day.getDayOfMonth();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Day day = getItem(position);

        if (day == null || day.getDayOfMonth() <= 0) { // placeholder
            holder.dayButton.setText("");
            holder.dayButton.setEnabled(false);
            holder.dayButton.setBackgroundResource(bgDefaultResId);
            holder.dayButton.setBackgroundTintList(null);
            holder.dayButton.setTag(null);
            return;
        }

        holder.dayButton.setText(String.valueOf(day.getDayOfMonth()));
        holder.dayButton.setEnabled(true);
        holder.dayButton.setTag(day);

        boolean isToday = day.getDayOfMonth() == todayDay;
        if (isToday) {
            holder.dayButton.setBackgroundResource(bgTodayResId);
        } else {
            holder.dayButton.setBackgroundResource(bgDefaultResId);
        }
        holder.dayButton.setBackgroundTintList(null);
        holder.dayButton.setTextColor(colorIcono);
    }

    /**
     * ItemDecoration para mantener espacios iguales entre botones
     */
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) outRect.top = spacing;
            }
        }
    }

    class VH extends RecyclerView.ViewHolder {
        final Button dayButton;

        VH(@NonNull View itemView) {
            super(itemView);
            dayButton = itemView.findViewById(R.id.dayButton);

            dayButton.setOnClickListener(v -> {
                Object tag = v.getTag();
                if (!(tag instanceof Day clicked)) return;
                // evitar lanzar actividad para placeholders
                if (clicked.getDayOfMonth() <= 0) return;

                Intent intent = new Intent(context, DetalleDiaActivity.class);
                intent.putExtra("selectedDay", clicked);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        }
    }
}