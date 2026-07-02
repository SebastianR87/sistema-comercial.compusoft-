package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.TipoComprobante;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.utp.Conexion.QueryHelper;

public class TipoComprobanteDAO {

    private Connection conexion;

    public TipoComprobanteDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<TipoComprobante> listar() {
        List<TipoComprobante> lista = new ArrayList<>();
        String sql = "SELECT * FROM TipoComprobante";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TipoComprobante tc = new TipoComprobante();
                tc.setIdTipoComprobante(rs.getString("id_tipo_comprobante"));
                tc.setNombre(rs.getString("nombre"));
                lista.add(tc);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar tipo comprobante: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(TipoComprobante tc) {
        String sql = "INSERT INTO TipoComprobante VALUES (?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, tc.getIdTipoComprobante());
            ps.setString(2, tc.getNombre());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(TipoComprobante tc) {
        String sql = "UPDATE TipoComprobante SET nombre=? WHERE id_tipo_comprobante=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, tc.getNombre());
            ps.setString(2, tc.getIdTipoComprobante());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM TipoComprobante WHERE id_tipo_comprobante=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al eliminar: " + e.getMessage());
            return false;
        }
    }

    public boolean existeNombre(String nombre, String idExcluir) {
        String sql;
        if (idExcluir != null) {
            sql = "SELECT COUNT(*) FROM TipoComprobante " +
                    "WHERE nombre = ? AND id_tipo_comprobante != ?";
        } else {
            sql = "SELECT COUNT(*) FROM TipoComprobante WHERE nombre = ?";
        }
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, nombre);
            if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar nombre: " + e.getMessage());
        }
        return false;
    }

    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_tipo_comprobante FROM TipoComprobante " +
                        "ORDER BY id_tipo_comprobante DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_tipo_comprobante");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
}