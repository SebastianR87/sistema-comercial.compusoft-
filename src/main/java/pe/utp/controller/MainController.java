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

import java.util.List;


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
    @FXML private Button btnKardex;
    @FXML private Button btnCompatibilidad;
    @FXML private Label lblSeccionSistema;
    @FXML private Label lblSeccionGestion;
    @FXML private Label lblSeccionComercial;
    @FXML private Label lblBienvenidaUsuario;

    public void setEmpleado(Empleado empleado) {
        Sesion.iniciar(empleado);
        lblNombre.setText(empleado.getNombre());
        lblCargo.setText(empleado.getCargo());
        configurarMenu();

        // Personaliza el mensaje de bienvenida con el nombre del empleado
        if (lblBienvenidaUsuario != null) {
            lblBienvenidaUsuario.setText(
                    "Sesión iniciada como " + empleado.getNombre() +
                            " · " + empleado.getCargo()
            );
        }
    }

    private void configurarMenu() {
        configurarBoton(btnCategoria, Modulo.CATEGORIA);
        configurarBoton(btnProducto, Modulo.PRODUCTO);
        configurarBoton(btnCliente, Modulo.CLIENTE);
        configurarBoton(btnProveedor, Modulo.PROVEEDOR);
        configurarBoton(btnVenta, Modulo.VENTA);
        configurarBoton(btnCompra, Modulo.COMPRA);
        configurarBoton(btnConfigurador, Modulo.COTIZACION);
        configurarBoton(btnCompatibilidad, Modulo.COMPATIBILIDAD);
        configurarBoton(btnEmpleados, Modulo.EMPLEADOS);
        configurarBoton(btnKardex, Modulo.KARDEX);
        configurarBoton(btnConfiguracion, Modulo.CONFIGURACION);

        // Oculta la sección SISTEMA completa si ningún botón es visible
        // Así el menú queda limpio sin secciones vacías según el rol
        boolean hayBotonesGestion = btnCategoria.isVisible()
                || btnProducto.isVisible()
                || btnCliente.isVisible()
                || btnProveedor.isVisible();
        lblSeccionGestion.setVisible(hayBotonesGestion);
        lblSeccionGestion.setManaged(hayBotonesGestion);

        boolean hayBotonesComercial = btnCompra.isVisible()
                || btnVenta.isVisible()
                || btnConfigurador.isVisible()
                || btnCompatibilidad.isVisible();
        lblSeccionComercial.setVisible(hayBotonesComercial);
        lblSeccionComercial.setManaged(hayBotonesComercial);

        boolean hayBotonesSistema = btnEmpleados.isVisible()
                || btnKardex.isVisible()
                || btnConfiguracion.isVisible();
        lblSeccionSistema.setVisible(hayBotonesSistema);
        lblSeccionSistema.setManaged(hayBotonesSistema);
    }

    private void configurarBoton(Button boton, Modulo modulo) {
        boolean visible = PermisoService.puedeAcceder(modulo);
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML
    private void abrirCategoria() {
        marcarActivo(btnCategoria); cargarVista("/fxml/Categoria.fxml", Modulo.CATEGORIA);
    }

    @FXML
    private void abrirProducto() {
        marcarActivo(btnProducto);cargarVista("/fxml/Producto.fxml", Modulo.PRODUCTO);
    }

    @FXML
    private void abrirCliente() {
        marcarActivo(btnCliente);cargarVista("/fxml/Cliente.fxml", Modulo.CLIENTE);
    }

    @FXML
    private void abrirProveedor() { marcarActivo(btnProveedor);
        cargarVista("/fxml/Proveedor.fxml", Modulo.PROVEEDOR);
    }

    @FXML
    private void abrirVenta() { marcarActivo(btnVenta);
        cargarVista("/fxml/Venta.fxml", Modulo.VENTA);
    }

    @FXML
    private void abrirCompra() {
        marcarActivo(btnCompra);cargarVista("/fxml/Compra.fxml", Modulo.COMPRA);
    }

    @FXML
    private void abrirCotizacion() {marcarActivo(btnConfigurador);cargarVista("/fxml/Cotizacion.fxml", Modulo.COTIZACION);}

    @FXML
    private void abrirCompatibilidad() {marcarActivo(btnCompatibilidad); cargarVista("/fxml/Compatibilidad.fxml", Modulo.COMPATIBILIDAD);}

    @FXML
    private void abrirEmpleados() {
        marcarActivo(btnEmpleados); cargarVista("/fxml/Empleado.fxml", Modulo.EMPLEADOS);
    }

    @FXML
    private void abrirKardex() { marcarActivo(btnKardex);cargarVista("/fxml/Kardex.fxml", Modulo.KARDEX);}

    @FXML
    private void abrirConfiguracion() {
        marcarActivo(btnConfiguracion);cargarVista("/fxml/Configuracion.fxml", Modulo.CONFIGURACION);
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
                    stage.setMaximized(false);
                    stage.setWidth(700);
                    stage.setHeight(520);
                    stage.setScene(new Scene(root));
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

    // Lista de todos los botones del sidebar
    private List<Button> botonesMenu;

    private void inicializarBotonesMenu() {
        botonesMenu = List.of(
                btnCategoria, btnProducto, btnCliente, btnProveedor,
                btnCompra, btnVenta, btnConfigurador, btnCompatibilidad,
                btnEmpleados, btnKardex, btnConfiguracion
        );
    }

    /**
     * Marca el botón activo con el estilo resaltado
     * y quita el estilo a todos los demás.
     */
    private void marcarActivo(Button boton) {
        if (botonesMenu == null) inicializarBotonesMenu();
        for (Button b : botonesMenu) {
            b.getStyleClass().remove("sidebar-btn-active");
        }
        if (boton != null) {
            boton.getStyleClass().add("sidebar-btn-active");
        }
    }
}
