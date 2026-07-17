package pe.utp.security;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import pe.utp.dialog.CSDialog;

public final class PermisoUtil {

    private PermisoUtil() {}

    public static void ocultar(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(false);
            nodo.setManaged(false);
        }
    }

    public static void ocultarColumna(TableColumn<?, ?> columna) {
        columna.setVisible(false);
        columna.setMaxWidth(0);
        columna.setMinWidth(0);
        columna.setPrefWidth(0);
    }

    // Ejemplo de reemplazo de Alert por CSDialog en un punto centralizado:
    // como denegado() ya se llamaba desde una veintena de controllers
    // (cada verificación de permiso en el sistema pasa por aquí), este
    // único cambio actualiza la apariencia de TODOS esos avisos de golpe,
    // sin tocar ninguno de los sitios que lo invocan.
    public static void denegado() {
        CSDialog.warning("Permiso denegado", "No tienes permiso para realizar esta acción.");
    }
}
