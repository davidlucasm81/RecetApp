# Contexto para Agentes de IA - RecetApp

Este documento proporciona el contexto necesario para que los agentes de IA comprendan la arquitectura, el flujo de datos y las decisiones de diseño de **RecetApp**.

## 1. Qué es esta aplicación
**RecetApp** es un gestor personalizado de recetas y planificador de comidas para Android. Su objetivo principal es facilitar la gestión de recetas propias y la creación de menús semanales equilibrados basados en los principios de la **Dieta Mediterránea**. Incluye integración con YouTube para ver vídeos de cocina, un sistema de puntuación de salud para las recetas y generación automática de listas de la compra.

## 2. Stack tecnológico
- **Lenguaje**: Java (Java 21).
- **Plataforma**: Android (Min SDK: 34, Target SDK: 37).
- **UI**: Layouts XML tradicionales con Material Components (Material 3).
- **Backend/Base de Datos**: Firebase (Cloud Firestore y Firebase Authentication).
- **Integraciones**: 
  - `YouTube Player API`: Reproducción de vídeos de recetas.
  - `Glide`: Carga de imágenes.
  - `Google ID / Credentials Manager`: Autenticación segura.
- **IA**: Integración manual mediante plantillas de prompts y pegado de JSON (flujo Prompt -> IA Externa -> JSON -> App).
- **i18n**: Soporte para Español (`values`) e Inglés (`values-en`). Los ingredientes en `ingredient_list.xml` deben incluir ambos idiomas.
- **Testing**: Actualmente, la aplicación no cuenta con tests automatizados (unitarios o instrumentados). No hay dependencias de testing configuradas en `build.gradle` ni directorios de tests en `src`. Las validaciones se realizan de forma manual y ad-hoc, sin checklists o planes de prueba formalizados por el momento.

### 2.1 Dependencias y configuración de build
Para asegurar la consistencia del entorno, se utilizan las siguientes versiones y configuraciones:

- **Versiones de Librerías**:
    - `Firebase BOM`: `34.15.0` (gestiona Firestore y Auth).
    - `YouTube Player API (Pierfrancesco Soffritti)`: `13.0.0`.
    - `Glide`: `4.16.0`.
    - `Material Components`: `1.14.0`.
    - `AndroidX AppCompat`: `1.7.1`.
    - `Credential Manager`: `1.6.0`.
- **Configuración de Android**:
    - `Compile SDK`: `37`.
    - `Target SDK`: `37`.
    - `Min SDK`: `34`.
    - `Java Toolchain`: `Java 21`.
- **Build Types**:
    - `debug`: Sin minificación.
    - `release`: Minificación (`minifyEnabled`) y reducción de recursos (`shrinkResources`) activados.
- **Flavors**: No se utilizan product flavors en la configuración actual.

## 3. Estructura de paquetes
La aplicación sigue una arquitectura de capas simple pero efectiva (Service-Bean):

- `com.david.recetapp.actividades`: Actividades de la interfaz de usuario (Login, Main, Detalle, etc.).
- `com.david.recetapp.fragments`: Fragmentos que componen la interfaz principal (Recetas, Calendario, Lista de Compra).
- `com.david.recetapp.adaptadores`: Adaptadores para RecyclerViews, ListViews y ExpandableListViews:
  - `RecetaExpandableListAdapter`: Gestiona la lista principal de recetas con soporte para vídeos de YouTube.
  - `RecetaExpandableListCalendarAdapter`: Muestra recetas adaptadas en la vista de detalle de un día.
  - `CalendarioRecyclerAdapter`: Adaptador para el grid mensual del calendario.
  - `RecetasAdapter`: Adaptador simple para listados lineales de recetas.
- `com.david.recetapp.negocio.beans`: Modelos de datos (POJOs) como `Receta`, `Day`, `Ingrediente`.
- `com.david.recetapp.negocio.servicios`: Lógica de negocio y persistencia:
  - `FirebaseManager`: Gestor central de operaciones con Firestore y caché.
  - `RecetasSrv`: Lógica específica de recetas (puntuación, filtrado).
  - `CalendarioSrv`: Algoritmo de generación de menús y gestión del calendario.
  - `UtilsSrv`: Utilidades comunes de UI y conversiones.

### 3.1 Flujo de navegación
La navegación en la aplicación se basa en un esquema de Actividad Principal (`MainActivity`) que gestiona fragmentos para las secciones principales, y actividades secundarias para tareas específicas (CRUD, detalles).

#### Pantalla de Entrada (Login)
- **`LoginActivity`**: Es la actividad de lanzamiento (`LAUNCHER`). Gestiona la autenticación con Google mediante `CredentialManager`.
- **Transición**: Tras el login exitoso (o si ya existe sesión), se navega a `MainActivity` utilizando `Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK` para limpiar la pila de actividades.

