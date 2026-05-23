package pe.utp.model;

public class Empleado {
    private String idEmpleado;
    private String nombre;
    private String cargo;
    private String usuario;
    private String password;
    private String dni;

    public Empleado() {}

    public Empleado(String idEmpleado, String nombre, String cargo,
                    String usuario, String password, String dni) {
        this.idEmpleado = idEmpleado;
        this.nombre = nombre;
        this.cargo = cargo;
        this.usuario = usuario;
        this.password = password;
        this.dni = dni;
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

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    @Override
    public String toString() { return nombre; }
}