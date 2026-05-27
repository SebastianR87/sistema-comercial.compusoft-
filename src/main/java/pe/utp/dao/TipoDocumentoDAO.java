package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.TipoDocumento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
}