#### Pantalla Principal (MainActivity)
Gestiona tres fragmentos principales mediante `FragmentTransaction.replace()` en un contenedor central (`fragmentContainer`):
1.  **`RecetasFragment`**: Lista de recetas del usuario.
2.  **`CalendarioFragment`**: Planificador semanal/mensual.
3.  **`ListaCompraFragment`**: Lista de ingredientes agregada.

#### Flujo de Recetas
- **Creación**: `RecetasFragment` → `AddRecetaActivity` (vía FAB).
- **Edición**: `RecetasFragment` → `EditarRecetaActivity` (desde el botón de edición en `RecetaExpandableListAdapter`).
- **Importación/IA**:
    - `RecetasFragment` → `ImportExportActivity` (JSON).
    - `RecetasFragment` → `IAInputActivity` → `AnadirRecetaIAActivity` (IA).

#### Flujo de Calendario
- **Detalle del día**: `CalendarioFragment` → `DetalleDiaActivity`. Se pasa el objeto `Day` seleccionado como `Serializable` en el Intent.
- **Añadir receta al día**: `DetalleDiaActivity` → `AddRecetaDiaActivity`. Se lanza mediante `ActivityResultLauncher` para refrescar la UI al volver.
- **Retorno**: Al finalizar la edición o añadir una receta, se usa `setResult(RESULT_OK)` y `finish()`. `DetalleDiaActivity` informa a `MainActivity` del día actualizado mediante extras en el resultado.

#### Mecanismos de paso de datos
1.  **Intent Extras**: Uso extensivo de `Serializable` para pasar beans completos (`Receta`, `Day`, `List<Receta>`).
2.  **ActivityResultLauncher**: Utilizado para sincronizar cambios entre actividades (ej. refrescar el calendario tras añadir una receta).
3.  **Servicios Estáticos (Srv)**: `RecetasSrv` y `CalendarioSrv` mantienen el estado global (`userId`, listas en caché) al que acceden tanto actividades como fragmentos.
4.  **No se usa ViewModel**: La aplicación sigue un patrón Service-Bean donde los servicios actúan como controladores de datos y estado.

## 4. Modelo de datos en Firestore
Los datos se almacenan en tres colecciones principales, siempre filtrados por `userId`:

- `recetas`: Contiene documentos con metadatos de la receta, lista de ingredientes (con cantidades y tipos), pasos de preparación, alérgenos y URL de YouTube.
    - **Temporadas permitidas**: `PRIMAVERA`, `VERANO`, `OTONIO`, `INVIERNO`.
    - **Alérgenos permitidos**: `Gluten` (0), `Lácteos` (1), `Frutos secos` (2), `Soja` (3), `Pescado` (4), `Mariscos` (5), `Huevos` (6).
- `calendario`: Documentos por mes/año (`uid_año_mes`). Contienen un array de objetos `Day`, cada uno con una lista de `RecetaDia`.
    ### Detalle de RecetaDia (Escalado de Ingredientes)
    Cada objeto `RecetaDia` representa una instancia de una receta en un día específico. Las cantidades se recalculan dinámicamente mediante `RecetasSrv.getRecetasAdaptadasCalendario` siguiendo estas reglas:
    - **Fórmula de Escalado**: $Q_{nueva} = Q_{original} \cdot \frac{P_{objetivo}}{P_{original}}$
        - **$Q_{original}$**: Cantidad definida en la receta base (puede ser fracción "1/2" o decimal "0.5").
        - **$P_{objetivo}$**: Número de personas indicado en `RecetaDia` (`numeroPersonas`).
        - **$P_{original}$**: Número de personas para las que se diseñó la receta base (`numPersonas`).
    - **Redondeo y Formato**:
        - Se utiliza `RoundingMode.HALF_UP` con **2 decimales**.
        - Se eliminan los ceros a la derecha innecesarios (ej: `1.50` $\rightarrow$ `1.5`).
    - **Casos Límite**:
        - **División por cero**: Si la receta original tiene 0 personas, el factor de escalado es `1.0`.
        - **Datos Inválidos**: Si la cantidad original no es numérica ni una fracción válida, `UtilsSrv.convertirNumero` devuelve `-1.0`, lo que resultará en una cantidad negativa visible en la UI.
        - **Fracciones**: Se soportan formatos tipo `numerador/denominador`. Si el denominador es 0, se trata como error (`-1.0`).
    - **Gestión de Sustituciones**:
        - `RecetaDia` almacena en `ingredientesElegidos` qué variante de ingrediente ha escogido el usuario (ej: "Leche de Avena" en lugar de "Leche").
        - Solo se escala y mantiene el ingrediente seleccionado (o el principal si no hay elección). Los demás ingredientes del mismo grupo de sustitución se descartan en la adaptación para el calendario.
