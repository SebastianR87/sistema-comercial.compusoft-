package pe.utp.model;

public class TipoDocumento {
    private String idTipoDocumento;
    private String documento;

    public TipoDocumento() {}

    public TipoDocumento(String idTipoDocumento, String documento) {
        this.idTipoDocumento = idTipoDocumento;
        this.documento = documento;
    }

    public String getIdTipoDocumento() {return idTipoDocumento;}
    public void setIdTipoDocumento(String idTipoDocumento) {this.idTipoDocumento = idTipoDocumento;}

    public String getDocumento() {return documento;}
    public void setDocumento(String documento) {this.documento = documento;}

    @Override
    public String toString() {return documento;}
}
