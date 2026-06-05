package pe.utp.Conexion;
import java.io.InputStream;
import java.util.Properties;

public class QueryHelper {

    private static String motor;

    static {
        try {
            InputStream input = QueryHelper.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");

            Properties props = new Properties();
            props.load(input);
            motor = props.getProperty("motor");

        } catch (Exception e) {
            System.out.println("Error al leer motor en QueryHelper: " + e.getMessage());
            motor = "mysql"; // valor por defecto
        }
    }

    /**
     * Devuelve la query con el límite correcto según el motor.
     * Ejemplo: limitar("SELECT id FROM empleado ORDER BY id DESC", 1)
     * MySQL:      SELECT id FROM empleado ORDER BY id DESC LIMIT 1
     * SQLServer:  SELECT TOP 1 id FROM empleado ORDER BY id DESC
     */
    public static String limitar(String query, int cantidad) {
        if (motor.equalsIgnoreCase("mysql")) {
            return query + " LIMIT " + cantidad;
        } else {
            // Inserta TOP N después del primer SELECT
            return query.replaceFirst(
                    "(?i)SELECT",
                    "SELECT TOP " + cantidad
            );
        }
    }
}