- `ingredientes_usuario`: Puntuaciones personalizadas que el usuario asigna a ingredientes específicos para influir en el algoritmo de salud.

### 4.1 Definición de beans

La aplicación utiliza varios POJOs (Beans) para representar los datos, diseñados para ser compatibles con Firebase Firestore (requieren constructor vacío y getters/setters).

#### Receta
Representa la entidad base de una receta.
- `id` (**String**): Identificador único (document ID en Firestore).
- `nombre` (**String**): Nombre de la receta. (Default: `""`)
- `temporadas` (**List<Temporada>**): Lista de temporadas recomendadas. (Default: lista vacía)
- `ingredientes` (**List<Ingrediente>**): Lista de ingredientes necesarios. (Default: lista vacía)
- `pasos` (**List<Paso>**): Lista ordenada de pasos de preparación. (Default: lista vacía)
- `alergenos` (**List<Alergeno>**): Alérgenos presentes en la receta. (Default: lista vacía)
- `estrellas` (**float**): Puntuación de 0 a 5 dada por el usuario. (Default: `0f`)
- `numPersonas` (**int**): Número de raciones base para las cantidades indicadas. (Default: `-1`)
- `fechaCalendario` (**Date**, *opcional*): Fecha en la que se cocinó por última vez. (Default: `null`)
- `shared` (**boolean**): Indica si la receta es pública/compartida. (Default: `false`)
- `tipoReceta` (**TipoReceta**): Categoría (`PRINCIPAL`, `POSTRE`, `COCTEL`, `SIDE`). (Default: `PRINCIPAL`)
- `puntuacionDada` (**double**): Nota de salud calculada (0-10). (Default: `-1`)
- `userId` (**String**): ID del propietario de la receta.
- `momentoReceta` (**MomentoReceta**): Momento ideal de consumo (`COMIDA`, `CENA`, `AMBOS`). (Default: `AMBOS`)
- `youtubeUrl` (**String**): URL del vídeo de preparación. (Default: `""`)

#### Day
Representa un día en la colección `calendario`.
- `dayOfMonth` (**int**): Día del mes.
- `month` (**int**): Mes (0-11).
- `year` (**int**): Año.
- `recetas` (**List<RecetaDia>**): Lista de recetas planificadas para ese día (comida y cena).

#### Ingrediente
Componente individual de una receta.
- `nombre` (**String**): Nombre descriptivo del ingrediente.
- `cantidad` (**String**): Cantidad en formato numérico ("150"), decimal ("0.5") o fracción ("1/2").
- `tipoCantidad` (**String**): Unidad de medida (ej: "g", "ml", "unidad", "taza").
- `puntuacion` (**double**): Nota de salud individual (0-10).
- `opcional` (**boolean**): Indica si se puede omitir. (Default: `false`)
- `esSustitutoDe` (**String**, *opcional*): Nombre del ingrediente principal al que este puede sustituir.
- `tipo` (**TipoIngrediente**): Categoría nutricional para el algoritmo de salud.
- `recetaId` (**String**, *opcional*): ID de otra receta si este ingrediente es una elaboración compuesta.

#### RecetaDia
Instancia de una receta asignada a un día específico en el calendario.
- `idReceta` (**String**): Referencia a la receta base.
- `numeroPersonas` (**int**): Raciones a cocinar para este día específico (usado para el escalado).
- `ingredientesElegidos` (**Map<String, String>**): Mapeo de elección de sustitutos (NombreBase -> NombreElegido).

#### Paso
Instrucción individual de preparación.
- `paso` (**String**): Descripción de la acción.
- `tiempo` (**String**): Duración estimada en formato "HH:mm".

#### Alergeno
- `nombre` (**String**): Nombre del alérgeno (ej: "Gluten").
- `numero` (**int**): Código identificador.

#### IngredienteUsuario
Preferencia personalizada de salud para un ingrediente.
- `nombre` (**String**): Nombre del ingrediente (normalizado).
- `puntuacion` (**int**): Puntuación de salud asignada por el usuario (0-10).

---

### 4.2 Enums de Negocio
- **Temporada**: `PRIMAVERA`, `VERANO`, `OTONIO`, `INVIERNO`.
- **Alergeno (Lista cerrada)**: `Gluten` (0), `Lácteos` (1), `Frutos secos` (2), `Soja` (3), `Pescado` (4), `Mariscos` (5), `Huevos` (6).
- **TipoReceta**: `PRINCIPAL`, `POSTRE`, `COCTEL`, `SIDE`.
- **MomentoReceta**: `COMIDA`, `CENA`, `AMBOS`.
- **TipoIngrediente**: Ver mapeo de bits en la Sección 9.

