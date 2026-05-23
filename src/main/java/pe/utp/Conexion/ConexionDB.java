package pe.utp.Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String SERVER = "localhost";
    private static final String DATABASE = "SISTEMA_COMPUSOFT";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456";

    private static final String URL =
            "jdbc:sqlserver://" + SERVER + ":1433;" +
                    "databaseName=" + DATABASE + ";" +
                    "encrypt=false;" +
                    "trustServerCertificate=true;";

    public static Connection getConexion() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexion exitosa a " + DATABASE);
            return conn;
        } catch (SQLException e) {
            System.out.println("Error de conexion: " + e.getMessage());
            return null;
        }
    }
}