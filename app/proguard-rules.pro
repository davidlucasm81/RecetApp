# =============================================
# Reglas para Firestore
# =============================================

# Mantener todas las clases de beans (modelos) y TODOS sus campos y métodos (evita ofuscación de campos privados para Gson/Firestore)
-keep class com.david.recetapp.negocio.beans.** {
    *;
}

# Mantener nombres de campos anotados con SerializedName de Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Mantener enums y sus métodos para Firestore
-keepclassmembers enum com.david.recetapp.negocio.beans.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Mantener Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Mantener constructores públicos para Firestore
-keepclassmembers class com.david.recetapp.negocio.beans.** {
    public <init>();
}

# =============================================
# Reglas generales opcionales de Android
# =============================================

# Evita renombrar clases y métodos utilizados por librerías de Android
# -keep class androidx.** { *; }
# -keep class android.** { *; }

# Evita optimizaciones que rompan reflexiones (por ejemplo, Firestore)
-keepattributes Signature
-keepattributes *Annotation*

# =============================================
# Si tienes utilidades o métodos estáticos que usan reflexión
# =============================================
-keepclassmembers class * {
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