## 5. Gestión de credenciales y API keys
**REQUISITO CRÍTICO**: Nunca publicar claves privadas o credenciales al repositorio.
- **Firebase**: Se configura mediante `google-services.json` (excluido del VCS).
- **Web Client ID**: Se almacena en `local.properties` como `web.client.id` y se inyecta en `BuildConfig` mediante Gradle.
- **Firmado**: Las claves de release se gestionan localmente en la carpeta `app/keystore`.

## 6. Requisitos funcionales
- **Autenticación**: Login obligatorio con Google para sincronizar datos.
- **Gestión de Recetas**: CRUD completo de recetas con soporte para alérgenos y temporadas.
- **Planificación de Menús**: Generación automática de menús (botón "Rellenar") que aplica un algoritmo de salud mediterránea basado en restricciones y pesos:
    1.  **Restricciones Críticas (Hard Constraints)**: Descarte inmediato de recetas si violan:
        - **Recencia**: No repetir la misma receta en 14 días (configurable mediante fallback progresivo).
        - **Frecuencia Semanal**: Máximo 1 carne roja/procesada, 2 legumbres y 2 pasta/arroz por semana.
        - **Balance Diario**: No dos platos "pesados" el mismo día, ni combinar pasta con legumbre.
        - **Variedad**: Máximo 1 coincidencia de ingredientes significativos entre platos del mismo día.
    2.  **Puntuación Dinámica (Pesos)**: Multiplicadores que favorecen o penalizan recetas:
        - **Déficit Nutricional**: x3.0 si faltan legumbres (<2) o pescado (<3) en la semana; x2.0 si falta verdura hoy.
        - **Exceso**: x0.5 si hay exceso de carne blanca (>=3) o huevos (>=2).
        - **Momento**: Cenas ligeras x1.25, cenas densas x0.75.
        - **Modo Vago**: Prioriza recetas rápidas según el tiempo total (suma de `tiempo` de todos los `pasos`).
            - **<= 20 min**: x3.5
            - **<= 40 min**: x2.0
            - **> 60 min**: x0.1 (penalización fuerte).
            - También aplica un bonus **x1.3** si `healthNorm > 0.7`.
        - **Sinergia Temporal**: Bonus por reaprovechar ingredientes frescos usados en los últimos 7 días.
            - **Bonus Base**: +15% por ingrediente compartido si es Verdura/Lácteo; +5% para otros tipos.
            - **Decaimiento**: El bonus se reduce un 10% por cada día de antigüedad (1.0 ayer -> 0.4 hace una semana).
            - **Cálculo**: `score *= (1.0 + (min(2, coincidencias) * bonusBase * factorTemporal))`.
    3.  **Prioridad y Resolución (Pipeline)**: El sistema aplica los filtros en un orden secuencial estricto:
        - **Fase 1: Temporada y Tipo (Pre-filtro)**: La receta debe ser del tipo adecuado (`PRINCIPAL` para comida/cena) y pertenecer a la temporada del mes.
        - **Fase 2: Momento (MomentoReceta)**: Filtro por `MomentoReceta` (Comida/Cena). Si no hay candidatos, este es el primer filtro que se relaja (se aceptan recetas "AMBOS" o del otro momento).
        - **Fase 3: Recencia de Sesión**: Evita repetir recetas que ya han sido asignadas en la misma ejecución del "Rellenar" (memoria volátil).
        - **Fase 4: Recencia Histórica (Relajación Progresiva)**: Se comprueba la última vez que se cocinó. El límite empieza en **14 días** y baja progresivamente (10, 7, 3, 0) si no hay candidatos.
        - **Fase 5: Frecuencia Semanal (Ventana Deslizante)**: Máximo 1 carne roja/procesada, 2 legumbres y 2 pasta/arroz por semana.
        - **Fase 6: Variedad (Similitud)**: Máximo 1 coincidencia de **ingredientes significativos** entre platos del mismo día.
            - **Ingrediente Significativo**: Ingredientes que no son opcionales y no son "base" (ajo, cebolla, tomate, aceite, sal, pimienta).
        - **Fase 7: Balance Diario (Intradiario)**: No permitir dos platos "pesados" el mismo día, ni combinar pasta con legumbre.
        - **Selección**: Ruleta ponderada entre los candidatos finales.
        - **Fallo de Generación**: Si después de todos los intentos y relajaciones de filtros no se encuentra ninguna receta para un slot específico (Comida/Cena/Side), el slot permanecerá vacío en el calendario para ese día. El sistema no aplica un fallback de "receta aleatoria total" para garantizar que no se violen las restricciones críticas.
