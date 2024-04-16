package com.david.recetapp.adaptadores;

import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.david.recetapp.R;
import com.david.recetapp.negocio.beans.NoteItem;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<NoteItem> notes;
    private OnDeleteClickListener onDeleteClickListener;

    public NoteAdapter(List<NoteItem> notes, OnDeleteClickListener onDeleteClickListener) {
        this.notes = notes;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteItem noteItem = notes.get(position);
        holder.noteText.setText(noteItem.getText());
        holder.checkBox.setChecked(noteItem.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            noteItem.setChecked(isChecked);
            if (isChecked) {
                holder.noteText.setPaintFlags(holder.noteText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.noteText.setPaintFlags(holder.noteText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });

        holder.noteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteItem.setText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return (notes == null)? 0 : notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        public CheckBox checkBox;
        public EditText noteText;
        public ImageButton deleteButton;

        public NoteViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            noteText = itemView.findViewById(R.id.noteText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
}