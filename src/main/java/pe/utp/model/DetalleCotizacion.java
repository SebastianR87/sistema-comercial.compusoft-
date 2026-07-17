package pe.utp.model;

public class DetalleCotizacion {

    private String       idDetalle;
    private Cotizacion   cotizacion;
    private Producto     producto;
    private int          cantidad;
    private double       precio;

    public DetalleCotizacion() {}

    public DetalleCotizacion(String idDetalle, Cotizacion cotizacion,
                             Producto producto, int cantidad, double precio) {
        this.idDetalle   = idDetalle;
        this.cotizacion  = cotizacion;
        this.producto    = producto;
        this.cantidad    = cantidad;
        this.precio      = precio;
    }

    public String      getIdDetalle()  { return idDetalle; }
    public void        setIdDetalle(String v) { idDetalle = v; }
    public Cotizacion  getCotizacion() { return cotizacion; }
    public void        setCotizacion(Cotizacion v) { cotizacion = v; }
    public Producto    getProducto()   { return producto; }
    public void        setProducto(Producto v) { producto = v; }
    public int         getCantidad()   { return cantidad; }
    public void        setCantidad(int v) { cantidad = v; }
    public double      getPrecio()     { return precio; }
    public void        setPrecio(double v) { precio = v; }
    public double      getSubtotal()   { return cantidad * precio; }
}