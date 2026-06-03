package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;

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

    public List<Cliente> listar(){
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT c.*, t.documento " +
                "FROM cliente c " +
                "INNER JOIN tipo_documento t ON c.id_tipo_documento = t.id_tipo_documento";

        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        }catch (SQLException e){
            System.out.println("Error al listar Cliente" + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Cliente c){
        String sql = "INSERT INTO cliente VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, c.getIdCliente());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getCorreo());
            ps.setString(5, c.getDireccion());
            ps.setString(6, c.getNumeroDocumento());
            ps.setString(7, c.getTipoDocumento().getIdTipoDocumento());
            ps.executeUpdate();
            return true;
        }catch (SQLException e){
            System.out.println("Error al insertar Cliente" + e.getMessage());
            return false;
        }
    }

    public boolean actualizar (Cliente c){
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
        }catch (SQLException e){
            System.out.println("Error al actualizar Cliente" + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM cliente WHERE id_cliente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar Cliente: " + e.getMessage());
            return false;
        }
    }

    public Cliente buscarPorId(String id) {
        String sql = "SELECT c.*, t.documento " +
                "FROM cliente c " +
                "INNER JOIN tipo_documento t ON c.id_tipo_documento = t.id_tipo_documento " +
                "WHERE c.id_cliente = ?";

        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1,id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapearCliente(rs);
        } catch (SQLException e){
            System.out.println("Error al buscar Cliente" + e.getMessage());
        }
        return null;
    }

    public List<Cliente> buscarPorNombre(String nombre) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT c.*, t.documento " +
                "FROM cliente c " +
                "INNER JOIN tipo_documento t ON c.id_tipo_documento = t.id_tipo_documento " +
                "WHERE c.nombre LIKE ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearCliente(rs));
        } catch (SQLException e) {
            System.out.println("Error al buscar cliente: " + e.getMessage());
        }
        return lista;
    }

    public String obtenerUltimoId() {
        String sql = "SELECT TOP 1 id_cliente FROM cliente ORDER BY id_cliente DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_cliente");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }


    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        TipoDocumento td = new TipoDocumento();
        td.setIdTipoDocumento(rs.getString("id_tipo_documento"));
        td.setDocumento(rs.getString("documento"));

        Cliente c = new Cliente();
        c.setIdCliente(rs.getString("id_cliente"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setDireccion(rs.getString("direccion"));
        c.setNumeroDocumento(rs.getString("numero_documento"));
        c.setTipoDocumento(td);
        return c;
    }

}
