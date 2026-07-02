package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.util.ResultadoEliminacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ValidacionEliminacionDAO {

    private final Connection conexion;

    public ValidacionEliminacionDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public ResultadoEliminacion validarCategoria(String id, String nombre) {
        int productos = contar("producto", "id_categoria", id);
        if (productos > 0) {
            return ResultadoEliminacion.bloquear(
                    "La categoría \"" + nombre + "\" tiene productos registrados",
                    "Hay " + productos + " producto(s) en esta categoría.\n\n" +
                            "Elimine esos productos o asígnelos a otra categoría " +
                            "antes de eliminar la categoría."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarProducto(String id, String nombre) {
        List<String> conflictos = new ArrayList<>();

        int enVentas = contar("detalleventa", "id_producto", id);
        if (enVentas > 0) {
            conflictos.add("• " + enVentas + " registro(s) en ventas");
        }

        // Antes era "detalle_compra" → ahora es "detallecompra"
        int enCompras = contar("detallecompra", "id_producto", id);
        if (enCompras > 0) {
            conflictos.add("• " + enCompras + " registro(s) en compras");
        }

        int enCotizaciones = contar("detallecotizacion", "id_producto", id);
        if (enCotizaciones > 0) {
            conflictos.add("• " + enCotizaciones +
                    " registro(s) en cotizaciones");
        }

        if (!conflictos.isEmpty()) {
            return ResultadoEliminacion.bloquear(
                    "El producto \"" + nombre + "\" está en uso",
                    "No se puede eliminar porque aparece en:\n\n" +
                            String.join("\n", conflictos) + "\n\n" +
                            "Debe conservarse para el historial de operaciones."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarProveedor(String id, String nombre) {
        int compras = contar("compra", "id_proveedor", id);
        if (compras > 0) {
            return ResultadoEliminacion.bloquear(
                    "El proveedor \"" + nombre + "\" tiene compras registradas",
                    "Hay " + compras + " compra(s) asociadas a este proveedor.\n\n" +
                            "No es posible eliminarlo para preservar el historial de compras."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarCliente(String id, String nombre) {
        List<String> conflictos = new ArrayList<>();

        int ventas = contar("venta", "id_cliente", id);
        if (ventas > 0) {
            conflictos.add("• " + ventas + " venta(s)");
        }

        int cotizaciones = contar("cotizacion", "id_cliente", id);
        if (cotizaciones > 0) {
            conflictos.add("• " + cotizaciones + " cotización(es)");
        }

        if (!conflictos.isEmpty()) {
            return ResultadoEliminacion.bloquear(
                    "El cliente \"" + nombre + "\" tiene movimientos registrados",
                    "No se puede eliminar porque tiene:\n\n" +
                            String.join("\n", conflictos) + "\n\n" +
                            "Debe conservarse para el historial de transacciones."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarEmpleado(String id, String nombre) {
        List<String> conflictos = new ArrayList<>();

        int ventas = contar("venta", "id_empleado", id);
        if (ventas > 0) {
            conflictos.add("• " + ventas + " venta(s)");
        }

        int compras = contar("compra", "id_empleado", id);
        if (compras > 0) {
            conflictos.add("• " + compras + " compra(s)");
        }

        if (!conflictos.isEmpty()) {
            return ResultadoEliminacion.bloquear(
                    "El empleado \"" + nombre + "\" tiene movimientos registrados",
                    "No se puede eliminar porque participó en:\n\n" +
                            String.join("\n", conflictos) + "\n\n" +
                            "Si ya no trabaja aquí, use la opción Desactivar en su lugar."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarTipoDocumento(String id, String documento) {
        List<String> conflictos = new ArrayList<>();

        int clientes = contar("cliente", "id_tipo_documento", id);
        if (clientes > 0) {
            conflictos.add("• " + clientes + " cliente(s)");
        }

        int empleados = contar("empleado", "id_tipo_documento", id);
        if (empleados > 0) {
            conflictos.add("• " + empleados + " empleado(s)");
        }

        if (!conflictos.isEmpty()) {
            return ResultadoEliminacion.bloquear(
                    "El tipo de documento \"" + documento + "\" está en uso",
                    "No se puede eliminar porque lo usan:\n\n" +
                            String.join("\n", conflictos) + "\n\n" +
                            "Cambie el tipo de documento de esos registros antes de eliminarlo."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarTipoComprobante(String id, String nombre) {
        int ventas = contar("venta", "id_tipo_comprobante", id);
        if (ventas > 0) {
            return ResultadoEliminacion.bloquear(
                    "El comprobante \"" + nombre + "\" está en uso",
                    "Hay " + ventas + " venta(s) que usan este tipo de comprobante.\n\n" +
                            "No es posible eliminarlo para preservar el historial de ventas."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    public ResultadoEliminacion validarMetodoPago(String id, String nombre) {
        int ventas = contar("venta", "id_metodopago", id);
        if (ventas > 0) {
            return ResultadoEliminacion.bloquear(
                    "El método de pago \"" + nombre + "\" está en uso",
                    "Hay " + ventas + " venta(s) registradas con este método de pago.\n\n" +
                            "No es posible eliminarlo para preservar el historial de ventas."
            );
        }
        return ResultadoEliminacion.permitir();
    }

    private int contar(String tabla, String columna, String valor) {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + columna + " = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Validación en " + tabla + ": " + e.getMessage());
        }
        return 0;
    }
}
