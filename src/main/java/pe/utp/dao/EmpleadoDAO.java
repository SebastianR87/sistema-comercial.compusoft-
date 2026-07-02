package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Empleado;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    private Connection conexion;

    public EmpleadoDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    // ===== LOGIN =====
    public Empleado login(String usuario, String password) {
        String sql = "SELECT e.*, t.documento " +
                "FROM empleado e " +
                "LEFT JOIN tipo_documento t ON e.id_tipo_documento = t.id_tipo_documento " +
                "WHERE e.usuario = ? AND e.password = ? AND e.estado = 'ACTIVO'";
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

    // ===== LISTAR =====
    public List<Empleado> listar() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT e.*, t.documento " +
                "FROM empleado e " +
                "LEFT JOIN tipo_documento t ON e.id_tipo_documento = t.id_tipo_documento " +
                "ORDER BY e.estado ASC, e.nombre ASC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearEmpleado(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar empleados: " + e.getMessage());
        }
        return lista;
    }

    // ===== INSERTAR =====
    public boolean insertar(Empleado e) {
        String sql = "INSERT INTO empleado " +
                "(id_empleado, nombre, cargo, usuario, password, " +
                "id_tipo_documento, numero_documento, telefono, direccion, estado) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getIdEmpleado());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getCargo());
            ps.setString(4, e.getUsuario());
            ps.setString(5, e.getPassword());
            ps.setString(6, e.getIdTipoDocumento());
            ps.setString(7, e.getNumeroDocumento());
            ps.setString(8, e.getTelefono());
            ps.setString(9, e.getDireccion());
            ps.setString(10, "ACTIVO");
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al insertar empleado: " + ex.getMessage());
            return false;
        }
    }

    // ===== ACTUALIZAR =====
    public boolean actualizar(Empleado e) {
        String sql = "UPDATE empleado SET nombre=?, cargo=?, usuario=?, " +
                "password=?, id_tipo_documento=?, numero_documento=?, " +
                "telefono=?, direccion=? " +
                "WHERE id_empleado=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getCargo());
            ps.setString(3, e.getUsuario());
            ps.setString(4, e.getPassword());
            ps.setString(5, e.getIdTipoDocumento());
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

    // ===== ELIMINAR =====
    public boolean eliminar(String id) {
        String sql = "DELETE FROM empleado WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar empleado: " + e.getMessage());
            return false;
        }
    }

    // ===== DESACTIVAR =====
    public boolean desactivar(String id) {
        return cambiarEstado(id, "INACTIVO");
    }

    // ===== REACTIVAR =====
    public boolean reactivar(String id) {
        return cambiarEstado(id, "ACTIVO");
    }

    // ===== CAMBIAR ESTADO =====
    public boolean cambiarEstado(String id, String nuevoEstado) {
        String sql = "UPDATE empleado SET estado = ? WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, nuevoEstado);
            ps.setString(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al cambiar estado: " + e.getMessage());
            return false;
        }
    }

    // ===== BUSCAR POR ID =====
    public Empleado buscarPorId(String id) {
        String sql = "SELECT e.*, t.documento " +
                "FROM empleado e " +
                "LEFT JOIN tipo_documento t ON e.id_tipo_documento = t.id_tipo_documento " +
                "WHERE e.id_empleado = ?";
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

    // ===== TIENE MOVIMIENTOS =====
    public boolean tieneMovimientos(String idEmpleado) {
        String[] tablas = {"venta", "compra"};
        for (String tabla : tablas) {
            String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE id_empleado = ?";
            try {
                PreparedStatement ps = conexion.prepareStatement(sql);
                ps.setString(1, idEmpleado);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return true;
            } catch (SQLException e) {
                System.out.println("Error al verificar movimientos: " + e.getMessage());
            }
        }
        return false;
    }

    // ===== OBTENER ULTIMO ID =====
    public String obtenerUltimoId() {
        String sql = "SELECT id_empleado FROM empleado ORDER BY id_empleado DESC LIMIT 1";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_empleado");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }

    // ===== EXISTE NUMERO DOCUMENTO =====
    public boolean existeNumeroDocumento(String numeroDocumento, String idExcluir) {
        String sql;
        if (idExcluir != null) {
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ? AND id_empleado != ? AND estado = 'ACTIVO'";
        } else {
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ? AND estado = 'ACTIVO'";
        }
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, numeroDocumento);
            if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar numero documento: " + e.getMessage());
        }
        return false;
    }

    // ===== MAPEAR =====
    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre"));
        e.setCargo(rs.getString("cargo"));
        e.setUsuario(rs.getString("usuario"));
        e.setPassword(rs.getString("password"));
        e.setIdTipoDocumento(rs.getString("id_tipo_documento"));
        e.setNombreTipoDocumento(rs.getString("documento"));
        e.setNumeroDocumento(rs.getString("numero_documento"));
        e.setTelefono(rs.getString("telefono"));
        e.setDireccion(rs.getString("direccion"));
        e.setEstado(rs.getString("estado"));
        return e;
    }
}