
package pe.utp.model;

public class Empleado {
    private String idEmpleado;
    private String nombre;
    private String cargo;
    private String usuario;
    private String password;
    private String tipoDocumento;
    private String numeroDocumento;
    private String telefono;
    private String direccion;
    private String estado;

    public Empleado() {}

    public Empleado(String idEmpleado, String nombre, String cargo,
                    String usuario, String password,
                    String tipoDocumento, String numeroDocumento,
                    String telefono, String direccion, String estado) {
        this.idEmpleado      = idEmpleado;
        this.nombre          = nombre;
        this.cargo           = cargo;
        this.usuario         = usuario;
        this.password        = password;
        this.tipoDocumento   = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.telefono        = telefono;
        this.direccion       = direccion;
        this.estado          = estado != null ? estado : "ACTIVO";
    }

    public String getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(String idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() { return nombre; }
}