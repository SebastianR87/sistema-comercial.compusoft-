package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    private Connection conexion;

    public ProveedorDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<Proveedor> listar() {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM proveedor";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearProveedor(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar proveedores: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Proveedor p) {
        String sql = "INSERT INTO proveedor VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, p.getIdProveedor());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getRuc());
            ps.setString(5, p.getDireccion());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar proveedor: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Proveedor p) {
        String sql = "UPDATE proveedor SET nombre=?, telefono=?, RUC=?, direccion=? " +
                "WHERE id_proveedor=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getTelefono());
            ps.setString(3, p.getRuc());
            ps.setString(4, p.getDireccion());
            ps.setString(5, p.getIdProveedor());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar proveedor: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM proveedor WHERE id_proveedor = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar proveedor: " + e.getMessage());
            return false;
        }
    }

    public Proveedor buscarPorId(String id) {
        String sql = "SELECT * FROM proveedor WHERE id_proveedor = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapearProveedor(rs);
        } catch (SQLException e) {
            System.out.println("Error al buscar proveedor: " + e.getMessage());
        }
        return null;
    }

    public String obtenerUltimoId() {
        String sql = "SELECT TOP 1 id_proveedor FROM proveedor ORDER BY id_proveedor DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_proveedor");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM proveedor WHERE nombre LIKE ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearProveedor(rs));
        } catch (SQLException e) {
            System.out.println("Error al buscar proveedor: " + e.getMessage());
        }
        return lista;
    }

    private Proveedor mapearProveedor(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getString("id_proveedor"));
        p.setNombre(rs.getString("nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setRuc(rs.getString("RUC"));
        p.setDireccion(rs.getString("direccion"));
        return p;
    }
}
