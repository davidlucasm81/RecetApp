package com.david.recetapp.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateDeserializer implements JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) throws JsonParseException {
        String dateStr = json.getAsString();
        if (dateStr == null || dateStr.isEmpty()) return null;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.ENGLISH);
        sdf.setLenient(false);
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new JsonParseException("No se pudo parsear la fecha: " + dateStr, e);
        }
    }
}
