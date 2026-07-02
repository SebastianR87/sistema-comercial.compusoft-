package pe.utp.model;

import java.time.LocalDateTime;

public class Cotizacion {

    public static final String PENDIENTE  = "PENDIENTE";
    public static final String ACEPTADA   = "ACEPTADA";
    public static final String RECHAZADA  = "RECHAZADA";
    public static final String CONVERTIDA = "CONVERTIDA";

    private String idCotizacion;
    private Cliente cliente;
    private Empleado empleado;
    private LocalDateTime fecha;
    private double descuento;
    private double total;
    private String estado;

    public Cotizacion() {}

    public Cotizacion(String idCotizacion, Cliente cliente,
                      Empleado empleado, LocalDateTime fecha,
                      double descuento, double total, String estado) {
        this.idCotizacion = idCotizacion;
        this.cliente   = cliente;
        this.empleado  = empleado;
        this.fecha     = fecha;
        this.descuento = descuento;
        this.total     = total;
        this.estado    = estado;
    }

    public String        getIdCotizacion() { return idCotizacion; }
    public void          setIdCotizacion(String v) { idCotizacion = v; }
    public Cliente       getCliente()  { return cliente; }
    public void          setCliente(Cliente v) { cliente = v; }
    public Empleado      getEmpleado() { return empleado; }
    public void          setEmpleado(Empleado v) { empleado = v; }
    public LocalDateTime getFecha()    { return fecha; }
    public void          setFecha(LocalDateTime v) { fecha = v; }
    public double        getDescuento(){ return descuento; }
    public void          setDescuento(double v) { descuento = v; }
    public double        getTotal()    { return total; }
    public void          setTotal(double v) { total = v; }
    public String        getEstado()   { return estado; }
    public void          setEstado(String v) { estado = v; }

    @Override
    public String toString() { return idCotizacion; }
}