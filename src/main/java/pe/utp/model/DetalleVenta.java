package pe.utp.model;

public class DetalleVenta {
    private String idDetalleVenta;
    private Venta venta;
    private Producto producto;
    private int cantidad;
    private double precio;

    public DetalleVenta() {}

    public DetalleVenta(String idDetalleVenta, Venta venta,
                        Producto producto, int cantidad, double precio) {
        this.idDetalleVenta = idDetalleVenta;
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getIdDetalleVenta() { return idDetalleVenta; }
    public void setIdDetalleVenta(String idDetalleVenta) { this.idDetalleVenta = idDetalleVenta; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

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