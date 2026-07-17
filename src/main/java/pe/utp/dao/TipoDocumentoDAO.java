package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.TipoDocumento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.utp.Conexion.QueryHelper;


public class TipoDocumentoDAO {

    private Connection conexion;

    public TipoDocumentoDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<TipoDocumento> listar() {
        List<TipoDocumento> lista = new ArrayList<>();
        String sql = "SELECT * FROM tipo_documento";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TipoDocumento td = new TipoDocumento();
                td.setIdTipoDocumento(rs.getString("id_tipo_documento"));
                td.setDocumento(rs.getString("documento"));
                lista.add(td);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar tipo documento: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(TipoDocumento td) {
        String sql = "INSERT INTO tipo_documento VALUES (?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, td.getIdTipoDocumento());
            ps.setString(2, td.getDocumento());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(TipoDocumento td) {
        String sql = "UPDATE tipo_documento SET documento=? WHERE id_tipo_documento=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, td.getDocumento());
            ps.setString(2, td.getIdTipoDocumento());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM tipo_documento WHERE id_tipo_documento=?";
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

    public boolean existeDocumento(String documento, String idExcluir) {
        String sql;
        if (idExcluir != null) {
            sql = "SELECT COUNT(*) FROM tipo_documento " +
                    "WHERE documento = ? AND id_tipo_documento != ?";
        } else {
            sql = "SELECT COUNT(*) FROM tipo_documento WHERE documento = ?";
        }
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, documento);
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
                "SELECT id_tipo_documento FROM tipo_documento " +
                        "ORDER BY id_tipo_documento DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_tipo_documento");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
}