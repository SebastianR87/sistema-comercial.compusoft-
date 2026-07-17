package pe.utp.util;

import java.time.LocalDate;

/**
 * Atajos de rango de fechas reutilizables ("Hoy", "Últimos 7 días",
 * "Este mes", "Este año"), usados por los filtros de Kárdex y
 * Reportes. Antes cada controller reimplementaba las mismas 4
 * fórmulas de fecha con su propio botón; centralizarlas aquí evita
 * que un futuro ajuste (por ejemplo, cambiar "Últimos 7 días" a
 * "Últimos 15 días") tenga que replicarse manualmente en cada
 * módulo que use el mismo atajo.
 *
 * Cada método devuelve un arreglo {desde, hasta} listo para asignar
 * directamente a un par de DatePicker.
 */
public final class RangoFechaUtil {

    private RangoFechaUtil() {
    }

    public static LocalDate[] hoy() {
        LocalDate hoy = LocalDate.now();
        return new LocalDate[]{hoy, hoy};
    }

    public static LocalDate[] ultimos7Dias() {
        LocalDate hoy = LocalDate.now();
        return new LocalDate[]{hoy.minusDays(6), hoy};
    }

    public static LocalDate[] esteMes() {
        LocalDate hoy = LocalDate.now();
        return new LocalDate[]{hoy.withDayOfMonth(1), hoy};
    }

    public static LocalDate[] esteAnio() {
        LocalDate hoy = LocalDate.now();
        return new LocalDate[]{hoy.withDayOfYear(1), hoy};
    }
}
