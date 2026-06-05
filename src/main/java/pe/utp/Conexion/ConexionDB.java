package pe.utp.Conexion;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionDB {

    private static String motor;
    private static String server;
    private static String database;
    private static String user;
    private static String password;
    private static String url;

    static {
        try {
            InputStream input = ConexionDB.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");

            Properties props = new Properties();
            props.load(input);

            motor    = props.getProperty("motor");
            server   = props.getProperty("server");
            database = props.getProperty("database");
            user     = props.getProperty("user");
            password = props.getProperty("password");

            if (motor.equalsIgnoreCase("mysql")) {
                url = "jdbc:mysql://" + server + ":3306/" + database
                        + "?useSSL=false&serverTimezone=UTC";
            } else {
                url = "jdbc:sqlserver://" + server + ":1433;"
                        + "databaseName=" + database + ";"
                        + "encrypt=false;"
                        + "trustServerCertificate=true;";
            }

        } catch (Exception e) {
            System.out.println("Error al leer config.properties: " + e.getMessage());
        }
    }

    public static Connection getConexion() {
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexion exitosa a " + database + " [" + motor + "]");
            return conn;
        } catch (SQLException e) {
            System.out.println("Error de conexion: " + e.getMessage());
            return null;
        }
    }
}