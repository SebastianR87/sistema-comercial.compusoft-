package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import pe.utp.model.Empleado;

public class MainController {

    @FXML private StackPane panelContenido;
    @FXML private Label lblNombre;
    @FXML private Label lblCargo;

    private Empleado empleadoActual;

    public void setEmpleado(Empleado empleado) {
        this.empleadoActual = empleado;
        lblNombre.setText(empleado.getNombre());
        lblCargo.setText(empleado.getCargo());
    }

    @FXML
    private void abrirCategoria() {
        cargarVista("/fxml/Categoria.fxml");
    }

    @FXML
    private void abrirProducto() {
        cargarVista("/fxml/Producto.fxml");
    }

    @FXML
    private void abrirCliente() {
        cargarVista("/fxml/Cliente.fxml");
    }

    @FXML
    private void abrirProveedor() {
        cargarVista("/fxml/Proveedor.fxml");
    }

    @FXML
    private void abrirVenta() {
        cargarVista("/fxml/Venta.fxml");
    }

    @FXML
    private void abrirCompra() {
        cargarVista("/fxml/Compra.fxml");
    }

    @FXML
    private void abrirConfigurador() {
        cargarVista("/fxml/Configurador.fxml");
    }


    @FXML
    private void cerrarSesion() {
        // Por ahora solo muestra alerta
        // Después aquí irá la lógica de volver al Login
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea cerrar sesión?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                System.out.println("Cerrando sesión...");
                // Aquí después cargamos el Login.fxml
            }
        });
    }

    private void cargarVista(String ruta) {
        try {
            var url = getClass().getResource(ruta);
            if (url == null) {
                System.out.println("Vista no implementada aun: " + ruta);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent vista = loader.load();
            panelContenido.getChildren().setAll(vista);
        } catch (Exception e) {
            System.out.println("Error al cargar vista: " + e.getMessage());
        }
    }


}