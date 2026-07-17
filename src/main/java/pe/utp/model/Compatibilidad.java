package pe.utp.model;

public class Compatibilidad {

    public static final String COMPATIBLE = "Compatible";
    public static final String NO_COMPATIBLE = "No Compatible";
    public static final String CONDICIONAL = "Condicional";

    private String  idDetalle;
    private Producto producto1;
    private Producto producto2;
    private String estado;
    private String restriccion;

    public Compatibilidad() {}

    public Compatibilidad(String idDetalle, Producto producto1,
                          Producto producto2, String estado,
                          String restriccion) {
        this.idDetalle = idDetalle;
        this.producto1 = producto1;
        this.producto2 = producto2;
        this.estado = estado;
        this.restriccion = restriccion;
    }

    public String getIdDetalle()   { return idDetalle; }
    public void setIdDetalle(String v) { idDetalle = v; }
    public Producto getProducto1()   { return producto1; }
    public void setProducto1(Producto v) { producto1 = v; }
    public Producto getProducto2()   { return producto2; }
    public void setProducto2(Producto v) { producto2 = v; }
    public String getEstado()      { return estado; }
    public void setEstado(String v) { estado = v; }
    public String getRestriccion() { return restriccion; }
    public void setRestriccion(String v) { restriccion = v; }
}