- **Lista de la Compra**: Agregación de ingredientes de un rango de días del calendario, con consolidación de unidades y normalización de nombres.
    ### Lógica de Consolidación
    La generación de la lista (en `CalendarioSrv.getListaCompra`) sigue estos pasos:
    1.  **Agregación Inicial**: Se recorren las recetas del rango de fechas y se agrupan los ingredientes por su nombre normalizado (trim y minúsculas).
    2.  **Detección de Conflictos**: Si un mismo ingrediente tiene múltiples unidades de medida (ej: "200 g" y "1 kg"), se activa el motor de conversión.
    3.  **Conversión a Gramos/Mililitros**: Para consolidar, se intenta llevar todo a una unidad base ("g" o "ml") usando:
        - **Factores Fijos**: `kg` $\rightarrow$ 1000, `l` $\rightarrow$ 1000.
        - **Gramos por Unidad**: Si la unidad es "unidad", se busca el peso en gramos definido para ese ingrediente en `ingredient_list.xml` (ej: 1 huevo = 60g).
        - **Mapa de Importancia**: Para unidades de volumen (taza, cucharada), se utiliza el peso definido en `R.array.importance_values`.
        > [!NOTE]
        > Este mapa debe mantenerse sincronizado con los pesos utilizados en el **Sistema de puntuación de salud** (Sección 9). Ambos representan la misma escala de conversión a gramos/mililitros.
    4.  **Simplificación**: Una vez sumados todos los valores en la unidad base, se muestra el total simplificado eliminando ceros decimales innecesarios. Los nombres se muestran con la primera letra en mayúscula para la UI.
- **Importación IA**: Generador de prompts que copia al portapapeles una estructura para que el usuario obtenga un JSON de una IA externa y lo importe directamente.

### 6.1 Esquema JSON de importación
La aplicación espera un objeto JSON con la siguiente estructura y tipos de datos. El sistema es tolerante a fallos y utiliza valores por defecto si faltan campos opcionales.

```json
{
  "nombre": "Nombre de la receta",
  "tipoReceta": "PRINCIPAL",
  "momentoReceta": "AMBOS",
  "numPersonas": 4,
  "temporadas": ["INVIERNO", "PRIMAVERA"],
  "alergenos": ["Gluten", "Lácteos"],
  "youtubeUrl": "https://www.youtube.com/watch?v=...",
  "ingredientes": [
    {
      "nombre": "Harina de trigo",
      "cantidad": "500",
      "tipoCantidad": "g",
      "puntuacion": 5.0,
      "opcional": false,
      "esSustitutoDe": null
    }
  ],
  "pasos": [
    {
      "paso": "Mezclar los ingredientes secos",
      "tiempo": "00:05"
    }
  ]
}
```

#### Detalles del Esquema:
- **`nombre`** (String): Obligatorio.
- **`tipoReceta`** (Enum): `PRINCIPAL`, `POSTRE`, `COCTEL`, `SIDE`. (Default: `PRINCIPAL`)
- **`momentoReceta`** (Enum): `COMIDA`, `CENA`, `AMBOS`. (Default: `AMBOS`)
- **`numPersonas`** (int): Número de raciones base. (Default: `4`)
- **`temporadas`** (Array): Lista de `INVIERNO`, `VERANO`, `OTONIO`, `PRIMAVERA`.
- **`alergenos`** (Array): Nombres de alérgenos que deben coincidir con los definidos en la app.
- **`ingredientes`** (Array):
    - `nombre` (String): Obligatorio.
    - `cantidad` (String): Puede ser numérico ("150") o fracción ("1/2"). (Default: `"1"`)
    - `tipoCantidad` (String): Unidad de medida (ej: "g", "ml", "unidad"). (Default: `"unidad"`)
    - `puntuacion` (double): Nota de salud 0-10. (Default: `-2` para forzar recalculo)
    - `opcional` (boolean): (Default: `false`)
    - `esSustitutoDe` (String): Nombre del ingrediente base al que sustituye.
- **`pasos`** (Array):
    - `paso` (String): Descripción de la instrucción.
    - `tiempo` (String): Formato "HH:mm". (Default: `"00:00"`)
- **`youtubeUrl`** (String): URL opcional del vídeo.

## 7. Convenciones de código
- **Nomenclatura**: Sufijos claros (`Activity`, `Fragment`, `Srv`, `Adapter`).
- **Arquitectura**: Toda lógica de acceso a datos debe pasar por un `Srv` y usar `FirebaseManager`.
- **UI**: Uso de `UtilsSrv.notificacion()` para mensajes al usuario.
- **Rendimiento**: Estrategia *Offline-First*. Se prefiere siempre la carga desde la caché de Firestore antes que desde el servidor.
- **Manejo de errores y offline-first**: La aplicación utiliza una robusta estrategia para garantizar la disponibilidad de datos y la integridad en las escrituras:
    - **Caché Persistente**: Firestore está configurado con caché ilimitada (`PersistentCacheSettings`). Esto permite que la aplicación funcione completamente offline; las consultas primero intentan leer de `Source.CACHE` y las escrituras se encolan localmente hasta recuperar la conexión.
    - **Atomicidad (WriteBatch)**: Operaciones críticas como `guardarCalendarioConFechas` utilizan `WriteBatch`. Esto asegura que el calendario y las actualizaciones de "última vez cocinado" en las recetas se apliquen como una sola unidad atómica. Si falla la conexión a mitad, Firestore garantiza que la operación se complete íntegramente cuando se restablezca el servicio.
    - **Prevención de Duplicados**: Se utilizan IDs deterministas (ej: `uid_año_mes` para calendario, `uid_nombre` para ingredientes de usuario) y el método `.set(data, SetOptions.merge())`. Esto previene la creación de documentos duplicados ante reintentos o fallos de sincronización.
    - **Flujo de Carga**: `FirebaseManager.cargarRecetas` sigue un flujo de tres pasos: Memoria $\rightarrow$ Caché Local $\rightarrow$ Servidor (en segundo plano para refrescar). Los fallos de red se notifican mediante `onFailure` en los callbacks y Toasts informativos al usuario.

