package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
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
        // Antes tenía el "LIMIT 1" de MySQL escrito directo en el SQL,
        // a diferencia de TODOS los demás DAO que pasan por
        // QueryHelper.limitar() para que también funcione con SQL
        // Server (que usa "SELECT TOP 1" en vez de "LIMIT"). Con
        // motor=sqlserver esta consulta fallaba silenciosamente
        // (SQLException atrapada, retorna null), así que
        // generarId() en el controlador nunca detectaba el último ID
        // real y siempre proponía "EMP001" -- que ya existe desde el
        // primer empleado creado, haciendo fallar el alta de
        // cualquier empleado siguiente.
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

    // ===== CONTAR ADMINISTRADORES ACTIVOS =====
    // Usado antes de desactivar/eliminar/degradar un Administrador,
    // para no dejar el sistema sin ningún admin que pueda entrar a
    // revertir el cambio (ver EmpleadoController/EmpleadoModalController).
    public int contarAdministradoresActivos() {
        String sql = "SELECT COUNT(*) FROM empleado " +
                "WHERE cargo = 'Administrador' AND estado = 'ACTIVO'";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error al contar administradores activos: " + e.getMessage());
        }
        return 0;
    }

    // ===== EXISTE NUMERO DOCUMENTO =====
    // Antes filtraba "AND estado = 'ACTIVO'": solo miraba empleados
    // activos, así que un documento de un empleado desactivado
    // quedaba "libre" para asignarlo a un empleado nuevo. Si luego se
    // reactivaba al primer empleado, terminaban DOS empleados activos
    // con el mismo número de documento a la vez, sin ninguna
    // validación que lo detecte. El número de documento debe ser
    // único sin importar si el dueño está activo o no.
    public boolean existeNumeroDocumento(String numeroDocumento, String idExcluir) {
        String sql;
        if (idExcluir != null) {
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ? AND id_empleado != ?";
        } else {
            sql = "SELECT COUNT(*) FROM empleado " +
                    "WHERE numero_documento = ?";
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