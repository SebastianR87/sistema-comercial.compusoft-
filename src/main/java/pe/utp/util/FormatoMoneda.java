package pe.utp.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.Locale;

/**
 * Formateo uniforme de valores monetarios en toda la aplicación.
 *
 * Antes, varias tablas (Productos, Ventas, Compras, Cotizaciones)
 * mostraban precios/subtotales/totales como un Double crudo, sin
 * ningún cellFactory que los formatee: quedaban a merced del
 * Double.toString() por defecto de JavaFX, que a veces muestra un
 * solo decimal ("1250.0") y otras veces arrastra artefactos de punto
 * flotante de cantidad*precio ("54.49999999999999"). Reportes y
 * Kárdex ya formateaban bien con String.format("S/ %.2f", valor); esta
 * clase centraliza ese mismo formato para que el resto de tablas del
 * sistema lo reutilicen en vez de reimplementar la misma lógica en
 * cada controller.
 */
public final class FormatoMoneda {

    private FormatoMoneda() {
    }

    /** "S/ 120.00", "S/ 54.50", "S/ 999.90": siempre 2 decimales. */
    public static String formatear(double valor) {
        return String.format(Locale.US, "S/ %.2f", valor);
    }

    /**
     * Cell factory reutilizable para columnas TableColumn&lt;?, Double&gt;
     * que representan dinero: alinea a la derecha y aplica
     * formatear(). Uso:
     * <pre>colSubtotal.setCellFactory(FormatoMoneda.celda());</pre>
     */
    public static <S> Callback<TableColumn<S, Double>, TableCell<S, Double>> celda() {
        return columna -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setText(null);
                } else {
                    setText(formatear(valor));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        };
    }
}
