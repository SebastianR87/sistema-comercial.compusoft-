package pe.utp.model;

public class DetalleCompra {
    private String idDetalleCompra;
    private Compra compra;
    private Producto producto;
    private int cantidad;
    private double precio;
    private double costoAnterior;

    public DetalleCompra() {}

    public DetalleCompra(String idDetalleCompra, Compra compra,
                         Producto producto, int cantidad, double precio) {
        this.idDetalleCompra = idDetalleCompra;
        this.compra = compra;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(String idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }

    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public double getSubtotal() { return cantidad * precio; }

    public double getCostoAnterior() { return costoAnterior; }
    public void setCostoAnterior(double costoAnterior) { this.costoAnterior = costoAnterior; }

    @Override
    public String toString() { return producto.getNombre() + " x" + cantidad; }
}