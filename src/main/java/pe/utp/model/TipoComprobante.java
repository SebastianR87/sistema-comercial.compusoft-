package pe.utp.model;

public class TipoComprobante {
    private String idTipoComprobante;
    private String nombre;

    public TipoComprobante() {}

    public TipoComprobante(String idTipoComprobante, String nombre) {
        this.idTipoComprobante = idTipoComprobante;
        this.nombre = nombre;
    }

    public String getIdTipoComprobante() {return idTipoComprobante;}
    public void setIdTipoComprobante(String idTipoComprobante) {this.idTipoComprobante = idTipoComprobante;}

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}

    @Override
    public String toString() {return nombre;}
}

