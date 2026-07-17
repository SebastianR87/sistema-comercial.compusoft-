package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;
import pe.utp.Conexion.QueryHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private Connection conexion;

    public ClienteDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT c.*, t.documento " +
                "FROM cliente c " +
                "INNER JOIN tipo_documento t " +
                "ON c.id_tipo_documento = t.id_tipo_documento " +
                "ORDER BY c.nombre ASC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearCliente(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar clientes: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Cliente c) {
        String sql = "INSERT INTO cliente " +
                "(id_cliente, nombre, telefono, correo, " +
                "direccion, numero_documento, id_tipo_documento) " +
                "VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, c.getIdCliente());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getCorreo());
            ps.setString(5, c.getDireccion());
            ps.setString(6, c.getNumeroDocumento());
            // Guardamos el ID del tipo, no el nombre
            // porque la BD tiene una FK a tipo_documento
            ps.setString(7, c.getTipoDocumento().getIdTipoDocumento());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar cliente: " + e.getMessage());
            return false;
        }
    }


    public boolean actualizar(Cliente c) {
        String sql = "UPDATE cliente SET nombre=?, telefono=?, correo=?, " +
                "direccion=?, numero_documento=?, id_tipo_documento=? " +
                "WHERE id_cliente=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getCorreo());
            ps.setString(4, c.getDireccion());
            ps.setString(5, c.getNumeroDocumento());
            ps.setString(6, c.getTipoDocumento().getIdTipoDocumento());
            ps.setString(7, c.getIdCliente());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM cliente WHERE id_cliente = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    /** Verifica si el cliente tiene ventas o cotizaciones registradas.**/
    public boolean tieneMovimientos(String idCliente) {
        String[] tablas = {"venta", "cotizacion"};
        for (String tabla : tablas) {
            String sql = "SELECT COUNT(*) FROM " + tabla +
                    " WHERE id_cliente = ?";
            try {
                PreparedStatement ps = conexion.prepareStatement(sql);
                ps.setString(1, idCliente);
                ResultSet rs = ps.executeQuery();
                // Si COUNT(*) > 0 tiene movimientos, no se puede eliminar
                if (rs.next() && rs.getInt(1) > 0) return true;
            } catch (SQLException e) {
                System.out.println("Error al verificar movimientos en "
                        + tabla + ": " + e.getMessage());
            }
        }
        return false;
    }

    public boolean existeNumeroDocumento(String numeroDocumento,
                                         String idExcluir) {
        String sql;
        if (idExcluir != null) {
            // Al editar: excluye al propio cliente de la verificación
            sql = "SELECT COUNT(*) FROM cliente " +
                    "WHERE numero_documento = ? " +
                    "AND id_cliente != ?";
        } else {
            // Al crear: verifica que no exista ese número
            sql = "SELECT COUNT(*) FROM cliente " +
                    "WHERE numero_documento = ?";
        }

        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, numeroDocumento);
            if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar documento: " + e.getMessage());
        }
        return false;
    }

    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_cliente FROM cliente ORDER BY id_cliente DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_cliente");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }

    /** Verifica si ya existe un cliente con ese correo. */
    public boolean existeCorreo(String correo, String idExcluir) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE correo = ?";
        if (idExcluir != null) {
            sql += " AND id_cliente != ?";
        }
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, correo);
            if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar correo: " + e.getMessage());
        }
        return false;
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        // Construye el objeto TipoDocumento con los datos del JOIN
        TipoDocumento td = new TipoDocumento();
        td.setIdTipoDocumento(rs.getString("id_tipo_documento"));
        td.setDocumento(rs.getString("documento"));

        Cliente c = new Cliente();
        c.setIdCliente(rs.getString("id_cliente"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setDireccion(rs.getString("direccion"));
        c.setTipoDocumento(td);
        c.setNumeroDocumento(rs.getString("numero_documento"));
        return c;
    }

}
