package pe.utp.security;

import pe.utp.model.Empleado;

public final class Sesion {

    private static Empleado empleado;

    private Sesion() {}

    public static void iniciar(Empleado e) {
        empleado = e;
    }

    public static void cerrar() {
        empleado = null;
    }

    public static Empleado getEmpleado() {
        return empleado;
    }

    public static Rol getRol() {
        if (empleado == null) return null;
        return Rol.desdeCargo(empleado.getCargo());
    }

    public static boolean estaAutenticado() {
        return empleado != null && getRol() != null;
    }
}
