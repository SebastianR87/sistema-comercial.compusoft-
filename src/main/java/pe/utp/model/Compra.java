package pe.utp.model;

import java.time.LocalDateTime;

public class Compra {
    private String idCompra;
    private Proveedor proveedor;
    private Empleado empleado;
    private String numeroComprobante;
    private LocalDateTime fecha;
    private double total;
    private String estado;

    public Compra() {}

    public Compra(String idCompra, Proveedor proveedor, Empleado empleado,
                  String numeroComprobante, LocalDateTime fecha, double total) {
        this.idCompra = idCompra;
        this.proveedor = proveedor;
        this.empleado = empleado;
        this.numeroComprobante = numeroComprobante;
        this.fecha = fecha;
        this.total = total;
    }

    public String getIdCompra() { return idCompra; }
    public void setIdCompra(String idCompra) { this.idCompra = idCompra; }

    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() { return numeroComprobante; }
}