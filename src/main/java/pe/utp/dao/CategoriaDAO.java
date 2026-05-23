package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class CategoriaDAO {

    private Connection conexion;

    public CategoriaDAO(){
        this.conexion = ConexionDB.getConexion();
    }

    public boolean insertar(Categoria categoria){
        String sql = "INSERT INTO categoria (id_categoria, nombre) VALUES (?,?)";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, categoria.getIdCategoria());
            ps.setString(2, categoria.getNombre());
            ps.executeUpdate();
            return true;
        } catch (SQLException e){
            System.out.println("Error al insertar el categoria" + e.getMessage());
            return false;
        }

    }

    public List<Categoria> Listar(){
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM categoria";
        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Categoria c = new Categoria();
                c.setIdCategoria(rs.getString("id_categoria"));
                c.setNombre(rs.getString("nombre"));
                lista.add(c);

            }
        }catch(SQLException e){
            System.out.println("Error al listar el categoria" + e.getMessage());
        }
        return lista;
    }

    public Categoria buscarPorId(String id){
        Categoria categoria = null;
        String sql = "SELECT * FROM categoria WHERE id_categoria = ?";
        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                categoria = new Categoria();
                categoria.setIdCategoria(rs.getString("id_categoria"));
                categoria.setNombre(rs.getString("nombre"));
            }

        }catch (SQLException e){
            System.out.println("Error al buscar el categoria" + e.getMessage());
        }
        return categoria;
    }

    public boolean actualizar(Categoria categoria){
        String sql = "UPDATE categoria SET nombre = ? WHERE id_categoria = ?";
        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getIdCategoria());
            ps.executeUpdate();
            return true;

        }catch (SQLException e){
            System.out.println("Error al actualizar el categoria" + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String id){
        String sql = "DELETE FROM categoria WHERE id_categoria = ?";
        try{
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        }catch (SQLException e){
            System.out.println("Error al eliminar el categoria" + e.getMessage());
            return false;
        }
    }
}
