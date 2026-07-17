package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.model.Compatibilidad;
import pe.utp.model.Producto;

/**
 * Controlador del modal de solo lectura para una regla de compatibilidad.
 No tiene modo de edición: al presionar "Editar" en la tabla, la edición ocurre en la misma tarjeta
 "Nueva Regla" de la pantalla principal (no aquí). Este modal existe únicamente para el botón "Ver".*/
public class CompatibilidadVerModalController {

    @FXML private Label lblNombreCabecera;
    @FXML private TextField txtComponenteA;
    @FXML private Label lblTipoA;
    @FXML private TextField txtComponenteB;
    @FXML private Label lblTipoB;
    @FXML private VBox panelEstado;
    @FXML private Label lblEstadoIcono;
    @FXML private Label lblEstadoTexto;
    @FXML private TextArea txtRestriccion;

    public void setDatos(Compatibilidad c) {
        Producto p1 = c.getProducto1();
        Producto p2 = c.getProducto2();

        String nombre1 = p1 != null ? p1.getNombre() : "";
        String nombre2 = p2 != null ? p2.getNombre() : "";
        lblNombreCabecera.setText(nombre1 + "  ⇄  " + nombre2);

        txtComponenteA.setText(nombre1);
        lblTipoA.setText(p1 != null && p1.getCategoria() != null
                ? p1.getCategoria().getNombre() : "");

        txtComponenteB.setText(nombre2);
        lblTipoB.setText(p2 != null && p2.getCategoria() != null
                ? p2.getCategoria().getNombre() : "");

        aplicarEstado(c.getEstado());

        String restriccion = c.getRestriccion();
        txtRestriccion.setText(restriccion == null || restriccion.isEmpty()
                ? "Sin restricciones registradas." : restriccion);
    }

    private void aplicarEstado(String estado) {
        String icono;
        String colorTexto;
        String colorFondo;
        switch (estado == null ? "" : estado) {
            case Compatibilidad.COMPATIBLE -> {
                icono = "✅"; colorTexto = "#2DC653"; colorFondo = "#EEF8F0";
            }
            case Compatibilidad.NO_COMPATIBLE -> {
                icono = "❌"; colorTexto = "#E94560"; colorFondo = "#FFF0F2";
            }
            case Compatibilidad.CONDICIONAL -> {
                icono = "⚠"; colorTexto = "#E67E00"; colorFondo = "#FFF8F0";
            }
            default -> {
                icono = "❓"; colorTexto = "#64748B"; colorFondo = "#F8FAFC";
            }
        }
        panelEstado.setStyle(
                "-fx-background-color: " + colorFondo + ";" +
                        "-fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: " + colorTexto + "40;" +
                        "-fx-border-width: 1; -fx-padding: 12;"
        );
        lblEstadoIcono.setText(icono);
        lblEstadoTexto.setText(estado == null ? "SIN REGLA" : estado);
        lblEstadoTexto.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + colorTexto + ";" +
                        "-fx-text-fill: white; -fx-background-radius: 4;" +
                        "-fx-padding: 2 8;"
        );
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) txtRestriccion.getScene().getWindow();
        stage.close();
    }
}
