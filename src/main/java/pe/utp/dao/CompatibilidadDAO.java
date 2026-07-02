package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
import pe.utp.model.Compatibilidad;
import pe.utp.model.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompatibilidadDAO {

    private Connection conexion;

    public CompatibilidadDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    /**
     * Lista todas las reglas de compatibilidad.
     * Hace JOIN con producto dos veces para obtener
     * el nombre de ambos productos.
     */
    public List<Compatibilidad> listar() {
        List<Compatibilidad> lista = new ArrayList<>();
        String sql =
                "SELECT dc.id_detalle, dc.id_producto1, dc.id_producto2, " +
                        "dc.estado, dc.restriccion, " +
                        "p1.nombre AS nombre1, c1.nombre AS cat1, " +
                        "p2.nombre AS nombre2, c2.nombre AS cat2 " +
                        "FROM detallecompatibilidad dc " +
                        "INNER JOIN producto p1 ON dc.id_producto1 = p1.id_producto " +
                        "INNER JOIN categoria c1 ON p1.id_categoria = c1.id_categoria " +
                        "INNER JOIN producto p2 ON dc.id_producto2 = p2.id_producto " +
                        "INNER JOIN categoria c2 ON p2.id_categoria = c2.id_categoria " +
                        "ORDER BY dc.estado, p1.nombre";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar compatibilidad: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Verifica si existe una regla entre dos productos.
     * La relación es bidireccional: A↔B es igual que B↔A.
     * Retorna null si no existe ninguna regla.
     */
    public Compatibilidad verificar(String idP1, String idP2) {
        String sql =
                "SELECT dc.id_detalle, dc.id_producto1, dc.id_producto2, " +
                        "dc.estado, dc.restriccion, " +
                        "p1.nombre AS nombre1, c1.nombre AS cat1, " +
                        "p2.nombre AS nombre2, c2.nombre AS cat2 " +
                        "FROM detallecompatibilidad dc " +
                        "INNER JOIN producto p1 ON dc.id_producto1 = p1.id_producto " +
                        "INNER JOIN categoria c1 ON p1.id_categoria = c1.id_categoria " +
                        "INNER JOIN producto p2 ON dc.id_producto2 = p2.id_producto " +
                        "INNER JOIN categoria c2 ON p2.id_categoria = c2.id_categoria " +
                        "WHERE (dc.id_producto1 = ? AND dc.id_producto2 = ?) " +
                        "OR    (dc.id_producto1 = ? AND dc.id_producto2 = ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idP1); ps.setString(2, idP2);
            ps.setString(3, idP2); ps.setString(4, idP1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) {
            System.out.println("Error al verificar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si ya existe una regla entre dos productos
     * para evitar duplicados al insertar.
     */
    public boolean existeRegla(String idP1, String idP2) {
        String sql =
                "SELECT COUNT(*) FROM detallecompatibilidad " +
                        "WHERE (id_producto1 = ? AND id_producto2 = ?) " +
                        "OR    (id_producto1 = ? AND id_producto2 = ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idP1); ps.setString(2, idP2);
            ps.setString(3, idP2); ps.setString(4, idP1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar existencia: " + e.getMessage());
        }
        return false;
    }

    public boolean insertar(Compatibilidad c) {
        String sql =
                "INSERT INTO detallecompatibilidad " +
                        "(id_detalle, id_producto1, id_producto2, estado, restriccion) " +
                        "VALUES (?,?,?,?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, c.getIdDetalle());
            ps.setString(2, c.getProducto1().getIdProducto());
            ps.setString(3, c.getProducto2().getIdProducto());
            ps.setString(4, c.getEstado());
            ps.setString(5, c.getRestriccion());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Compatibilidad c) {
        String sql =
                "UPDATE detallecompatibilidad SET " +
                        "estado = ?, restriccion = ? " +
                        "WHERE id_detalle = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, c.getEstado());
            ps.setString(2, c.getRestriccion());
            ps.setString(3, c.getIdDetalle());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String idDetalle) {
        String sql =
                "DELETE FROM detallecompatibilidad WHERE id_detalle = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idDetalle);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar: " + e.getMessage());
            return false;
        }
    }

    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_detalle FROM detallecompatibilidad " +
                        "ORDER BY id_detalle DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_detalle");
        } catch (SQLException e) {
            System.out.println("Error al obtener id: " + e.getMessage());
        }
        return null;
    }

    private Compatibilidad mapear(ResultSet rs) throws SQLException {
        Compatibilidad c = new Compatibilidad();
        c.setIdDetalle(rs.getString("id_detalle"));
        c.setEstado(rs.getString("estado"));
        c.setRestriccion(rs.getString("restriccion"));

        Producto p1 = new Producto();
        p1.setIdProducto(rs.getString("id_producto1"));
        p1.setNombre(rs.getString("nombre1"));
        pe.utp.model.Categoria cat1 = new pe.utp.model.Categoria();
        cat1.setNombre(rs.getString("cat1"));
        p1.setCategoria(cat1);
        c.setProducto1(p1);

        Producto p2 = new Producto();
        p2.setIdProducto(rs.getString("id_producto2"));
        p2.setNombre(rs.getString("nombre2"));
        pe.utp.model.Categoria cat2 = new pe.utp.model.Categoria();
        cat2.setNombre(rs.getString("cat2"));
        p2.setCategoria(cat2);
        c.setProducto2(p2);

        return c;
    }
}
