package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Especificacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EspecificacionDAO {

    private Connection conexion;

    public EspecificacionDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public boolean insertar(Especificacion e) {
        String sql = "INSERT INTO especificacion VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getIdEspecificacion());
            ps.setString(2, e.getIdProducto());
            ps.setString(3, e.getClave());
            ps.setString(4, e.getValor());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al insertar especificacion: " + ex.getMessage());
            return false;
        }
    }

    public List<Especificacion> listarPorProducto(String idProducto) {
        List<Especificacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM especificacion WHERE id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Especificacion e = new Especificacion();
                e.setIdEspecificacion(rs.getString("id_especificacion"));
                e.setIdProducto(rs.getString("id_producto"));
                e.setClave(rs.getString("clave"));
                e.setValor(rs.getString("valor"));
                lista.add(e);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar especificaciones: " + e.getMessage());
        }
        return lista;
    }

    public boolean eliminarPorProducto(String idProducto) {
        String sql = "DELETE FROM especificacion WHERE id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar especificaciones: " + e.getMessage());
            return false;
        }
    }
}
