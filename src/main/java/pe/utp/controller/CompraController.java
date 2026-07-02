package pe.utp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import pe.utp.dao.CompraDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dao.ProveedorDAO;
import pe.utp.model.*;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CompraController implements AccesoControlable {

    // Pestaña Nueva Compra
    @FXML private TextField txtNumeroComprobante;
    @FXML private ComboBox<Proveedor> cbProveedor;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtFecha;
    @FXML private TextField txtBuscarProducto;
    @FXML private TextField txtBuscarProveedor;
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioUnitario;
    @FXML private TableView<DetalleCompra> tablaDetalle;
    @FXML private TableColumn<DetalleCompra, String>  colProducto;
    @FXML private TableColumn<DetalleCompra, Integer> colCantidad;
    @FXML private TableColumn<DetalleCompra, Double>  colPrecioUnit;
    @FXML private TableColumn<DetalleCompra, Double>  colSubtotal;
    @FXML private TableColumn<DetalleCompra, Void>    colQuitarDet;
    @FXML private Label lblTotal;


    // Pestaña Historial
    @FXML private TextField txtBuscarHistorial;
    @FXML private TableView<Compra> tablaHistorial;
    @FXML private TableColumn<Compra, String> colIdCompra;
    @FXML private TableColumn<Compra, String> colComprobante;
    @FXML private TableColumn<Compra, String> colProveedor;
    @FXML private TableColumn<Compra, String> colEmpleado;
    @FXML private TableColumn<Compra, String> colFecha;
    @FXML private TableColumn<Compra, Double> colTotal;
    @FXML private TableColumn<Compra, Void> colAccionesHist;

    private CompraDAO compraDAO = new CompraDAO();
    private ProveedorDAO proveedorDAO = new ProveedorDAO();
    private ProductoDAO productoDAO  = new ProductoDAO();

    // ObservableList para que la tabla se actualice automáticamente del carrito
    private ObservableList<DetalleCompra> detalleActual =
            FXCollections.observableArrayList();

    // FilteredList para el historial
    private ObservableList<Compra> listaHistorial =
            FXCollections.observableArrayList();
    private FilteredList<Compra>   listaFiltrada;

    @FXML
    public void initialize() {
        configurarTablaDetalle();
        configurarTablaHistorial();
        cargarCombos();
        cargarDatosSesion();
        cargarHistorial();
    }

    // Administrador y Almacenero tienen acceso completo
    @Override
    public void aplicarPermisos() {
    }

    /** Carga el nombre del empleado logueado y la fecha actual. */
    private void cargarDatosSesion() {
        Empleado emp = Sesion.getEmpleado();
        if (emp != null) {
            txtEmpleado.setText(emp.getNombre());
        }
        // Fecha y hora actual
        DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm");
        txtFecha.setText(LocalDateTime.now().format(fmt));
        txtNumeroComprobante.setText(
                compraDAO.generarNumeroComprobante()
        );
    }

    /** Carga los ComboBox de proveedor y producto.*/
    private void cargarCombos() {
        configurarComboProveedor();
        configurarComboProducto();
    }

    /** Configura el ComboBox de proveedor con búsqueda integrada.*/
    private void configurarComboProveedor() {
        List<Proveedor> todos = proveedorDAO.listar();
        ObservableList<Proveedor> listaProveedores =
                FXCollections.observableArrayList(todos);

        cbProveedor.setEditable(true);
        cbProveedor.setItems(listaProveedores);

        // StringConverter convierte Proveedor en String para que el ComboBox muestre el nombre correctamente
        cbProveedor.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre();
            }
            @Override
            public Proveedor fromString(String s) {
                // Busca el proveedor por nombre exacto
                return listaProveedores.stream()
                        .filter(p -> p.getNombre().equals(s))
                        .findFirst().orElse(null);
            }
        });

        // Listener protegido contra el bug de backspace de JavaFX
        cbProveedor.getEditor().textProperty().addListener(
                (obs, oldVal, newVal) -> {

                    Proveedor sel = null;
                    try {
                        Object v = cbProveedor.getValue();
                        if (v instanceof Proveedor pv) sel = pv;
                    } catch (Exception ignored) {}

                    if (sel != null && sel.getNombre().equals(newVal)) return;

                    if (sel != null) {
                        cbProveedor.setValue(null);
                    }

                    // Filtra la lista según lo que escribe
                    String texto = newVal == null ? "" : newVal.toLowerCase();
                    ObservableList<Proveedor> filtrados =
                            FXCollections.observableArrayList(
                                    todos.stream()
                                            .filter(p ->
                                                    texto.isEmpty() ||
                                                            p.getNombre().toLowerCase().contains(texto) ||
                                                            p.getRuc().toLowerCase().contains(texto))
                                            .toList()
                            );
                    cbProveedor.setItems(filtrados);

                    if (!cbProveedor.isShowing() && !texto.isEmpty()) {
                        cbProveedor.show();
                    }
                });
    }

    /** Configura el ComboBox de producto con búsqueda integrada. */
    private void configurarComboProducto() {
        List<Producto> todos = productoDAO.listar().stream()
                .filter(p -> "Activo".equals(p.getEstado()))
                .toList();
        ObservableList<Producto> listaProductos =
                FXCollections.observableArrayList(todos);

        cbProducto.setEditable(true);
        cbProducto.setItems(listaProductos);

        cbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Producto p) {
                return p == null ? "" : p.getNombre();
            }
            @Override
            public Producto fromString(String s) {
                return listaProductos.stream()
                        .filter(p -> p.getNombre().equals(s))
                        .findFirst().orElse(null);
            }
        });

        cbProducto.getEditor().textProperty().addListener(
                (obs, oldVal, newVal) -> {
                    Producto sel = null;
                    try {
                        Object v = cbProducto.getValue();
                        if (v instanceof Producto pv) sel = pv;
                    } catch (Exception ignored) {}

                    if (sel != null && sel.getNombre().equals(newVal)) return;

                    if (sel != null) {
                        cbProducto.setValue(null);
                    }

                    String texto = newVal == null ? "" : newVal.toLowerCase();
                    ObservableList<Producto> filtrados =
                            FXCollections.observableArrayList(
                                    todos.stream()
                                            .filter(p ->
                                                    texto.isEmpty() ||
                                                            p.getNombre().toLowerCase().contains(texto) ||
                                                            p.getIdProducto().toLowerCase().contains(texto))
                                            .toList()
                            );
                    cbProducto.setItems(filtrados);

                    if (!cbProducto.isShowing() && !texto.isEmpty()) {
                        cbProducto.show();
                    }
                });

        // Rellena el precio al seleccionar
        cbProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Producto p &&
                    txtPrecioUnitario.getText().isEmpty()) {
                txtPrecioUnitario.setText(
                        String.format("%.2f", p.getPrecioCompra())
                );
            }
        });
    }

    /** Configura la tabla del detalle de la compra actual.*/
    private void configurarTablaDetalle() {
        // Nombre del producto via lambda porque está en objeto anidado
        colProducto.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProducto() != null
                                ? data.getValue().getProducto().getNombre()
                                : ""
                )
        );

        // Cantidad como propiedad observable para poder editarla
        colCantidad.setCellValueFactory(data ->
                new SimpleIntegerProperty(
                        data.getValue().getCantidad()).asObject()
        );

        // Precio unitario
        colPrecioUnit.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getPrecio()).asObject()
        );

        // Subtotal calculado: cantidad × precio
        // getSubtotal() ya está definido en el modelo DetalleCompra
        colSubtotal.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getSubtotal()).asObject()
        );

        // Columna de quitar: botón X para eliminar la línea del detalle
        colQuitarDet.setCellFactory(col -> new TableCell<>() {
            final Button btnQuitar = new Button("✕");
            {
                btnQuitar.setStyle(
                        "-fx-background-color: #fcebeb;" +
                                "-fx-text-fill: #a32d2d;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 12px;"
                );
                btnQuitar.setOnAction(e -> {
                    DetalleCompra det = getTableView()
                            .getItems().get(getIndex());
                    // Elimina del ObservableList y la tabla
                    // se actualiza automáticamente
                    detalleActual.remove(det);
                    actualizarTotal();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnQuitar);
            }
        });

        tablaDetalle.setItems(detalleActual);
    }

    @FXML
    private void filtrarProveedores() {
        String texto = txtBuscarProveedor.getText()
                .trim().toLowerCase();
        // Accede al FilteredList a través del ComboBox
        FilteredList<Proveedor> lista =
                (FilteredList<Proveedor>) cbProveedor.getItems();
        lista.setPredicate(p ->
                texto.isEmpty() ||
                        p.getNombre().toLowerCase().contains(texto) ||
                        p.getRuc().toLowerCase().contains(texto)
        );
        // Abre el dropdown automáticamente si hay texto
        if (!texto.isEmpty()) cbProveedor.show();
    }

    @FXML
    private void filtrarProductos() {
        String texto = txtBuscarProducto.getText()
                .trim().toLowerCase();
        FilteredList<Producto> lista =
                (FilteredList<Producto>) cbProducto.getItems();
        lista.setPredicate(p ->
                texto.isEmpty() ||
                        p.getNombre().toLowerCase().contains(texto) ||
                        p.getIdProducto().toLowerCase().contains(texto)
        );
        if (!texto.isEmpty()) cbProducto.show();
    }


    private void configurarTablaHistorial() {
        colIdCompra.setCellValueFactory(
                new PropertyValueFactory<>("idCompra"));

        colComprobante.setCellValueFactory(
                new PropertyValueFactory<>("numeroComprobante"));

        // Nombre del proveedor via lambda
        colProveedor.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProveedor() != null
                                ? data.getValue().getProveedor().getNombre()
                                : ""
                )
        );

        // Nombre del empleado via lambda
        colEmpleado.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmpleado() != null
                                ? data.getValue().getEmpleado().getNombre()
                                : ""
                )
        );

        // Fecha formateada para mostrar en la tabla
        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getFecha() != null
                                ? data.getValue().getFecha().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : ""
                )
        );

        colTotal.setCellValueFactory(
                new PropertyValueFactory<>("total"));

        // Columna Ver: abre el detalle de la compra seleccionada
        colAccionesHist.setCellFactory(col -> new TableCell<>() {
            final Button btnVer = new Button("Ver detalle");
            {
                btnVer.setStyle(
                        "-fx-background-color: #4361ee;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 11px;"
                );
                btnVer.setOnAction(e -> {
                    Compra c = getTableView()
                            .getItems().get(getIndex());
                    mostrarDetalleCompra(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnVer);
            }
        });

        // Inicializa el FilteredList igual que en los demás módulos
        listaFiltrada = new FilteredList<>(listaHistorial,
                c -> true);
        tablaHistorial.setItems(listaFiltrada);
    }

    /** Agrega un producto al detalle de la compra actual. */
    @FXML
    private void agregarProducto() {
        // Con StringConverter getValue() puede retornar null
        // si el texto no coincide exactamente con ningún producto
        Object val = cbProducto.getValue();
        Producto producto = (val instanceof Producto p) ? p : null;

        String cantStr   = txtCantidad.getText().trim();
        String precioStr = txtPrecioUnitario.getText().trim();

        if (producto == null || cantStr.isEmpty() ||
                precioStr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona un producto de la lista.\n" +
                            "Escribe para buscar y luego haz clic en la opción.")
                    .showAndWait();
            return;
        }

        try {
            int    cantidad = Integer.parseInt(cantStr);
            double precio = Double.parseDouble(
                    precioStr.replace(",", ".")
            );

            if (cantidad <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        "La cantidad debe ser mayor a cero").showAndWait();
                return;
            }
            if (precio <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        "El precio debe ser mayor a cero").showAndWait();
                return;
            }

            // Validación adicional: precio de compra no debe superar el precio de venta registrado del producto
            if (precio > producto.getPrecioVenta()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Advertencia de precio");
                confirm.setHeaderText("El precio de compra supera el de venta");
                confirm.setContentText(
                        "Precio de compra ingresado: S/ " +
                                String.format("%.2f", precio) + "\n" +
                                "Precio de venta registrado: S/ " +
                                String.format("%.2f", producto.getPrecioVenta()) + "\n\n" +
                                "¿Deseas continuar de todas formas?"
                );
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                if (confirm.showAndWait().orElse(ButtonType.NO)
                        == ButtonType.NO) return;
            }

            // Si el producto ya está en el detalle suma la cantidad
            for (DetalleCompra det : detalleActual) {
                if (det.getProducto().getIdProducto()
                        .equals(producto.getIdProducto())) {
                    det.setCantidad(det.getCantidad() + cantidad);
                    tablaDetalle.refresh();
                    actualizarTotal();
                    limpiarFormProducto();
                    return;
                }
            }

            String idDetalle = "DTC" + String.format("%03d",
                    detalleActual.size() + 1);
            detalleActual.add(new DetalleCompra(
                    idDetalle, null, producto, cantidad, precio
            ));
            actualizarTotal();
            limpiarFormProducto();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING,
                    "Cantidad y precio deben ser números válidos")
                    .showAndWait();
        }
    }

    /** Calcula y muestra el total sumando todos los subtotales */
    private void actualizarTotal() {
        double total = detalleActual.stream()
                .mapToDouble(DetalleCompra::getSubtotal)
                .sum();
        lblTotal.setText(String.format("S/ %.2f", total));
    }


    @FXML
    private void registrarCompra() {
        if (!PermisoService.puedeEditarProveedor()) {
            PermisoUtil.denegado();
            return;
        }

        Object provObj = cbProveedor.getValue();
        Proveedor prov = (provObj instanceof Proveedor p)
                ? p : null;

        // Validación 1: proveedor seleccionado del listado
        if (prov == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona un proveedor del listado.\n" +
                            "Escribe el nombre y haz clic en una opción.")
                    .showAndWait();
            return;
        }

        // Validación 2: al menos un producto en el detalle
        if (detalleActual.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Agrega al menos un producto al detalle")
                    .showAndWait();
            return;
        }

        String idCompra  = generarId();
        String numComp   = compraDAO.generarNumeroComprobante();
        double total     = detalleActual.stream()
                .mapToDouble(DetalleCompra::getSubtotal).sum();

        Compra compra = new Compra(
                idCompra, prov, Sesion.getEmpleado(),
                numComp, LocalDateTime.now(), total
        );

        // Genera IDs definitivos para cada línea
        List<DetalleCompra> detallesFinales = new ArrayList<>();
        int i = 1;
        for (DetalleCompra det : detalleActual) {
            det.setIdDetalleCompra(
                    idCompra + "-D" + String.format("%03d", i++)
            );
            detallesFinales.add(det);
        }

        boolean exito = compraDAO.registrarCompra(
                compra, detallesFinales);

        if (exito) {
            // Verifica si algún producto quedó con costo promedio
            // mayor a su precio de venta actual, lo cual generaría pérdida
            StringBuilder alerta = new StringBuilder();
            for (DetalleCompra det : detallesFinales) {
                Producto actualizado = productoDAO.buscarPorId(
                        det.getProducto().getIdProducto()
                );
                if (actualizado != null &&
                        actualizado.getPrecioCompra() > actualizado.getPrecioVenta()) {
                    alerta.append("• ").append(actualizado.getNombre())
                            .append(": costo S/ ")
                            .append(String.format("%.2f", actualizado.getPrecioCompra()))
                            .append(" > precio venta S/ ")
                            .append(String.format("%.2f", actualizado.getPrecioVenta()))
                            .append("\n");
                }
            }

            String msg = "Compra " + numComp + " registrada correctamente.\n" +
                    "El stock y costo promedio han sido actualizados.";
            // Aviso 1: productos con CPP mayor al precio de venta
            if (alerta.length() > 0) {
                msg += "\n\n⚠ Costo mayor al precio de venta:\n" + alerta +
                        "Considera actualizar el precio de venta " +
                        "desde el módulo Producto.";
            }

            // Aviso 2: productos cuyo stock superó el máximo definido
            // La compra NO se cancela, solo se informa al almacenero
            List<String> avisosStock = compraDAO.getAvisos();
            if (!avisosStock.isEmpty()) {
                msg += "\n\n📦 Stock superó el máximo definido:\n" +
                        String.join("\n", avisosStock) + "\n" +
                        "Considera revisar el stock máximo de estos productos.";
            }

            new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
            limpiarFormulario();
            cargarHistorial();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo registrar la compra.\n" +
                            "Revisa la consola para más detalles.").showAndWait();
        }
    }

    private void mostrarDetalleCompra(Compra c) {
        List<DetalleCompra> detalle =
                compraDAO.listarDetalle(c.getIdCompra());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CompraDetalleModal.fxml")
            );
            Parent root = loader.load();

            CompraDetalleModalController ctrl = loader.getController();
            ctrl.cargarDatos(c, detalle);

            Stage modal = new Stage();
            modal.setTitle("Comprobante de Compra");
            modal.setScene(new Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir el detalle: " + ex.getMessage()).show();
        }
    }

    /** Filtra el historial en tiempo real por número de comprobante o nombre del proveedor */
    @FXML
    private void filtrarHistorial() {
        String texto = txtBuscarHistorial.getText()
                .trim().toLowerCase();
        listaFiltrada.setPredicate(c -> {
            if (texto.isEmpty()) return true;
            boolean enComp = c.getNumeroComprobante()
                    .toLowerCase().contains(texto);
            boolean enProv = c.getProveedor() != null &&
                    c.getProveedor().getNombre()
                            .toLowerCase().contains(texto);
            boolean enEmpl = c.getEmpleado().getNombre().toLowerCase().contains(texto);
            return enComp || enProv || enEmpl;
        });
    }

    private void cargarHistorial() {
        listaHistorial.setAll(compraDAO.listar());
    }

    private String generarId() {
        String ultimo = compraDAO.obtenerUltimoId();
        if (ultimo == null) return "COM001";
        String prefijo   = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        int numero = numeroStr.isEmpty()
                ? 1 : Integer.parseInt(numeroStr) + 1;
        return String.format("%s%03d", prefijo, numero);
    }

    private void limpiarFormProducto() {
        cbProducto.setValue(null);
        cbProducto.getEditor().clear();
        txtCantidad.clear();
        txtPrecioUnitario.clear();
        // Restaura la lista completa de productos
        configurarComboProducto();
    }

    private void limpiarFormulario() {
        cbProveedor.setValue(null);
        cbProveedor.getEditor().clear();
        configurarComboProveedor();

        detalleActual.clear();
        actualizarTotal();
        limpiarFormProducto();
        cargarDatosSesion();
    }
}