package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.utp.model.Empleado;
import pe.utp.security.Modulo;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;

public class MainController {

    @FXML private StackPane panelContenido;
    @FXML private Label lblNombre;
    @FXML private Label lblCargo;

    @FXML private Button btnCategoria;
    @FXML private Button btnProducto;
    @FXML private Button btnCliente;
    @FXML private Button btnProveedor;
    @FXML private Button btnVenta;
    @FXML private Button btnCompra;
    @FXML private Button btnConfigurador;
    @FXML private Button btnEmpleados;
    @FXML private Button btnConfiguracion;

    public void setEmpleado(Empleado empleado) {
        Sesion.iniciar(empleado);
        lblNombre.setText(empleado.getNombre());
        lblCargo.setText(empleado.getCargo());
        configurarMenu();
    }

    private void configurarMenu() {
        configurarBoton(btnCategoria, Modulo.CATEGORIA);
        configurarBoton(btnProducto, Modulo.PRODUCTO);
        configurarBoton(btnCliente, Modulo.CLIENTE);
        configurarBoton(btnProveedor, Modulo.PROVEEDOR);
        configurarBoton(btnVenta, Modulo.VENTA);
        configurarBoton(btnCompra, Modulo.COMPRA);
        configurarBoton(btnConfigurador, Modulo.COTIZACION);
        configurarBoton(btnEmpleados, Modulo.EMPLEADOS);
        configurarBoton(btnConfiguracion, Modulo.CONFIGURACION);
    }

    private void configurarBoton(Button boton, Modulo modulo) {
        boolean visible = PermisoService.puedeAcceder(modulo);
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML
    private void abrirCategoria() {
        cargarVista("/fxml/Categoria.fxml", Modulo.CATEGORIA);
    }

    @FXML
    private void abrirProducto() {
        cargarVista("/fxml/Producto.fxml", Modulo.PRODUCTO);
    }

    @FXML
    private void abrirCliente() {
        cargarVista("/fxml/Cliente.fxml", Modulo.CLIENTE);
    }

    @FXML
    private void abrirProveedor() {
        cargarVista("/fxml/Proveedor.fxml", Modulo.PROVEEDOR);
    }

    @FXML
    private void abrirVenta() {
        cargarVista("/fxml/Venta.fxml", Modulo.VENTA);
    }

    @FXML
    private void abrirCompra() {
        cargarVista("/fxml/Compra.fxml", Modulo.COMPRA);
    }

    @FXML
    private void abrirConfigurador() {
        cargarVista("/fxml/Configurador.fxml", Modulo.COTIZACION);
    }

    @FXML
    private void abrirEmpleados() {
        cargarVista("/fxml/Empleado.fxml", Modulo.EMPLEADOS);
    }

    @FXML
    private void abrirConfiguracion() {
        cargarVista("/fxml/Configuracion.fxml", Modulo.CONFIGURACION);
    }

    @FXML
    private void cerrarSesion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea cerrar sesión?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    Sesion.cerrar();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/Login.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) panelContenido.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setMaximized(false);
                    stage.centerOnScreen();
                    stage.show();
                } catch (Exception e) {
                    System.out.println("Error al cerrar sesion: " + e.getMessage());
                }
            }
        });
    }

    private void cargarVista(String ruta, Modulo modulo) {
        if (!PermisoService.puedeAcceder(modulo)) {
            PermisoUtil.denegado();
            return;
        }
        try {
            var url = getClass().getResource(ruta);
            if (url == null) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Este módulo aún no está implementado.").show();
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent vista = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AccesoControlable acceso) {
                acceso.aplicarPermisos();
            }
            panelContenido.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo cargar la vista: " + ruta + "\n" + e.getMessage()).show();
        }
    }
}
