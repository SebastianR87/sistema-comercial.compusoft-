package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Categoria;
import pe.utp.model.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.utp.Conexion.QueryHelper;

public class ProductoDAO {

    private Connection conexion;

    public ProductoDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<Producto> listar() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nombre as nombre_categoria " +
                "FROM producto p " +
                "INNER JOIN categoria c ON p.id_categoria = c.id_categoria";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar productos: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Producto p) {
        String sql = "INSERT INTO producto (id_producto, id_categoria, nombre, " +
                "descripcion, precio_compra, precio_venta, stock, " +
                "stock_minimo, stock_maximo, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, p.getIdProducto());
            ps.setString(2, p.getCategoria().getIdCategoria());
            ps.setString(3, p.getNombre());
            ps.setString(4, p.getDescripcion());
            ps.setDouble(5, p.getPrecioCompra());
            ps.setDouble(6, p.getPrecioVenta());
            ps.setInt(7, p.getStock());
            ps.setInt(8, p.getStockMinimo());
            ps.setInt(9, p.getStockMaximo());
            ps.setString(10, p.getEstado());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar producto: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Producto p) {
        String sql = "UPDATE producto SET " +
                "nombre = ?, " +
                "descripcion = ?, " +
                "precio_venta = ?, " +
                "stock_minimo = ?, " +
                "stock_maximo = ?, " +
                "estado = ? " +
                "WHERE id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecioVenta());
            ps.setInt(4, p.getStockMinimo());
            ps.setInt(5, p.getStockMaximo());
            ps.setString(6, p.getEstado());
            ps.setString(7, p.getIdProducto());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            return false;
        }
    }

    public Producto buscarPorId(String id) {
        String sql = "SELECT p.*, c.nombre as nombre_categoria " +
                "FROM producto p " +
                "INNER JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "WHERE p.id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearProducto(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar producto: " + e.getMessage());
        }
        return null;
    }

    public List<Producto> listarPorCategoria(String idCategoria) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nombre as nombre_categoria " +
                "FROM producto p " +
                "INNER JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "WHERE p.id_categoria = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idCategoria);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar por categoria: " + e.getMessage());
        }
        return lista;
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getString("id_categoria"));
        categoria.setNombre(rs.getString("nombre_categoria"));

        Producto p = new Producto();
        p.setIdProducto(rs.getString("id_producto"));
        p.setCategoria(categoria);
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecioCompra(rs.getDouble("precio_compra"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setStock(rs.getInt("stock"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setStockMaximo(rs.getInt("stock_maximo"));
        p.setEstado(rs.getString("estado"));
        return p;
    }

    public String obtenerUltimoId(String idCategoria) {
        // El ID del producto incluye la categoría: PROC001, RAM001, etc.
        String sql = QueryHelper.limitar(
                "SELECT id_producto FROM producto WHERE id_categoria = ? ORDER BY id_producto DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idCategoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_producto");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }
}