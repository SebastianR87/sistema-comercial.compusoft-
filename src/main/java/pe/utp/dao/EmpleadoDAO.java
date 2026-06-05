package pe.utp.dao;
import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
import pe.utp.model.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    private Connection conexion;

    public EmpleadoDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public Empleado login(String usuario, String password) {
        String sql = "SELECT * FROM empleado WHERE usuario = ? AND password = ? AND estado = 'ACTIVO'";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, usuario);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapearEmpleado(rs);
        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
        }
        return null;
    }
    /** Lista de todos los empleados **/
    public List<Empleado> listar() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT * FROM empleado ORDER BY estado ASC, nombre ASC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearEmpleado(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar empleados: " + e.getMessage());
        }
        return lista;
    }

    /**
     * INSERTAR: siempre crea el empleado como ACTIVO
     */

    public boolean insertar(Empleado e) {
        String sql = "INSERT INTO empleado " +
                "(id_empleado, nombre, cargo, usuario, password, " +
                "tipo_documento, numero_documento, telefono, direccion, estado) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getIdEmpleado());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getCargo());
            ps.setString(4, e.getUsuario());
            ps.setString(5, e.getPassword());
            ps.setString(6, e.getTipoDocumento());
            ps.setString(7, e.getNumeroDocumento());
            ps.setString(8, e.getTelefono());
            ps.setString(9, e.getDireccion());
            // Siempre ACTIVO al crear
            ps.setString(10, "ACTIVO");
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al insertar empleado: " + ex.getMessage());
            return false;
        }
    }

    public boolean actualizar(Empleado e) {
        String sql = "UPDATE empleado SET nombre=?, cargo=?, usuario=?, " +
                "password=?, tipo_documento=?, numero_documento=?, " +
                "telefono=?, direccion=? " +
                "WHERE id_empleado=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getCargo());
            ps.setString(3, e.getUsuario());
            ps.setString(4, e.getPassword());
            ps.setString(5, e.getTipoDocumento());
            ps.setString(6, e.getNumeroDocumento());
            ps.setString(7, e.getTelefono());
            ps.setString(8, e.getDireccion());
            ps.setString(9, e.getIdEmpleado());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al actualizar empleado: " + ex.getMessage());
            return false;
        }
    }

    public boolean cambiarEstado(String id, String nuevoEstado) {
        String sql = "UPDATE empleado SET estado = ? WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, nuevoEstado);
            ps.setString(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al cambiar estado: " + ex.getMessage());
            return false;
        }
    }

    public boolean eliminar(String idEmpleado) {
        String sql = "DELETE FROM empleado WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idEmpleado);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar empleado: " + e.getMessage());
            return false;
        }
    }


    public Empleado buscarPorId(String id) {
        String sql = "SELECT * FROM empleado WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapearEmpleado(rs);
        } catch (SQLException e) {
            System.out.println("Error al buscar empleado: " + e.getMessage());
        }
        return null;
    }

    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre"));
        e.setCargo(rs.getString("cargo"));
        e.setUsuario(rs.getString("usuario"));
        e.setPassword(rs.getString("password"));
        // Lee por nombre de columna, no por posición,
        // así el orden de la BD no importa
        e.setTipoDocumento(rs.getString("tipo_documento"));
        e.setNumeroDocumento(rs.getString("numero_documento"));
        e.setTelefono(rs.getString("telefono"));
        e.setDireccion(rs.getString("direccion"));
        e.setEstado(rs.getString("estado"));
        return e;
    }

    /**
     * Verifica si ya existe un empleado con ese número de documento
     */
    public boolean existeNumeroDocumento(String numeroDocumento, String idExcluir) {
        String sql;
        if (idExcluir != null) {
            // Al editar: excluye al propio empleado Y verifica estado ACTIVO
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ? " +
                    "AND id_empleado != ? " +
                    "AND estado = 'ACTIVO'";
        } else {
            // Al crear: solo verifica que no exista ese número en activos
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ? " +
                    "AND estado = 'ACTIVO'";
        }

        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, numeroDocumento);
            if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar número de documento: " + e.getMessage());
        }
        return false;
    }

    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_empleado FROM empleado ORDER BY id_empleado DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_empleado");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si el empleado tiene registros en ventas, compras o cotizaciones antes de permitir eliminarlo.
     */
    public boolean tieneMovimientos(String idEmpleado) {
        // Array con las tres tablas donde puede aparecer el empleado
        String[] tablas = {"venta", "compra", "cotizacion"};

        for (String tabla : tablas) {
            // Para cada tabla verificamos si hay registros con ese id
            String sql = "SELECT COUNT(*) FROM " + tabla +
                    " WHERE id_empleado = ?";
            try {
                PreparedStatement ps = conexion.prepareStatement(sql);
                ps.setString(1, idEmpleado);
                ResultSet rs = ps.executeQuery();

                // rs.next() mueve el cursor a la primera fila del resultado
                // rs.getInt(1) trae el valor de la primera columna (el COUNT)
                if (rs.next() && rs.getInt(1) > 0) {
                    // Encontró al menos un registro en esta tabla
                    // No necesita seguir revisando las demás, ya sabemos que
                    // tiene movimientos
                    return true;
                }
            } catch (SQLException e) {
                System.out.println("Error al verificar movimientos en "
                        + tabla + ": " + e.getMessage());
            }
        }

        // Revisó las tres tablas y no encontró ningún registro
        return false;
    }

}