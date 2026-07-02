package pe.utp.model;

import java.time.LocalDateTime;

public class Venta {
    private String idVenta;
    private Cliente cliente;
    private Empleado empleado;
    private TipoComprobante tipoComprobante;
    private MetodoPago metodoPago;
    private String numeroComprobante;
    private LocalDateTime fecha;
    private double descuento;
    private double total;
    private double montoPagado;
    private double vuelto;

    public Venta() {}

    public Venta(String idVenta, Cliente cliente, Empleado empleado,
                 TipoComprobante tipoComprobante, MetodoPago metodoPago,
                 String numeroComprobante, LocalDateTime fecha,
                 double descuento, double total) {
        this.idVenta = idVenta;
        this.cliente = cliente;
        this.empleado = empleado;
        this.tipoComprobante = tipoComprobante;
        this.metodoPago = metodoPago;
        this.numeroComprobante = numeroComprobante;
        this.fecha = fecha;
        this.descuento = descuento;
        this.total = total;
    }

    public String getIdVenta() { return idVenta; }
    public void setIdVenta(String idVenta) { this.idVenta = idVenta; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public TipoComprobante getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(TipoComprobante tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(double montoPagado) { this.montoPagado = montoPagado; }

    public double getVuelto() { return vuelto; }
    public void setVuelto(double vuelto) { this.vuelto = vuelto; }

    @Override
    public String toString() { return numeroComprobante; }
}