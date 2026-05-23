package pe.utp.model;

public class MetodoPago {
    private String idMetodoPago;
    private String metodoDePago;

    public MetodoPago() {}

    public MetodoPago(String idMetodoPago, String metodoDePago) {
        this.idMetodoPago = idMetodoPago;
        this.metodoDePago = metodoDePago;
    }

    public String getIdMetodoPago() { return idMetodoPago; }
    public void setIdMetodoPago(String idMetodoPago) { this.idMetodoPago = idMetodoPago; }

    public String getMetodoDePago() { return metodoDePago; }
    public void setMetodoDePago(String metodoDePago) { this.metodoDePago = metodoDePago; }

    @Override
    public String toString() { return metodoDePago; }
}