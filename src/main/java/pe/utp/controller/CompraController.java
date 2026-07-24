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
import pe.utp.dao.LoteDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dao.ProveedorDAO;
import pe.utp.service.CompraService;
import pe.utp.dialog.CSDialog;
import pe.utp.model.*;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;
import pe.utp.util.FormatoMoneda;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.HBox;

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
    @FXML private TableColumn<Compra, String> colEstado;

    private CompraDAO compraDAO = new CompraDAO();
    private CompraService compraService = new CompraService();
    private ProveedorDAO proveedorDAO = new ProveedorDAO();
    private ProductoDAO productoDAO  = new ProductoDAO();
    // producto.precio_compra ya no se guarda (cada lote tiene su
    // propio costo real -- ver comentario donde se usa más abajo);
    // el costo promedio real se calcula desde los lotes
    private LoteDAO loteDAO = new LoteDAO(pe.utp.Conexion.ConexionDB.getConexion());

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

        // Rellena el precio al seleccionar. Antes usaba
        // p.getPrecioCompra(), columna que ya NO se actualiza tras la
        // creación del producto (queda fija en 0 para siempre, ver
        // comentario en CompraDAO.registrarCompra()) -- así que este
        // autocompletado nunca proponía nada útil. Se usa el costo
        // promedio real calculado desde los lotes, igual que en
        // ProductoController.cargarTabla().
        cbProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Producto p &&
                    txtPrecioUnitario.getText().isEmpty()) {
                double costoPromedio = loteDAO.obtenerCostoPromedio(p.getIdProducto());
                if (costoPromedio > 0) {
                    txtPrecioUnitario.setText(
                            String.format("%.2f", costoPromedio)
                    );
                }
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
        colPrecioUnit.setCellFactory(FormatoMoneda.celda());

        // Subtotal calculado: cantidad × precio
        // getSubtotal() ya está definido en el modelo DetalleCompra
        colSubtotal.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getSubtotal()).asObject()
        );
        colSubtotal.setCellFactory(FormatoMoneda.celda());

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

        colProveedor.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProveedor() != null
                                ? data.getValue().getProveedor().getNombre()
                                : ""
                )
        );

        colEmpleado.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmpleado() != null
                                ? data.getValue().getEmpleado().getNombre()
                                : ""
                )
        );

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
        colTotal.setCellFactory(FormatoMoneda.celda());

        // Columna de estado, pintada según el valor
        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEstado() != null
                                ? data.getValue().getEstado() : "ACTIVA"
                )
        );
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(estado);
                if ("ANULADA".equalsIgnoreCase(estado)) {
                    setStyle("-fx-text-fill: #a32d2d; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #2d8a3e; -fx-font-weight: bold;");
                }
            }
        });

        colAccionesHist.setCellFactory(col -> new TableCell<>() {
            final Button btnVer = new Button("Ver");
            final Button btnAnular = new Button("Anular");
            final HBox contenedor = new HBox(6, btnVer, btnAnular);
            {
                btnVer.getStyleClass().add("btn-table-view");
                btnVer.setOnAction(e -> {
                    Compra c = getTableView()
                            .getItems().get(getIndex());
                    mostrarDetalleCompra(c);
                });

                btnAnular.getStyleClass().add("btn-table-delete");
                btnAnular.setOnAction(e -> {
                    Compra c = getTableView().getItems().get(getIndex());
                    confirmarYAnular(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Compra c = getTableView().getItems().get(getIndex());
                boolean yaAnulada = "ANULADA".equalsIgnoreCase(c.getEstado());
                boolean puedeAnular = PermisoService.puedeAnularCompra();

                btnAnular.setVisible(puedeAnular);
                btnAnular.setManaged(puedeAnular);
                btnAnular.setDisable(yaAnulada);

                contenedor.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(contenedor);
            }
        });

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
            CSDialog.warning("Producto no seleccionado",
                    "Selecciona un producto de la lista. Escribe para buscar y luego haz clic en la opción.");
            return;
        }

        try {
            int    cantidad = Integer.parseInt(cantStr);
            double precio = Double.parseDouble(
                    precioStr.replace(",", ".")
            );

            if (cantidad <= 0) {
                CSDialog.warning("Cantidad inválida", "La cantidad debe ser mayor a cero.");
                return;
            }
            if (precio <= 0) {
                CSDialog.warning("Precio inválido", "El precio debe ser mayor a cero.");
                return;
            }

            // Validación adicional: precio de compra no debe superar el precio de venta registrado del producto
            if (precio > producto.getPrecioVenta()) {
                boolean continuar = CSDialog.confirm(
                        "Advertencia de precio",
                        "El precio de compra supera el de venta.\n\n" +
                                "Precio de compra ingresado: S/ " +
                                String.format("%.2f", precio) + "\n" +
                                "Precio de venta registrado: S/ " +
                                String.format("%.2f", producto.getPrecioVenta()) + "\n\n" +
                                "¿Deseas continuar de todas formas?",
                        "Continuar", "Cancelar", true);
                if (!continuar) return;
            }

            // Si el producto ya está en el detalle CON EL MISMO PRECIO,
            // suma la cantidad a esa línea. Si el precio es distinto,
            // NO se mezcla -- se crea una línea nueva más abajo, porque
            // cada línea genera su propio lote con su propio costo real
            // (FIFO). Mezclar cantidades a distinto precio en una sola
            // línea perdería el costo real de una de las dos compras.
            for (DetalleCompra det : detalleActual) {
                if (det.getProducto().getIdProducto()
                        .equals(producto.getIdProducto())
                        && Math.abs(det.getPrecio() - precio) < 0.001) {
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
            CSDialog.warning("Datos inválidos", "Cantidad y precio deben ser números válidos.");
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
        if (!PermisoService.puedeEditarCompra()) {
            PermisoUtil.denegado();
            return;
        }

        Object provObj = cbProveedor.getValue();
        Proveedor prov = (provObj instanceof Proveedor p)
                ? p : null;

        // Validación 1: proveedor seleccionado del listado
        if (prov == null) {
            CSDialog.warning("Proveedor no seleccionado",
                    "Selecciona un proveedor del listado. Escribe el nombre y haz clic en una opción.");
            return;
        }

        // Validación 2: al menos un producto en el detalle
        if (detalleActual.isEmpty()) {
            CSDialog.warning("Detalle vacío", "Agrega al menos un producto al detalle.");
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
            // mayor a su precio de venta actual, lo cual generaría
            // pérdida. Antes comparaba contra
            // actualizado.getPrecioCompra(), que siempre es 0 (esa
            // columna no se actualiza más -- ver comentario en el
            // listener de arriba), así que este aviso NUNCA podía
            // dispararse. Se usa el costo promedio real de los lotes.
            StringBuilder alerta = new StringBuilder();
            for (DetalleCompra det : detallesFinales) {
                Producto actualizado = productoDAO.buscarPorId(
                        det.getProducto().getIdProducto()
                );
                double costoPromedio = actualizado != null
                        ? loteDAO.obtenerCostoPromedio(actualizado.getIdProducto())
                        : 0;
                if (actualizado != null &&
                        costoPromedio > actualizado.getPrecioVenta()) {
                    alerta.append("• ").append(actualizado.getNombre())
                            .append(": costo S/ ")
                            .append(String.format("%.2f", costoPromedio))
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

            CSDialog.success("Compra registrada", msg);
            limpiarFormulario();
            cargarHistorial();
        } else {
            CSDialog.error("Error", "No se pudo registrar la compra. Revisa la consola para más detalles.");
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

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle("Comprobante de Compra");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir el detalle: " + ex.getMessage());
        }
    }

    private void confirmarYAnular(Compra c) {
        if (!PermisoService.puedeAnularCompra()) {
            PermisoUtil.denegado();
            return;
        }

        if ("ANULADA".equalsIgnoreCase(c.getEstado())) {
            CSDialog.info("Compra anulada", "Esta compra ya está anulada.");
            return;
        }

        boolean confirmado = CSDialog.confirm(
                "Confirmar anulación",
                "¿Anular la compra " + c.getNumeroComprobante() + "?\n\n" +
                        "Esta acción revertirá el stock y el costo promedio (CPP) " +
                        "de los productos comprados a su valor anterior. " +
                        "La compra quedará marcada como ANULADA y no se eliminará del historial.",
                "Anular", "Cancelar", true, true);
        if (!confirmado) return;

        String resultado = compraService.anularCompra(c.getIdCompra());

        switch (resultado) {
            case "OK" -> {
                CSDialog.success("Compra anulada",
                        "Compra " + c.getNumeroComprobante() + " anulada correctamente. " +
                                "El stock y el costo promedio han sido revertidos.");
                cargarHistorial();
            }
            case "YA_ANULADA" -> CSDialog.warning("Compra anulada", "Esta compra ya estaba anulada.");
            case "NO_EXISTE" -> CSDialog.error("Error", "No se encontró la compra.");
            default -> {
                if (resultado.startsWith("STOCK_INSUFICIENTE:")) {
                    String[] partes = resultado.split(":");
                    CSDialog.warning("No se puede anular",
                            "El producto \"" + partes[1] + "\" ya no tiene stock suficiente " +
                                    "(disponible: " + partes[2] + " unidades) porque parte de esa " +
                                    "mercadería ya fue vendida.\n\n" +
                                    "No es posible revertir esta compra sin dejar el stock inconsistente.");
                } else {
                    CSDialog.error("Error", "No se pudo anular la compra. Revisa la consola para más detalles.");
                }
            }
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