## 8. Cómo debe trabajar el agente
- **Respeto a la arquitectura**: No introducir Kotlin ni Compose a menos que se pida explícitamente; mantener Java y XML.
- **Seguridad**: Asegurar que toda consulta a la base de datos incluya el filtro de `userId`.
- **Caché**: Si se añaden nuevas entidades, implementar la lógica de caché en `FirebaseManager` para evitar lecturas excesivas de Firestore.
- **Validación**: Validar los datos antes de enviarlos a Firebase (usar los métodos `validarReceta` existentes).

## 9. Decisiones tomadas en el desarrollo
- **Algoritmo Mediterráneo**: Se implementó una lógica de "ruleta ponderada" en `CalendarioSrv` que utiliza máscaras de bits (`tiposMask`) para asegurar la variedad nutricional de forma eficiente.

    ### Funcionamiento de tiposMask
    La máscara de bits es un valor `long` donde cada bit representa la presencia de un tipo de ingrediente en la receta. Esto permite realizar comprobaciones nutricionales complejas y filtrado de frecuencia en tiempo constante $O(1)$.

    #### Mapeo de Bits (TipoIngrediente)
    Existen **25 categorías** definidas en `TipoIngrediente.java`. La posición del bit corresponde al `ordinal()` del enum:
    - **Bit 0**: `ESPECIA`
    - **Bit 1**: `VERDURA`
    - **Bit 2**: `LEGUMBRE`
    - **Bit 3**: `FRUTA`
    - **Bit 4**: `FRUTO_SECO`
    - **Bit 5**: `SEMILLA`
    - **Bit 6**: `CARNE_BLANCA`
    - **Bit 7**: `CARNE_ROJA`
    - **Bit 8**: `CARNE_PROCESADA`
    - **Bit 9**: `PESCADO_BLANCO`
    - **Bit 10**: `PESCADO_AZUL`
    - **Bit 11**: `MARISCO`
    - **Bit 12**: `PROTEINA_VEGETAL`
    - **Bit 13**: `CEREAL`
    - **Bit 14**: `HARINA`
    - **Bit 15**: `PASTA`
    - **Bit 16**: `ACEITE`
    - **Bit 17**: `GRASA`
    - **Bit 18**: `LACTEO`
    - **Bit 19**: `HUEVO`
    - **Bit 20**: `PROCESADO`
    - **Bit 21**: `BEBIDA`
    - **Bit 22**: `SALSA`
    - **Bit 23**: `DULCE`
    - **Bit 24**: `OTROS`

    #### Máscaras de Conveniencia (Agrupaciones)
    El sistema utiliza agrupaciones de bits para reglas de salud globales:
    - **`MASK_CARNE_ROJA`**: Bits 7, 8 (Roja + Procesada).
    - **`MASK_PESCADO`**: Bits 9, 10, 11 (Blanco + Azul + Marisco).
    - **`MASK_PASTA_ARROZ`**: Bits 13, 15 (Cereal + Pasta).
    - **`MASK_PROTEINA`**: Carne Roja/Procesada + Blanca + Pescado + Huevo + Legumbre.
    - **`MASK_PESADA`**: `MASK_CARNE_ROJA` + `PASTA` + `LEGUMBRE`.
    > [!NOTE]
    > Es intencional que `PASTA` y `LEGUMBRE` formen parte de `MASK_PESADA` (Balance Diario) a la vez que tienen sus propios límites en la **Fase 5: Frecuencia Semanal**. Esto asegura que, aunque no se superen los máximos semanales, no se concentren estos platos en el mismo día.
    - **`MASK_DENSA`**: `MASK_PESADA` + `CEREAL` + `HARINA` + `GRASA` + `DULCE` + `PROCESADO`.

    #### Cálculo de Flags Nutricionales
    En `CalendarioSrv.CachedRecetaData`, se determinan dos flags críticos para el balance diario:
    - **`isPesada`**: Se activa si la receta contiene algún ingrediente de la `MASK_PESADA` (Carne Roja/Procesada, Pasta o Legumbre).
    - **`isDensa`**:
        - **True** si la puntuación de salud es muy baja (<0.4) o si contiene ingredientes de la `MASK_DENSA`.
        - **False** si la puntuación de salud es alta (>0.7), independientemente de los ingredientes.
        - En otros casos, depende de la presencia de ingredientes en `MASK_DENSA`.

    ### Pesos en la Ruleta Ponderada
    El algoritmo selecciona recetas basándose en una puntuación dinámica que modifica el peso base ($Salud^2$):
    - **Prioridad de Salud**: Se utiliza `Math.pow(healthNorm, 2)` para que las recetas más sanas tengan una probabilidad exponencialmente mayor de salir.

    | Condición | Modificador | Objetivo Nutricional |
    | :--- | :--- | :--- |
    | **Falta Legumbre** (<2/sem) | `x 3.0` | Fomentar consumo de fibra y proteína vegetal. |
    | **Falta Pescado** (<3/sem) | `x 3.0` | Priorizar Omega-3 y grasas saludables. |
    | **Exceso Carne Blanca** (>=3/sem) | `x 0.2` | Limitar pollo/pavo a favor de otras proteínas. |
    | **Exceso Carne Roja** (>=1/sem) | `x 0.1` | Penalización fuerte si se supera el límite crítico. |
    | **Exceso Huevos** (>=2/sem) | `x 0.2` | Limitar ingesta semanal según recomendaciones. |
    | **Falta Verdura hoy** (<1) | `x 2.0` | Forzar la presencia de vegetales en el menú diario. |
    | **Cena Densa** | `x 0.75` | Penalizar platos pesados antes de dormir. |
    | **Cena Ligera** | `x 1.25` | Favorecer digestiones fáciles nocturnas. |
    | **Modo Vago** (Receta <20 min) | `x 3.5` | Priorizar velocidad en situaciones de poco tiempo. |
    | **Sinergia Temporal** | `Bonus` | Aprovechar ingredientes frescos usados en días previos. |

    #### Filtros Críticos (Hard Constraints)
    El algoritmo aplica un pipeline de filtrado jerárquico. Una receta solo llega a la fase de puntuación (pesos) si supera todas estas validaciones en orden (con relajación progresiva en 3 niveles):

    1.  **Temporada y Tipo (Pre-filtro)**: La receta debe ser del tipo adecuado (`PRINCIPAL` para comida/cena) y pertenecer a la temporada del mes.
    2.  **Momento (MomentoReceta)**: Filtro por `MomentoReceta` (Comida/Cena).
    3.  **Recencia de Sesión**: Evita repetir recetas que ya han sido asignadas en la misma ejecución del "Rellenar" (memoria volátil).
    4.  **Recencia Histórica (Relajación Progresiva)**: Se comprueba la última vez que se cocinó. El límite empieza en **14 días** y baja progresivamente (10, 7, 3, 0).
    5.  **Frecuencia Semanal (Ventana Deslizante)**:
        - Máximo **1** carne roja/procesada por semana.
        - Máximo **2** legumbres por semana.
        - Máximo **2** pasta/arroz por semana.
        - Máximo **3** carne blanca por semana.
        - Máximo **2** huevos por semana.
    6.  **Variedad de Proteína**:
        - Prohibido repetir el mismo tipo de proteína principal (definida en `MASK_PROTEINA`) en el mismo día.
        - Prohibido repetir proteína respecto al día anterior (Consecutiva).
    7.  **Variedad de Ingredientes (Similitud)**: Máximo **1 ingrediente significativo** compartido con otras recetas del mismo día.
    8.  **Balance Diario (Intradiario)**:
        - No permitir dos platos "pesados" (`isPesada`) el mismo día.
        - **Conflictos Directos**: Prohibido combinar Pasta y Legumbre en el mismo día.
