package pe.utp.model;

import java.time.LocalDateTime;

public class Lote {
    private String idLote;
    private Producto producto;
    private String idDetalleCompra;
    private int cantidadOriginal;
    private int cantidadRestante;
    private double costoUnitario;
    private LocalDateTime fecha;
    private String estado;

    public Lote() {}

    public Lote(String idLote, Producto producto, String idDetalleCompra,
                int cantidadOriginal, int cantidadRestante,
                double costoUnitario, LocalDateTime fecha, String estado) {
        this.idLote = idLote;
        this.producto = producto;
        this.idDetalleCompra = idDetalleCompra;
        this.cantidadOriginal = cantidadOriginal;
        this.cantidadRestante = cantidadRestante;
        this.costoUnitario = costoUnitario;
        this.fecha = fecha;
        this.estado = estado;
    }

    public String getIdLote() { return idLote; }
    public void setIdLote(String idLote) { this.idLote = idLote; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(String idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }

    public int getCantidadOriginal() { return cantidadOriginal; }
    public void setCantidadOriginal(int cantidadOriginal) { this.cantidadOriginal = cantidadOriginal; }

    public int getCantidadRestante() { return cantidadRestante; }
    public void setCantidadRestante(int cantidadRestante) { this.cantidadRestante = cantidadRestante; }

    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    /** Valor monetario restante en este lote (cantidad_restante x costo_unitario). */
    public double getValorRestante() {
        return cantidadRestante * costoUnitario;
    }

    @Override
    public String toString() {
        return (producto != null ? producto.getNombre() : "") +
                " - " + cantidadRestante + " unid. a S/ " + costoUnitario;
    }
}
