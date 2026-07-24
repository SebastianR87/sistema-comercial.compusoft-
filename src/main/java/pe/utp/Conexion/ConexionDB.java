package pe.utp.Conexion;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionDB {

    private static String database;
    private static String user;
    private static String password;
    private static String url;

    static {
        try {
            InputStream input = ConexionDB.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");

            if (input == null) {
                System.out.println("No se encontró config.properties");
            } else {
                Properties props = new Properties();
                props.load(input);

                database = props.getProperty("database");
                user     = props.getProperty("user");
                password = props.getProperty("password");
                url      = props.getProperty("url");

                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

                System.out.println("Config cargada: SQL Server - " + database);
            }

        } catch (Exception e) {
            System.out.println("Error al leer config.properties: " + e.getMessage());
        }
    }

    private static Connection conexion;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(url, user, password);
                System.out.println("Conexion exitosa a " + database + " [SQL Server]");
            }
            return conexion;
        } catch (SQLException e) {
            System.out.println("Error de conexion: " + e.getMessage());
            return null;
        }
    }
}