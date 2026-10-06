package com.david.recetapp.negocio.beans;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FiltroRecetas implements Serializable {

    public enum CriterioOrden {
        NOMBRE_ASC,
        NOMBRE_DESC,
        ESTRELLAS_DESC,
        ESTRELLAS_ASC,
        SALUD_DESC,
        SALUD_ASC,
        TIEMPO_ASC,
        FECHA_DESC
    }

    private String query;
    private TipoReceta tipoReceta;
    private MomentoReceta momentoReceta;
    private Set<Temporada> temporadas;
    private float minEstrellas;
    private double minPuntuacionSalud;
    private Set<Integer> alergenosExcluidos;
    private int maxTiempoMinutos;
    private boolean soloIngredientesSinPuntuar;
    private CriterioOrden criterioOrden;

    public FiltroRecetas() {
        reset();
    }

    public FiltroRecetas(FiltroRecetas other) {
        if (other != null) {
            this.query = other.query;
            this.tipoReceta = other.tipoReceta;
            this.momentoReceta = other.momentoReceta;
            this.temporadas = new HashSet<>(other.temporadas);
            this.minEstrellas = other.minEstrellas;
            this.minPuntuacionSalud = other.minPuntuacionSalud;
            this.alergenosExcluidos = new HashSet<>(other.alergenosExcluidos);
            this.maxTiempoMinutos = other.maxTiempoMinutos;
            this.soloIngredientesSinPuntuar = other.soloIngredientesSinPuntuar;
            this.criterioOrden = other.criterioOrden;
        } else {
            reset();
        }
    }

    public void reset() {
        this.query = "";
        this.tipoReceta = null;
        this.momentoReceta = null;
        this.temporadas = new HashSet<>();
        this.minEstrellas = 0.0f;
        this.minPuntuacionSalud = 0.0;
        this.alergenosExcluidos = new HashSet<>();
        this.maxTiempoMinutos = 0;
        this.soloIngredientesSinPuntuar = false;
        this.criterioOrden = CriterioOrden.NOMBRE_ASC;
    }

    public boolean test(Receta receta) {
        if (receta == null) return false;

        // 1. Filtro por texto (Nombre o Ingrediente)
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            boolean nameMatch = receta.getNombre() != null &&
                    receta.getNombre().toLowerCase(Locale.ROOT).contains(q);
            boolean ingrMatch = receta.getIngredientes() != null &&
                    receta.getIngredientes().stream().anyMatch(i -> i.getNombre() != null &&
                            i.getNombre().toLowerCase(Locale.ROOT).contains(q));
            if (!nameMatch && !ingrMatch) return false;
        }

        // 2. Filtro por Tipo de Receta
        if (tipoReceta != null && receta.getTipoReceta() != tipoReceta) {
            return false;
        }

        // 3. Filtro por Momento de Receta
        if (momentoReceta != null) {
            MomentoReceta recMomento = receta.getMomentoReceta();
            if (recMomento != null && recMomento != MomentoReceta.AMBOS && recMomento != momentoReceta) {
                return false;
            }
        }

        // 4. Filtro por Temporada (Si hay seleccionadas, debe coincidir al menos una)
        if (temporadas != null && !temporadas.isEmpty()) {
            List<Temporada> recTemporadas = receta.getTemporadas();
            if (recTemporadas == null || recTemporadas.stream().noneMatch(temporadas::contains)) {
                return false;
            }
        }

        // 5. Filtro por Estrellas mínimas
        if (minEstrellas > 0.0f) {
            float estrellas = receta.getEstrellas();
            if (estrellas < minEstrellas) return false;
        }

        // 6. Filtro por Salubridad mínima
        if (minPuntuacionSalud > 0.0) {
            double salud = receta.getPuntuacionDada();
            if (salud < minPuntuacionSalud) return false;
        }

        // 7. Exclusión por Alérgenos
        if (alergenosExcluidos != null && !alergenosExcluidos.isEmpty()) {
            List<Alergeno> recetaAlergenos = receta.getAlergenos();
            if (recetaAlergenos != null) {
                boolean tieneAlergenoProhibido = recetaAlergenos.stream()
                        .anyMatch(a -> alergenosExcluidos.contains(a.getNumero()));
                if (tieneAlergenoProhibido) return false;
            }
        }

        // 8. Tiempo máximo de preparación
        if (maxTiempoMinutos > 0) {
            if (receta.getTiempoTotalMinutos() > maxTiempoMinutos) {
                return false;
            }
        }

        // 9. Solo recetas con ingredientes sin puntuar
        if (soloIngredientesSinPuntuar) {
            List<Ingrediente> ings = receta.getIngredientes();
            return ings != null && ings.stream().anyMatch(i -> i.getPuntuacion() < -1);
        }

        return true;
    }

    public Comparator<Receta> getComparator() {
        Comparator<Receta> nombreComp = Comparator.comparing(
                r -> r.getNombre() != null ? r.getNombre().toLowerCase(Locale.ROOT) : "",
                String.CASE_INSENSITIVE_ORDER);

        if (criterioOrden == null) return nombreComp;

        return switch (criterioOrden) {
            case NOMBRE_DESC -> nombreComp.reversed();
            case ESTRELLAS_DESC -> Comparator.comparingDouble((Receta r) -> (double) r.getEstrellas()).reversed()
                    .thenComparing(nombreComp);
            case ESTRELLAS_ASC -> Comparator.comparingDouble((Receta r) -> (double) (r.getEstrellas() < 0 ? 99f : r.getEstrellas()))
                    .thenComparing(nombreComp);
            case SALUD_DESC -> Comparator.comparingDouble(Receta::getPuntuacionDada).reversed()
                    .thenComparing(nombreComp);
            case SALUD_ASC -> Comparator.comparingDouble((Receta r) -> r.getPuntuacionDada() < 0 ? 99.0 : r.getPuntuacionDada())
                    .thenComparing(nombreComp);
            case TIEMPO_ASC -> Comparator.comparingInt(Receta::getTiempoTotalMinutos)
                    .thenComparing(nombreComp);
            case FECHA_DESC -> Comparator.comparing((Receta r) -> r.getFechaCalendario() != null ? r.getFechaCalendario().getTime() : 0L)
                    .reversed().thenComparing(nombreComp);
            default -> nombreComp;
        };
    }

    public int getActiveFilterCount() {
        int count = 0;
        if (tipoReceta != null) count++;
        if (momentoReceta != null) count++;
        if (temporadas != null && !temporadas.isEmpty()) count++;
        if (minEstrellas > 0.0f) count++;
        if (minPuntuacionSalud > 0.0) count++;
        if (alergenosExcluidos != null && !alergenosExcluidos.isEmpty()) count++;
        if (maxTiempoMinutos > 0) count++;
        if (soloIngredientesSinPuntuar) count++;
        if (criterioOrden != CriterioOrden.NOMBRE_ASC) count++;
        return count;
    }

    public boolean isActive() {
        return getActiveFilterCount() > 0;
    }

    // Getters y Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public TipoReceta getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(TipoReceta tipoReceta) { this.tipoReceta = tipoReceta; }

    public MomentoReceta getMomentoReceta() { return momentoReceta; }
    public void setMomentoReceta(MomentoReceta momentoReceta) { this.momentoReceta = momentoReceta; }

    public Set<Temporada> getTemporadas() { return temporadas; }
    public void setTemporadas(Set<Temporada> temporadas) { this.temporadas = temporadas; }

    public float getMinEstrellas() { return minEstrellas; }
    public void setMinEstrellas(float minEstrellas) { this.minEstrellas = minEstrellas; }

    public double getMinPuntuacionSalud() { return minPuntuacionSalud; }
    public void setMinPuntuacionSalud(double minPuntuacionSalud) { this.minPuntuacionSalud = minPuntuacionSalud; }

    public Set<Integer> getAlergenosExcluidos() { return alergenosExcluidos; }
    public void setAlergenosExcluidos(Set<Integer> alergenosExcluidos) { this.alergenosExcluidos = alergenosExcluidos; }

    public int getMaxTiempoMinutos() { return maxTiempoMinutos; }
    public void setMaxTiempoMinutos(int maxTiempoMinutos) { this.maxTiempoMinutos = maxTiempoMinutos; }

    public boolean isSoloIngredientesSinPuntuar() { return soloIngredientesSinPuntuar; }
    public void setSoloIngredientesSinPuntuar(boolean soloIngredientesSinPuntuar) { this.soloIngredientesSinPuntuar = soloIngredientesSinPuntuar; }

    public CriterioOrden getCriterioOrden() { return criterioOrden; }
    public void setCriterioOrden(CriterioOrden criterioOrden) { this.criterioOrden = criterioOrden; }
}
