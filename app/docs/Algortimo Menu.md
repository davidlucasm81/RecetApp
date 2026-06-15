# Algoritmo `addMenu` — CalendarioSrv (v6 — Ultra Rendimiento)

> **Propósito:** Rellena un rango de días del calendario añadiendo recetas automáticamente (no borra las existentes), generando un menú equilibrado siguiendo criterios de dieta mediterránea, con prioridad real de salud, ventana móvil semanal, cenas más ligeras y sinergia de ingredientes. Esta versión introduce optimizaciones de bitmasks (M23), pre-particionado de recetas (M24) y sinergia con decaimiento temporal (M19+).

---

## Firma del método

```java
public static void addMenu(
    Context context,          // Contexto Android (para UI, recursos, etc.)
    int mes,                  // Mes objetivo (0-indexed, estilo Calendar)
    int anio,                 // Año objetivo
    int diaInicio,            // Primer día del rango a rellenar (inclusive)
    int diaFin,               // Último día del rango a rellenar (inclusive)
    boolean forzarPasados,    // Si true, también rellena días anteriores al actual
    int numRecetas,           // Número de recetas a asignar por día
    int numPersonas,          // Número de personas (raciones/porciones)
    RellenarCallback callback // Callback asíncrono con resultado
)
```

---

## Flujo principal (paso a paso)

```
addMenu()
│
├─ 1. Validación de userId
├─ 2. CARGA PARALELA (M5) + TIMEOUT (M16)
├─ 3. Comprobación de Rango Completo (M14)
└─ 4. [BACKGROUND THREAD] Generación del menú
       ├─ 4a. OBTENCIÓN DE CACHE GLOBAL (M17+: Bitmasks pre-calculados)
       ├─ 4b. PRE-PARTICIONADO DE RECETAS (M24: Comida/Cena/Ambos por temporada)
       ├─ 4c. CARGA VENTANA PREVIA EFICIENTE (M13)
       ├─ 4d. INICIALIZACIÓN VENTANA ESTADÍSTICAS (M20: WindowStats con Bitmasks)
       ├─ 4e. Iteración por días
       │       ├─ 4f. ACTUALIZACIÓN VENTANA DESLIZANTE (M20: Restar día d-7)
       │       └─ 4g. addReceta() × numRecetas
       │               ├─ 4h. Selección de lista particionada (M24: Comida vs Cena)
       │               ├─ 4i. CONFLICTO NUTRICIONAL INTRADIARIO (M21: Bitmask check)
       │               └─ 4j. obtenerRecetaNoRepetida() [Optimizado M23]
       │                       └─ Aplicación de SINERGIA CON DECAIMIENTO (M19+: Bonus temporal)
       ├─ 4k. Guardado en Firebase (batch)
```

---

## Detalle de la Lógica de Selección

### 1. Niveles de Filtrado (Hard Constraints)
- **No repetición inmediata:** No haber sido usada en el proceso de rellenado actual.
- **Ventana de frecuencia (M9/M23):** Operaciones bitmask O(1) para Carne Roja, Legumbres y Pasta/Arroz.
- **Balance Nutricional Intradiario (M21/M23):** Uso de flag `isPesada` pre-calculado.
- **Momento adecuado (M24):** Listas pre-particionadas por momento (COMIDA/CENA/AMBOS).
- **Límite de repetición adaptativo (M15):** No haber sido planificada en los últimos `N` días.
- **Sinergia Diaria:** No mezclar Pasta y Legumbre en el mismo día natural (Check Bitmask).

### 2. Sistema de Pesos (Soft Constraints)
- **Salud (M3/M11):** Puntuación normalizada pre-calculada.
- **Estrellas:** Desempate técnico.
- **Sinergia con Decaimiento (M19+):** 
    - Bonus mayor por ingredientes frescos (Verduras, Lácteos).
    - El bonus decae linealmente según la antigüedad (máximo efecto si se usó ayer).
- **Priorización mediterránea:** Multiplicadores dinámicos (x3) si faltan grupos críticos (Pescado, Legumbre) en la ventana de 7 días.
- **Ligereza en Cenas (M11/M23):** Penalización automática si `isDensa` es true (basado en salud e ingredientes).

---

### Mejoras Clave (v6)

#### M23 — Bitmasks de Tipos de Ingredientes
Sustitución de `Set<TipoIngrediente>` por un `long` bitmask en `CachedRecetaData`. Esto reduce la carga del recolector de basura (GC) y permite comprobaciones nutricionales en nanosegundos.

#### M24 — Pre-particionado de Recetas
Las recetas se clasifican por momento de consumo y temporada una sola vez al inicio del proceso. Esto elimina el filtrado redundante de cientos de recetas en cada slot diario.

#### M19+ — Sinergia con Decaimiento Temporal
Fomenta el aprovechamiento de productos frescos (ej. si ayer usaste medio pimiento, hoy el sistema prioriza recetas con pimiento con un peso mayor que si lo hubieras usado hace 5 días).

---

## Resumen de Estado de Mejoras

 ID | Descripción breve | Estado |
---|---|---|
 M1–M16 | Mejoras de versiones anteriores | ✅ |
 M17+ | Cache Global de Pre-procesado | ✅ |
 M20 | Ventana deslizante (Estadísticas) | ✅ |
 M21 | Balance nutricional intradiario | ✅ |
 M22 | Optimización de bucle de reintentos | ✅ |
 M23 | Bitmasks de Tipos de Ingredientes | ✅ |
 M24 | Pre-particionado de Recetas | ✅ |
