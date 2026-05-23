package pe.utp.model;

public class Especificacion {
    private String idEspecificacion;
    private String idProducto;
    private String clave;
    private String valor;

    public Especificacion() {}

    public Especificacion(String idEspecificacion, String idProducto,
                          String clave, String valor) {
        this.idEspecificacion = idEspecificacion;
        this.idProducto = idProducto;
        this.clave = clave;
        this.valor = valor;
    }

    public String getIdEspecificacion() { return idEspecificacion; }
    public void setIdEspecificacion(String idEspecificacion) { this.idEspecificacion = idEspecificacion; }

    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    @Override
    public String toString() { return clave + ": " + valor; }
}