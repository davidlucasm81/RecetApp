package com.david.recetapp.adaptadores;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.david.recetapp.R;
import com.david.recetapp.actividades.DetalleDiaActivity;
import com.david.recetapp.negocio.beans.Day;

import java.util.List;

public class CalendarioAdapter extends BaseAdapter {
    private final Context context;
    private final List<Day> days;

    public CalendarioAdapter(Context context, List<Day> days) {
        this.context = context;
        this.days = days;
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public Object getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item_day, parent, false);
        }

        Button dayButton = convertView.findViewById(R.id.dayButton);
        Day day = days.get(position);
        dayButton.setText(String.valueOf(day.getDayOfMonth()));

        // Manejamos el evento onClick
        dayButton.setOnClickListener(v -> {
            // Empezamos la actividad del detalle dia
            Intent intent = new Intent(context, DetalleDiaActivity.class);
            intent.putExtra("selectedDay", day);
            context.startActivity(intent);
        });

        return convertView;
    }
}
