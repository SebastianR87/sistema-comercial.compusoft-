package pe.utp.security;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;

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

    public static void denegado() {
        new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING,
                "No tienes permiso para realizar esta acción."
        ).showAndWait();
    }
}