- **Carga de Ingredientes**: Los ingredientes se cargan desde un XML estático (`ingredient_list.xml`) para estandarizar puntuaciones y traducciones, complementado con el mapa de ingredientes del usuario.

    ### Estructura de ingredient_list.xml
    El archivo define los metadatos globales para cada ingrediente. Para añadir uno nuevo, debe seguir este formato:
    ```xml
    <item puntuacion="10" gramos="150" tipo="VERDURA">
      <nombre lang="es">Nombre en Español</nombre>
      <nombre lang="en">English Name</nombre>
    </item>
    ```
    - **`puntuacion`**: Nota de salud (0-10). Usar `-1` para ingredientes neutros (ej: especias).
    - **`gramos`**: Peso de una "unidad" estándar del ingrediente. Usar `-1` si no aplica.
    - **`tipo`**: Categoría obligatoria (debe coincidir con los nombres en `TipoIngrediente.java`).

- **Batch Updates**: Se utilizan `WriteBatch` de Firestore para asegurar que la actualización del calendario y las fechas de "última vez cocinado" de las recetas sean atómicas. Los fallos de red se gestionan mediante notificaciones pasivas (Toasts); Firestore se encarga de reintentar las escrituras pendientes en background gracias a la caché persistente.
- **IA Desconectada**: Se optó por un flujo manual de IA para evitar costes de API y dependencias de red adicionales, manteniendo la privacidad del usuario.
- **Sistema de puntuación de salud**: Implementado en `RecetasSrv`, calcula una nota de **0 a 10** para cada receta basándose en la composición nutricional de sus ingredientes.

    ### La Fórmula
    La puntuación total ($H$) es una **media ponderada** de las puntuaciones individuales de los ingredientes:
    $$H = \frac{\sum_{i \in R} (S_i \cdot Q_i \cdot W_i)}{\sum_{i \in R} (Q_i \cdot W_i)}$$

    Donde:
    - **$R$**: Conjunto de ingredientes "representantes". Se excluyen los opcionales y los que no tienen puntuación (score < 0). Si hay ingredientes que son sustitutos de otros, solo se toma el que tenga la puntuación más alta del grupo.
    - **$S_i$**: Puntuación de salud del ingrediente $i$ (0-10). Obtenida de `ingredient_list.xml`, del mapa de ingredientes personalizados del usuario, o calculada recursivamente si es una receta vinculada.
    - **$Q_i$**: Cantidad numérica del ingrediente.
    - **$W_i$**: Factor de importancia (peso) basado en la unidad de medida, obtenido de `R.array.importance_values`:
        - Unidades estándar: `g`, `ml` (1); `kg`, `l` (1000).
        - Unidades de volumen: `taza` (240), `vaso` (250), `cucharada` (10), `cucharadita` (5).
        - Unidades especiales: `lata` (400), `loncha` (30), `diente` (5).
        - Si la unidad es `unidad`, se usa el valor `gramos` definido para ese ingrediente en `ingredient_list.xml` (ej: un huevo = 60g). Si no está definido, se usa un valor por defecto (150).
        > [!IMPORTANT]
        > Si se modifican estos valores, debe asegurarse la consistencia con la **Lógica de Consolidación** de la lista de la compra (Sección 6).

    ### Variables Consideradas
    - ✅ **Ingredientes**: Puntuación individual (salubridad).
    - ✅ **Cantidades y Unidades**: Determinan el peso relativo de cada ingrediente en el total.
    - ✅ **Sustitutos**: Se utiliza la lógica de "mejor representante" para no penalizar/premiar doblemente.
    - ✅ **Recetas Vinculadas**: Permite composiciones complejas (ej: una salsa que es una receta en sí misma).

    ### Variables NO Consideradas
    - ❌ **Alérgenos**: No afectan a la puntuación de salud (son informativos/filtros).
    - ❌ **Tipo de Cocción**: El sistema asume la salubridad intrínseca del ingrediente.
    - ❌ **Temporada / Momento del día**: No influyen en el cálculo numérico.

    ### Rango Resultante
    - El resultado es un valor de **0.0 a 10.0**.
    - La UI utiliza este valor para mostrar un badge de color: **Verde** (>= 8.1), **Verde claro** (>= 6.1), **Amarillo** (>= 3.1) y **Rojo** (< 3.1).

