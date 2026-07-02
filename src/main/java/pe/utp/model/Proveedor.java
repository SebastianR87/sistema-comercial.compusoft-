package pe.utp.model;

public class Proveedor {
    private String idProveedor;
    private String nombre;
    private String ruc;
    private String telefono;
    private String direccion;

    public Proveedor() {}

    public Proveedor(String idProveedor, String nombre, String ruc, String telefono,
                     String direccion) {
        this.idProveedor = idProveedor;
        this.nombre = nombre;
        this.ruc = ruc;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public String getIdProveedor() { return idProveedor; }
    public void setIdProveedor(String idProveedor) { this.idProveedor = idProveedor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @Override
    public String toString() { return nombre; }
}