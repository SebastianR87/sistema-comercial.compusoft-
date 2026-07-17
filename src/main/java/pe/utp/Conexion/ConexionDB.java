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

            if (input == null) {
                System.out.println("No se encontró config.properties");
            } else {
                Properties props = new Properties();
                props.load(input);

                motor    = props.getProperty("motor");
                server   = props.getProperty("server");
                database = props.getProperty("database");
                user     = props.getProperty("user");
                password = props.getProperty("password");

                if (motor.equalsIgnoreCase("mysql")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    url = "jdbc:mysql://" + server + ":3306/" + database
                            + "?useSSL=false&serverTimezone=UTC";
                } else {
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    url = "jdbc:sqlserver://" + server + ":1433;"
                            + "databaseName=" + database + ";"
                            + "encrypt=false;"
                            + "trustServerCertificate=true;";
                }

                System.out.println("Config cargada: " + motor + " - " + database);
            }

        } catch (Exception e) {
            System.out.println("Error al leer config.properties: " + e.getMessage());
        }
    }

    // Antes se creaba una Connection física NUEVA en cada llamada y nunca
    // se cerraba en ningún DAO -> fuga de conexiones sin límite (cada
    // navegación entre pantallas abría más conexiones contra la BD hasta
    // agotar el límite del motor). Ahora se reutiliza una única conexión
    // (patrón singleton perezoso) y solo se abre una nueva si la anterior
    // nunca existió o quedó cerrada/caída.
    private static Connection conexion;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(url, user, password);
                System.out.println("Conexion exitosa a " + database + " [" + motor + "]");
            }
            return conexion;
        } catch (SQLException e) {
            System.out.println("Error de conexion: " + e.getMessage());
            return null;
        }
    }
}