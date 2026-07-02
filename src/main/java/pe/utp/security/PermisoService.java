package pe.utp.security;

import java.util.EnumSet;
import java.util.Set;

public final class PermisoService {

    private PermisoService() {}

    public static boolean puedeAcceder(Modulo modulo) {
        Rol rol = Sesion.getRol();
        if (rol == null) return false;
        return permisosDe(rol).contains(modulo);
    }

    public static boolean puedeEditarCategoria() {
        return Sesion.getRol() == Rol.ADMINISTRADOR;
    }

    public static boolean puedeEditarProducto() {
        Rol rol = Sesion.getRol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.ALMACENERO;
    }

    public static boolean puedeEditarCliente() {
        Rol rol = Sesion.getRol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.VENDEDOR;
    }

    public static boolean puedeEditarProveedor() {
        Rol rol = Sesion.getRol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.ALMACENERO;
    }

    public static boolean puedeEditarCotizacion() {
        Rol rol = Sesion.getRol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.VENDEDOR;
    }

    public static boolean puedeEditarVenta() {
        Rol rol = Sesion.getRol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.VENDEDOR;
    }

    public static boolean puedeAnularVenta() {
        return Sesion.getRol() == Rol.ADMINISTRADOR;
    }

    public static boolean puedeAnularCompra() {
        return Sesion.getRol() == Rol.ADMINISTRADOR;
    }

    private static Set<Modulo> permisosDe(Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR -> EnumSet.allOf(Modulo.class);
            case VENDEDOR -> EnumSet.of(
                    Modulo.PRODUCTO,
                    Modulo.CLIENTE,
                    Modulo.VENTA,
                    Modulo.COTIZACION,
                    Modulo.COMPATIBILIDAD
            );
            case ALMACENERO -> EnumSet.of(
                    Modulo.CATEGORIA,
                    Modulo.PRODUCTO,
                    Modulo.PROVEEDOR,
                    Modulo.KARDEX,
                    Modulo.COMPRA,
                    Modulo.COMPATIBILIDAD
            );
        };
    }
}
