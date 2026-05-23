package pe.utp.model;

public class ReglaCompatibilidad {
    private String idRegla;
    private Categoria categoria1;
    private Categoria categoria2;
    private String claveComparar;
    private String tipoComparacion;
    private String descripcion;

    public ReglaCompatibilidad() {}

    public ReglaCompatibilidad(String idRegla, Categoria categoria1,
                               Categoria categoria2, String claveComparar,
                               String tipoComparacion, String descripcion) {
        this.idRegla = idRegla;
        this.categoria1 = categoria1;
        this.categoria2 = categoria2;
        this.claveComparar = claveComparar;
        this.tipoComparacion = tipoComparacion;
        this.descripcion = descripcion;
    }

    public String getIdRegla() { return idRegla; }
    public void setIdRegla(String idRegla) { this.idRegla = idRegla; }

    public Categoria getCategoria1() { return categoria1; }
    public void setCategoria1(Categoria categoria1) { this.categoria1 = categoria1; }

    public Categoria getCategoria2() { return categoria2; }
    public void setCategoria2(Categoria categoria2) { this.categoria2 = categoria2; }

    public String getClaveComparar() { return claveComparar; }
    public void setClaveComparar(String claveComparar) { this.claveComparar = claveComparar; }

    public String getTipoComparacion() { return tipoComparacion; }
    public void setTipoComparacion(String tipoComparacion) { this.tipoComparacion = tipoComparacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean esComparacionIgual() { return "IGUAL".equals(tipoComparacion); }
    public boolean esComparacionMayorIgual() { return "MAYOR_IGUAL".equals(tipoComparacion); }

    @Override
    public String toString() { return descripcion; }
}

