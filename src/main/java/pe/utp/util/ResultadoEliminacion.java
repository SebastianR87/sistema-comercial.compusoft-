package pe.utp.util;

import javafx.scene.control.Alert;

public final class ResultadoEliminacion {

    private final boolean permitido;
    private final String titulo;
    private final String detalle;

    private ResultadoEliminacion(boolean permitido, String titulo, String detalle) {
        this.permitido = permitido;
        this.titulo = titulo;
        this.detalle = detalle;
    }

    public static ResultadoEliminacion permitir() {
        return new ResultadoEliminacion(true, null, null);
    }

    public static ResultadoEliminacion bloquear(String titulo, String detalle) {
        return new ResultadoEliminacion(false, titulo, detalle);
    }

    public boolean isPermitido() {
        return permitido;
    }

    public void mostrarAlerta() {
        if (permitido) return;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No se puede eliminar");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}
