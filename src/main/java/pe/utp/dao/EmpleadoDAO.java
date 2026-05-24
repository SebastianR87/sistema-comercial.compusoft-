package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {
    private Connection conexion;

    public EmpleadoDAO(){
        this.conexion = ConexionDB.getConexion();
    }

    public Empleado login(String usuario, String password){
        String sql = "SELECT * FROM empleado WHERE usuario = ? AND password = ? ";

        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, usuario);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                Empleado e = new Empleado();
                e.setIdEmpleado(rs.getString("id_empleado"));
                e.setNombre(rs.getString("nombre"));
                e.setCargo(rs.getString("cargo"));
                e.setUsuario(rs.getString("usuario"));
                e.setPassword(rs.getString("password"));
                e.setDni(rs.getString("dni"));
                return e;
            }
        }catch (Exception e){
            System.out.println("Error en login: "+e.getMessage());
        }
        return null;
    }

    public List<Empleado> listar(){
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT * FROM empleado";

        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                lista.add(mapearEmpleado(rs));
            }
        }catch (SQLException e){
            System.out.println("Error al listar empleados" + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Empleado e){
        String sql = "INSERT INTO empleado VALUES (?,?,?,?,?,?)";
        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getIdEmpleado());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getCargo());
            ps.setString(4, e.getUsuario());
            ps.setString(5, e.getPassword());
            ps.setString(6, e.getDni());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex){
            System.out.println("Error al insertar empleado" + ex.getMessage());
            return false;
        }
    }

    public boolean actualizar(Empleado e) {
        String sql = "UPDATE empleado SET nombre=?, cargo=?, usuario=?, password=?, dni=? WHERE id_empleado=?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getCargo());
            ps.setString(3, e.getUsuario());
            ps.setString(4, e.getPassword());
            ps.setString(5, e.getDni());
            ps.setString(6, e.getIdEmpleado());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al actualizar empleado: " + ex.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id) {
        String sql = "DELETE FROM empleado WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println("Error al eliminar empleado: " + ex.getMessage());
            return false;
        }
    }

    public Empleado buscarPorId(String id) {
        String sql = "SELECT * FROM empleado WHERE id_empleado = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearEmpleado(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar empleado: " + e.getMessage());
        }
        return null;
    }



    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre"));
        e.setCargo(rs.getString("cargo"));
        e.setUsuario(rs.getString("usuario"));
        e.setPassword(rs.getString("password"));
        e.setDni(rs.getString("dni"));
        return e;
    }
}
