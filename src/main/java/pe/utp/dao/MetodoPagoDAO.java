package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.MetodoPago;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetodoPagoDAO {

    private Connection conexion;

    public MetodoPagoDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    public List<MetodoPago> listar() {
        List<MetodoPago> lista = new ArrayList<>();
        String sql = "SELECT * FROM metodopago";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MetodoPago mp = new MetodoPago();
                mp.setIdMetodoPago(rs.getString("id_metodopago"));
                mp.setMetodoDePago(rs.getString("metodo_de_pago"));
                lista.add(mp);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar metodo pago: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(MetodoPago mp) {
        String sql = "INSERT INTO metodopago VALUES (?, ?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, mp.getIdMetodoPago());
            ps.setString(2, mp.getMetodoDePago());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(MetodoPago mp) {
        String sql = "UPDATE metodopago SET metodo_de_pago=? WHERE id_metodopago=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, mp.getMetodoDePago());
            ps.setString(2, mp.getIdMetodoPago());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM metodopago WHERE id_metodopago=?";
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

    public String obtenerUltimoId() {
        String sql = "SELECT TOP 1 id_metodopago FROM metodopago ORDER BY id_metodopago DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_metodopago");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
}