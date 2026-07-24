package pe.utp.Conexion;

public class QueryHelper {

    /**
     * Devuelve la query con el límite correcto para SQL Server.
     * Ejemplo: limitar("SELECT id FROM empleado ORDER BY id DESC", 1)
     * -> SELECT TOP 1 id FROM empleado ORDER BY id DESC
     */
    public static String limitar(String query, int cantidad) {
        // Inserta TOP N después del primer SELECT
        return query.replaceFirst(
                "(?i)SELECT",
                "SELECT TOP " + cantidad
        );
    }

    /**
     * Devuelve la expresión SQL para restar días a la fecha actual.
     * Se usa para calcular vencimientos sin necesidad de columna extra.
     * SQL Server: DATEADD(DAY, -30, GETDATE())
     * Ejemplo de uso:
     * "WHERE fecha < " + QueryHelper.restarDias(30)
     */
    public static String restarDias(int dias) {
        return "DATEADD(DAY, -" + dias + ", GETDATE())";
    }

    /**
     * Devuelve la expresión SQL para truncar una fecha-hora a solo
     * fecha (sin hora), usada para agrupar movimientos por día.
     * SQL Server: CAST(columna AS DATE)
     */
    public static String fechaSolo(String columna) {
        return "CAST(" + columna + " AS DATE)";
    }

}
