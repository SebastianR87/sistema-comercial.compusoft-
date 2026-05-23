package pe.utp.model;

public class DetalleCotizacion {
    private String idDetalle;
    private Cotizacion cotizacion;
    private Producto producto;
    private int cantidad;
    private double precio;

    public DetalleCotizacion() {}

    public DetalleCotizacion(String idDetalle, Cotizacion cotizacion,
                             Producto producto, int cantidad, double precio) {
        this.idDetalle = idDetalle;
        this.cotizacion = cotizacion;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getIdDetalle() { return idDetalle; }
    public void setIdDetalle(String idDetalle) { this.idDetalle = idDetalle; }

    public Cotizacion getCotizacion() { return cotizacion; }
    public void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public double getSubtotal() { return cantidad * precio; }

    @Override
    public String toString() { return producto.getNombre() + " x" + cantidad; }
}