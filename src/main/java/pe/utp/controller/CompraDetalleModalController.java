package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.model.Compra;
import pe.utp.model.DetalleCompra;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CompraDetalleModalController {

    @FXML private Label lblComprobante;
    @FXML private Label lblFecha;
    @FXML private Label lblProveedor;
    @FXML private Label lblEmpleado;
    @FXML private Label lblTotal;
    @FXML private VBox  panelProductos;

    /** Carga los datos de la compra */
    public void cargarDatos(Compra compra, List<DetalleCompra> detalle) {
        lblComprobante.setText(compra.getNumeroComprobante());
        lblFecha.setText(compra.getFecha().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        lblProveedor.setText(compra.getProveedor().getNombre());
        lblEmpleado.setText(compra.getEmpleado().getNombre());
        lblTotal.setText(String.format("S/ %.2f", compra.getTotal()));


        for (DetalleCompra d : detalle) {
            panelProductos.getChildren().add(crearFilaProducto(d));
        }
    }

    private HBox crearFilaProducto(DetalleCompra d) {
        Label lblNombre = new Label(d.getProducto().getNombre());
        lblNombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblNombre.setWrapText(true);
        HBox.setHgrow(lblNombre, javafx.scene.layout.Priority.ALWAYS);

        Label lblCantidad = new Label(String.valueOf(d.getCantidad()));
        lblCantidad.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblCantidad.setPrefWidth(35);
        lblCantidad.setAlignment(Pos.CENTER_RIGHT);

        Label lblPrecio = new Label(String.format("%.2f", d.getPrecio()));
        lblPrecio.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        lblPrecio.setPrefWidth(55);
        lblPrecio.setAlignment(Pos.CENTER_RIGHT);

        Label lblSubtotal = new Label(String.format("%.2f", d.getSubtotal()));
        lblSubtotal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1a1a2e;");
        lblSubtotal.setPrefWidth(65);
        lblSubtotal.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox(6, lblNombre, lblCantidad,
                lblPrecio, lblSubtotal);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) lblComprobante.getScene().getWindow();
        stage.close();
    }
}