package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.model.Cotizacion;
import pe.utp.model.DetalleCotizacion;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CotizacionDetalleModalController {

    @FXML private Label lblIdCotizacion;
    @FXML private Label lblFecha;
    @FXML private Label lblCliente;
    @FXML private Label lblEmpleado;
    @FXML private Label lblEstado;
    @FXML private Label lblDescuento;
    @FXML private Label lblTotal;
    @FXML private VBox  panelProductos;

    public void cargarDatos(Cotizacion cot, List<DetalleCotizacion> detalle) {
        lblIdCotizacion.setText(cot.getIdCotizacion());
        lblFecha.setText(cot.getFecha().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        lblCliente.setText(cot.getCliente().getNombre());
        lblEmpleado.setText(cot.getEmpleado().getNombre());

        // Estado con color según valor
        lblEstado.setText(cot.getEstado());
        String colorEstado = switch (cot.getEstado()) {
            case Cotizacion.PENDIENTE  -> "#e67e00";
            case Cotizacion.ACEPTADA   -> "#2dc653";
            case Cotizacion.RECHAZADA  -> "#e94560";
            case Cotizacion.CONVERTIDA -> "#4361ee";
            default                    -> "#1a1a2e";
        };
        lblEstado.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + colorEstado + ";"
        );

        lblDescuento.setText(String.format("S/ %.2f", cot.getDescuento()));
        lblTotal.setText(String.format("S/ %.2f", cot.getTotal()));

        // Genera las filas de productos
        for (DetalleCotizacion d : detalle) {
            panelProductos.getChildren().add(crearFilaProducto(d));
        }
    }

    private HBox crearFilaProducto(DetalleCotizacion d) {
        Label lblNombre = new Label(d.getProducto().getNombre());
        lblNombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblNombre.setWrapText(true);
        HBox.setHgrow(lblNombre, Priority.ALWAYS);

        Label lblCantidad = new Label(String.valueOf(d.getCantidad()));
        lblCantidad.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblCantidad.setPrefWidth(35);
        lblCantidad.setMinWidth(35);
        lblCantidad.setMaxWidth(35);
        lblCantidad.setAlignment(Pos.CENTER_RIGHT);

        Label lblPrecio = new Label(String.format("%.2f", d.getPrecio()));
        lblPrecio.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblPrecio.setPrefWidth(55);
        lblPrecio.setMinWidth(55);
        lblPrecio.setMaxWidth(55);
        lblPrecio.setAlignment(Pos.CENTER_RIGHT);

        Label lblSubtotal = new Label(String.format("%.2f", d.getSubtotal()));
        lblSubtotal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1a1a2e;");
        lblSubtotal.setPrefWidth(65);
        lblSubtotal.setMinWidth(65);
        lblSubtotal.setMaxWidth(65);
        lblSubtotal.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox(6, lblNombre, lblCantidad, lblPrecio, lblSubtotal);
        fila.setAlignment(Pos.CENTER);
        fila.setStyle("-fx-padding: 2 0;");
        return fila;
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) lblIdCotizacion.getScene().getWindow();
        stage.close();
    }
}