## 10. Flujo de trabajo y CI/CD

Actualmente, el proyecto sigue un enfoque de desarrollo simplificado:

- **Repositorio**: Se trabaja en la rama `master`. Actualmente existen cambios pendientes de commit en lógica de recetas, importación IA y recursos de strings.
- **CI/CD**: No existen pipelines de Integración Continua o Despliegue Continuo (GitHub Actions, Jenkins, etc.). Las compilaciones, el firmado de APKs y el despliegue se realizan de forma manual desde el entorno local.
- **Gestión de Ramas**: Se trabaja directamente sobre la rama principal `master`. No se ha implementado un sistema de ramas de características (Feature Branches) o Gitflow por el momento.
- **Convenciones de Commits**:
    - **Idioma**: Español.
    - **Formato**: Mensajes descriptivos que resumen los cambios (ej: "Mejoras en la gestión de recetas: ...", "Correccion posicion toast"). No se utilizan prefijos estrictos como en *Conventional Commits* (`feat:`, `fix:`), aunque se tiende a usar palabras clave como "Mejoras" o "Correccion".
- **Calidad de Código**: 
    - Las validaciones son manuales.
    - No hay ejecución automática de linters o tests en el flujo de Git (no hay git-hooks documentados).
    - Se recomienda revisar manualmente la consistencia con el estilo existente antes de cada commit.
