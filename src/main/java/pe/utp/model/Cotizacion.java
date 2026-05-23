package pe.utp.model;

import java.time.LocalDateTime;

public class Cotizacion {
    private String idCotizacion;
    private Cliente cliente;
    private LocalDateTime fecha;

    public Cotizacion() {}

    public Cotizacion(String idCotizacion, Cliente cliente, LocalDateTime fecha) {
        this.idCotizacion = idCotizacion;
        this.cliente = cliente;
        this.fecha = fecha;
    }

    public String getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(String idCotizacion) { this.idCotizacion = idCotizacion; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() { return idCotizacion; }
}
