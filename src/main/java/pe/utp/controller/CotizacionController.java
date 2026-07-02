package pe.utp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.CompatibilidadDAO;
import pe.utp.model.Compatibilidad;
import javafx.scene.paint.Color;
import pe.utp.dao.CotizacionDAO;
import pe.utp.dao.ClienteDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.model.*;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CotizacionController implements AccesoControlable {

    // Pestaña Nueva Cotización
    @FXML private TextField txtNumeroCotizacion;
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private TextField  txtEmpleado;
    @FXML private TextField  txtFecha;
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private Label lblStockDisponible;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioUnitario;

    @FXML private TableView<DetalleCotizacion>             tablaDetalle;
    @FXML private TableColumn<DetalleCotizacion, String>   colCodigo;
    @FXML private TableColumn<DetalleCotizacion, String>   colProducto;
    @FXML private TableColumn<DetalleCotizacion, Integer>  colCantidad;
    @FXML private TableColumn<DetalleCotizacion, Double>   colPrecioUnit;
    @FXML private TableColumn<DetalleCotizacion, Double>   colSubtotal;
    @FXML private TableColumn<DetalleCotizacion, Void>     colQuitarDet;

    @FXML private TextField txtDescuento;
    @FXML private Label lblTotal;
    @FXML private Label lblSubtotal;

    // ── Pestaña Historial ─────────────────────────────────
    @FXML private TextField txtBuscarHistorial;
    @FXML private ComboBox<String>  cbFiltroEstado;
    @FXML private TableView<Cotizacion> tablaHistorial;
    @FXML private TableColumn<Cotizacion, String>  colIdCot;
    @FXML private TableColumn<Cotizacion, String>  colCliente;
    @FXML private TableColumn<Cotizacion, String>  colEmpleado;
    @FXML private TableColumn<Cotizacion, String>  colFecha;
    @FXML private TableColumn<Cotizacion, Double>  colTotal;
    @FXML private TableColumn<Cotizacion, String>  colEstado;
    @FXML private TableColumn<Cotizacion, Void>  colAcciones;

    @FXML private VBox panelCompatibilidad;
    @FXML private VBox listaResultadosComp;

    private CompatibilidadDAO compatibilidadDAO = new CompatibilidadDAO();

    // DAOs
    private CotizacionDAO cotizacionDAO = new CotizacionDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProductoDAO productoDAO = new ProductoDAO();

    // Carrito actual
    private ObservableList<DetalleCotizacion> detalleActual =
            FXCollections.observableArrayList();

    //Historial filtrado
    private ObservableList<Cotizacion> listaHistorial =
            FXCollections.observableArrayList();
    private FilteredList<Cotizacion>   listaFiltrada;

    @FXML
    public void initialize() {
        configurarTablaDetalle();
        configurarTablaHistorial();
        configurarComboCliente();
        configurarComboProducto();
        configurarFiltroEstado();
        cargarDatosSesion();
        cargarHistorial();
        actualizarTotal();
    }

    @Override
    public void aplicarPermisos() {
        // Administrador y Vendedor tienen acceso completo
    }

    //CARGA INICIAL

    private void cargarDatosSesion() {
        Empleado emp = Sesion.getEmpleado();
        if (emp != null) txtEmpleado.setText(emp.getNombre());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        txtFecha.setText(LocalDateTime.now().format(fmt));
        generarNumeroCotizacion();
    }

    private void generarNumeroCotizacion() {
        String ultimo = cotizacionDAO.obtenerUltimoId();
        if (ultimo == null) {
            txtNumeroCotizacion.setText("COT001");
            return;
        }
        String prefijo   = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        int numero = numeroStr.isEmpty() ? 1 : Integer.parseInt(numeroStr) + 1;
        txtNumeroCotizacion.setText(String.format("%s%03d", prefijo, numero));
    }

    private void configurarFiltroEstado() {
        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "Todos", Cotizacion.PENDIENTE,
                Cotizacion.ACEPTADA, Cotizacion.RECHAZADA
        ));
        cbFiltroEstado.setValue("Todos");
    }

    //COMBOS CON BÚSQUEDA

    private void configurarComboCliente() {
        List<Cliente> todos = clienteDAO.listar();
        ObservableList<Cliente> lista = FXCollections.observableArrayList(todos);

        cbCliente.setEditable(true);
        cbCliente.setItems(lista);
        cbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Cliente c) {
                return c == null ? "" : c.getNombre() + " - " + c.getNumeroDocumento();
            }
            @Override public Cliente fromString(String s) {
                return lista.stream()
                        .filter(c -> (c.getNombre() + " - " + c.getNumeroDocumento()).equals(s))
                        .findFirst().orElse(null);
            }
        });

        cbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Cliente sel = null;
            try { Object v = cbCliente.getValue(); if (v instanceof Cliente cv) sel = cv; }
            catch (Exception ignored) {}
            String textoSel = sel != null
                    ? sel.getNombre() + " - " + sel.getNumeroDocumento() : null;
            if (sel != null && textoSel.equals(newVal)) return;
            if (sel != null) cbCliente.setValue(null);
            String texto = newVal == null ? "" : newVal.toLowerCase();
            cbCliente.setItems(FXCollections.observableArrayList(
                    todos.stream()
                            .filter(c -> texto.isEmpty()
                                    || c.getNombre().toLowerCase().contains(texto)
                                    || c.getNumeroDocumento().toLowerCase().contains(texto))
                            .toList()
            ));
            if (!cbCliente.isShowing() && !texto.isEmpty()) cbCliente.show();
        });
    }

    private void configurarComboProducto() {
        List<Producto> todos = productoDAO.listar().stream()
                .filter(p -> "Activo".equals(p.getEstado()) && p.getStock() > 0)
                .toList();
        ObservableList<Producto> lista = FXCollections.observableArrayList(todos);

        cbProducto.setEditable(true);
        cbProducto.setItems(lista);
        cbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto p) {
                return p == null ? "" : p.getNombre();
            }
            @Override public Producto fromString(String s) {
                return lista.stream()
                        .filter(p -> p.getNombre().equals(s))
                        .findFirst().orElse(null);
            }
        });

        cbProducto.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Producto sel = null;
            try { Object v = cbProducto.getValue(); if (v instanceof Producto pv) sel = pv; }
            catch (Exception ignored) {}
            if (sel != null && sel.getNombre().equals(newVal)) return;
            if (sel != null) cbProducto.setValue(null);
            String texto = newVal == null ? "" : newVal.toLowerCase();
            cbProducto.setItems(FXCollections.observableArrayList(
                    todos.stream()
                            .filter(p -> texto.isEmpty()
                                    || p.getNombre().toLowerCase().contains(texto)
                                    || p.getIdProducto().toLowerCase().contains(texto))
                            .toList()
            ));
            if (!cbProducto.isShowing() && !texto.isEmpty()) cbProducto.show();
        });

        // Al seleccionar: rellena precio y muestra stock
        cbProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Producto p) {
                txtPrecioUnitario.setText(String.format("%.2f", p.getPrecioVenta()));
                int enCarrito = detalleActual.stream()
                        .filter(d -> d.getProducto().getIdProducto().equals(p.getIdProducto()))
                        .mapToInt(DetalleCotizacion::getCantidad).sum();
                int disponible = p.getStock() - enCarrito;
                lblStockDisponible.setText(
                        "Stock disponible: " + disponible + " unidades" +
                                (enCarrito > 0 ? " (" + enCarrito + " en carrito)" : "")
                );
            } else {
                lblStockDisponible.setText("");
            }
        });
    }

    //TABLA DETALLE
    private void configurarTablaDetalle() {
        colCodigo.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProducto() != null
                                ? data.getValue().getProducto().getIdProducto() : ""
                )
        );
        colProducto.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProducto() != null
                                ? data.getValue().getProducto().getNombre() : ""
                )
        );
        colCantidad.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCantidad()).asObject()
        );
        colPrecioUnit.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getPrecio()).asObject()
        );
        colSubtotal.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject()
        );
        colQuitarDet.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("✕");
            {
                btn.setStyle(
                        "-fx-background-color: #fcebeb; -fx-text-fill: #a32d2d;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;"
                );
                btn.setOnAction(e -> {
                    detalleActual.remove(getTableView().getItems().get(getIndex()));
                    actualizarTotal();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        tablaDetalle.setItems(detalleActual);
    }

    //AGREGAR PRODUCTO
    @FXML
    private void agregarProducto() {
        Object val = cbProducto.getValue();
        Producto producto = (val instanceof Producto p) ? p : null;
        String cantStr   = txtCantidad.getText().trim();
        String precioStr = txtPrecioUnitario.getText().trim();

        if (producto == null || cantStr.isEmpty() || precioStr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona un producto, cantidad y precio").showAndWait();
            return;
        }
        try {
            int    cantidad = Integer.parseInt(cantStr);
            double precio   = Double.parseDouble(precioStr.replace(",", "."));

            if (cantidad <= 0 || precio <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        "Cantidad y precio deben ser mayores a cero").showAndWait();
                return;
            }

            // Valida stock
            int enCarrito = detalleActual.stream()
                    .filter(d -> d.getProducto().getIdProducto()
                            .equals(producto.getIdProducto()))
                    .mapToInt(DetalleCotizacion::getCantidad).sum();
            if (enCarrito + cantidad > producto.getStock()) {
                new Alert(Alert.AlertType.WARNING,
                        "Stock insuficiente para \"" + producto.getNombre() + "\".\n" +
                                "Disponible: " + producto.getStock() + " unidades.\n" +
                                "Ya tienes " + enCarrito + " en el carrito.").showAndWait();
                return;
            }

            // Si ya está en el carrito suma la cantidad
            for (DetalleCotizacion det : detalleActual) {
                if (det.getProducto().getIdProducto()
                        .equals(producto.getIdProducto())) {
                    det.setCantidad(det.getCantidad() + cantidad);
                    tablaDetalle.refresh();
                    actualizarTotal();
                    limpiarFormProducto();
                    return;
                }
            }

            detalleActual.add(new DetalleCotizacion(
                    "DTC" + String.format("%03d", detalleActual.size() + 1),
                    null, producto, cantidad, precio
            ));
            actualizarTotal();
            limpiarFormProducto();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING,
                    "Cantidad y precio deben ser números válidos").showAndWait();
        }
    }

    //TOTALES
    @FXML
    private void actualizarTotal() {
        double subtotal = detalleActual.stream()
                .mapToDouble(DetalleCotizacion::getSubtotal).sum();
        double descuento = 0;
        try {
            descuento = Double.parseDouble(
                    txtDescuento.getText().trim().replace(",", "."));
        } catch (NumberFormatException ignored) {}
        if (descuento < 0) descuento = 0;
        if (descuento > subtotal) descuento = subtotal;

        double total = subtotal - descuento;
        lblTotal.setText(String.format("S/ %.2f", total));
        lblSubtotal.setText("Subtotal: S/ " + String.format("%.2f", subtotal));
    }

    //REGISTRAR COTIZACIÓN
    @FXML
    private void registrarCotizacion() {
        if (!PermisoService.puedeEditarVenta()) {
            PermisoUtil.denegado();
            return;
        }

        Object clienteObj = cbCliente.getValue();
        Cliente cliente = (clienteObj instanceof Cliente c) ? c : null;

        if (cliente == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona un cliente del listado.").showAndWait();
            return;
        }
        if (detalleActual.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Agrega al menos un producto al detalle.").showAndWait();
            return;
        }

        double subtotal = detalleActual.stream()
                .mapToDouble(DetalleCotizacion::getSubtotal).sum();
        double descuento = 0;
        try {
            descuento = Double.parseDouble(
                    txtDescuento.getText().trim().replace(",", "."));
        } catch (NumberFormatException ignored) {}
        if (descuento < 0 || descuento > subtotal) descuento = 0;
        double total = subtotal - descuento;

        String idCot = txtNumeroCotizacion.getText();
        Cotizacion cot = new Cotizacion(
                idCot, cliente, Sesion.getEmpleado(),
                LocalDateTime.now(), descuento, total, Cotizacion.PENDIENTE
        );

        // Genera IDs del detalle
        List<DetalleCotizacion> detallesFinales = new ArrayList<>();
        int i = 1;
        for (DetalleCotizacion det : detalleActual) {
            det.setIdDetalle(idCot + "-D" + String.format("%03d", i++));
            detallesFinales.add(det);
        }

        boolean exito = cotizacionDAO.registrarCotizacion(cot, detallesFinales);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Cotización " + idCot + " registrada correctamente.\n" +
                            "Estado: PENDIENTE").showAndWait();
            limpiarFormulario();
            cargarHistorial();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo registrar la cotización.").showAndWait();
        }
    }

    //HISTORIAL
    private void configurarTablaHistorial() {
        colIdCot.setCellValueFactory(
                new PropertyValueFactory<>("idCotizacion"));
        colCliente.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getCliente() != null
                                ? data.getValue().getCliente().getNombre() : ""
                )
        );
        colEmpleado.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmpleado() != null
                                ? data.getValue().getEmpleado().getNombre() : ""
                )
        );
        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getFecha() != null
                                ? data.getValue().getFecha().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
                )
        );
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        // Columna estado con color según valor
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setText(null); setStyle("");
                    return;
                }
                String estado = getTableView().getItems()
                        .get(getIndex()).getEstado();
                setText(estado);
                setStyle(switch (estado) {
                    case Cotizacion.PENDIENTE  ->
                            "-fx-text-fill: #e67e00; -fx-font-weight: bold;";
                    case Cotizacion.ACEPTADA   ->
                            "-fx-text-fill: #2dc653; -fx-font-weight: bold;";
                    case Cotizacion.RECHAZADA  ->
                            "-fx-text-fill: #e94560; -fx-font-weight: bold;";
                    case Cotizacion.CONVERTIDA ->
                            "-fx-text-fill: #4361ee; -fx-font-weight: bold;";
                    default -> "";
                });
            }
        });

        // Columna acciones: Ver | Aceptar | Rechazar | Convertir a Venta
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnVer       = new Button("Ver");
            final Button btnAceptar   = new Button("Aceptar");
            final Button btnRechazar  = new Button("Rechazar");
            final Button btnConvertir = new Button("→ Venta");
            {
                btnVer.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px;");
                btnAceptar.setStyle(
                        "-fx-background-color: #2dc653; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px;");
                btnRechazar.setStyle(
                        "-fx-background-color: #e94560; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px;");
                btnConvertir.setStyle(
                        "-fx-background-color: #1a1a2e; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px;");

                btnVer.setOnAction(e -> {
                    Cotizacion c = getTableView().getItems().get(getIndex());
                    verDetalle(c);
                });
                btnAceptar.setOnAction(e -> {
                    Cotizacion c = getTableView().getItems().get(getIndex());
                    cambiarEstado(c, Cotizacion.ACEPTADA);
                });
                btnRechazar.setOnAction(e -> {
                    Cotizacion c = getTableView().getItems().get(getIndex());
                    cambiarEstado(c, Cotizacion.RECHAZADA);
                });
                // Convierte la cotización en venta abriendo VentaController pre-llenado
                btnConvertir.setOnAction(e -> {
                    Cotizacion c = getTableView().getItems().get(getIndex());
                    convertirAVenta(c);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Cotizacion c = getTableView().getItems().get(getIndex());
                String estado = c.getEstado();

                HBox hbox = new HBox(4);
                hbox.getChildren().add(btnVer);

                // Solo muestra botones relevantes según el estado
                if (Cotizacion.PENDIENTE.equals(estado)) {
                    hbox.getChildren().addAll(btnAceptar, btnRechazar);
                }
                if (Cotizacion.ACEPTADA.equals(estado)) {
                    hbox.getChildren().add(btnConvertir);
                }
                setGraphic(hbox);
            }
        });

        listaFiltrada = new FilteredList<>(listaHistorial, c -> true);
        tablaHistorial.setItems(listaFiltrada);
    }

    @FXML
    private void filtrarHistorial() {
        String texto  = txtBuscarHistorial.getText().trim().toLowerCase();
        String estado = cbFiltroEstado.getValue();

        listaFiltrada.setPredicate(c -> {
            boolean matchTexto = texto.isEmpty()
                    || c.getIdCotizacion().toLowerCase().contains(texto)
                    || (c.getCliente() != null
                    && c.getCliente().getNombre().toLowerCase().contains(texto));
            boolean matchEstado = "Todos".equals(estado)
                    || estado == null
                    || estado.equals(c.getEstado());
            return matchTexto && matchEstado;
        });
    }

    private void cargarHistorial() {
        listaHistorial.setAll(cotizacionDAO.listar());
    }

    // ACCIONES DEL HISTORIAL
    private void cambiarEstado(Cotizacion c, String nuevoEstado) {
        String msg = Cotizacion.ACEPTADA.equals(nuevoEstado)
                ? "¿Marcar esta cotización como ACEPTADA?"
                : "¿Marcar esta cotización como RECHAZADA?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                if (cotizacionDAO.cambiarEstado(c.getIdCotizacion(), nuevoEstado)) {
                    cargarHistorial();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "No se pudo cambiar el estado.").showAndWait();
                }
            }
        });
    }

    /**
     * Convierte una cotización ACEPTADA en venta.
     * En vez de abrir un modal, navega al módulo de Venta
     * dentro del panel principal, igual que al hacer clic
     * en "Venta" del menú, pero con los datos pre-cargados.
     * Marca la cotización como CONVERTIDA para evitar duplicados.
     */
    private void convertirAVenta(Cotizacion c) {
        // Verifica que no haya sido ya convertida
        if (Cotizacion.CONVERTIDA.equals(c.getEstado())) {
            new Alert(Alert.AlertType.WARNING,
                    "Esta cotización ya fue convertida a venta anteriormente.")
                    .showAndWait();
            return;
        }

        List<DetalleCotizacion> detalle =
                cotizacionDAO.listarDetalle(c.getIdCotizacion());

        // Verifica stock suficiente antes de continuar
        StringBuilder sinStock = new StringBuilder();
        for (DetalleCotizacion d : detalle) {
            if (d.getCantidad() > d.getProducto().getStock()) {
                sinStock.append("• ").append(d.getProducto().getNombre())
                        .append(": necesitas ").append(d.getCantidad())
                        .append(", disponible ").append(d.getProducto().getStock())
                        .append("\n");
            }
        }
        if (sinStock.length() > 0) {
            new Alert(Alert.AlertType.WARNING,
                    "No se puede convertir a venta. Stock insuficiente:\n\n" +
                            sinStock + "\nActualiza el stock antes de continuar.")
                    .showAndWait();
            return;
        }

        try {
            // Carga el FXML de Venta
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Venta.fxml")
            );
            Parent vistaVenta = loader.load();

            VentaController ventaCtrl = loader.getController();
            ventaCtrl.aplicarPermisos();
            // Pre-carga los datos de la cotización en el formulario de venta
            ventaCtrl.preCargarDesdeCotizacion(c, detalle);

            // Navega al módulo de Venta en el panel principal
            // Obtiene el StackPane del contenido principal
            javafx.scene.layout.StackPane panelContenido =
                    (javafx.scene.layout.StackPane)
                            tablaHistorial.getScene().lookup("#panelContenido");

            if (panelContenido != null) {
                panelContenido.getChildren().setAll(vistaVenta);

                // Marca la cotización como CONVERTIDA para evitar duplicados
                cotizacionDAO.cambiarEstado(
                        c.getIdCotizacion(), Cotizacion.CONVERTIDA
                );
                cargarHistorial();
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "No se pudo navegar al módulo de Venta.").showAndWait();
            }

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Error al convertir: " + ex.getMessage()).showAndWait();
            ex.printStackTrace();
        }
    }

    private void verDetalle(Cotizacion c) {
        List<DetalleCotizacion> detalle =
                cotizacionDAO.listarDetalle(c.getIdCotizacion());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CotizacionDetalleModal.fxml")
            );
            Parent root = loader.load();

            CotizacionDetalleModalController ctrl = loader.getController();
            ctrl.cargarDatos(c, detalle);

            Stage modal = new Stage();
            modal.setTitle("Detalle de Cotización " + c.getIdCotizacion());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir el detalle: " + ex.getMessage()).show();
        }
    }

    /**
     * Verifica la compatibilidad entre todos los productos
     * que están actualmente en el carrito de la cotización.
     * Compara cada par posible y muestra el resultado.
     * Solo aplica si hay 2 o más productos en el carrito.
     */
    @FXML
    private void verificarCarrito() {
        if (detalleActual.size() < 2) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Agrega al menos 2 productos al carrito\n" +
                            "para verificar compatibilidad entre ellos.")
                    .showAndWait();
            return;
        }

        listaResultadosComp.getChildren().clear();
        boolean hayAlgunaRegla = false;

        // Compara cada par único de productos en el carrito
        // Si hay A, B, C: verifica A↔B, A↔C, B↔C
        List<DetalleCotizacion> items = new ArrayList<>(detalleActual);
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                String idP1 = items.get(i).getProducto().getIdProducto();
                String idP2 = items.get(j).getProducto().getIdProducto();
                String nomP1 = items.get(i).getProducto().getNombre();
                String nomP2 = items.get(j).getProducto().getNombre();

                Compatibilidad resultado = compatibilidadDAO.verificar(idP1, idP2);
                listaResultadosComp.getChildren().add(
                        crearFilaResultado(nomP1, nomP2, resultado)
                );
                if (resultado != null) hayAlgunaRegla = true;
            }
        }

        // Si ningún par tiene regla definida avisa al vendedor
        if (!hayAlgunaRegla) {
            Label lbl = new Label(
                    "ℹ️ Ningún par de productos tiene reglas de compatibilidad definidas.\n" +
                            "El administrador puede agregarlas en el módulo Compatibilidad."
            );
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
            lbl.setWrapText(true);
            listaResultadosComp.getChildren().add(lbl);
        }

        panelCompatibilidad.setVisible(true);
        panelCompatibilidad.setManaged(true);
    }

    /**
     * Crea una fila visual del resultado de compatibilidad
     * para un par de productos del carrito.
     */
    private javafx.scene.layout.HBox crearFilaResultado(
            String nomP1, String nomP2, Compatibilidad resultado) {

        String icono, color, texto;

        if (resultado == null) {
            icono = "❓"; color = "#6c757d";
            texto = nomP1 + " ↔ " + nomP2 + " — Sin regla definida";
        } else {
            switch (resultado.getEstado()) {
                case Compatibilidad.COMPATIBLE -> {
                    icono = "✅"; color = "#2dc653";
                    texto = nomP1 + " ↔ " + nomP2 + " — Compatible";
                }
                case Compatibilidad.NO_COMPATIBLE -> {
                    icono = "❌"; color = "#e94560";
                    texto = nomP1 + " ↔ " + nomP2 + " — No Compatible";
                }
                default -> {
                    icono = "⚠"; color = "#e67e00";
                    texto = nomP1 + " ↔ " + nomP2 + " — Condicional";
                }
            }
        }

        Label lblIcono = new Label(icono);
        lblIcono.setStyle("-fx-font-size: 13px;");

        Label lblTexto = new Label(texto);
        lblTexto.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + color + ";");
        javafx.scene.layout.HBox.setHgrow(lblTexto,
                javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(
                8, lblIcono, lblTexto
        );
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Si hay restricción la muestra en una segunda línea
        if (resultado != null && resultado.getRestriccion() != null
                && !resultado.getRestriccion().isEmpty()) {
            Label lblNota = new Label("   ↳ " + resultado.getRestriccion());
            lblNota.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;" +
                    "-fx-font-style: italic;");
            lblNota.setWrapText(true);
            javafx.scene.layout.VBox wrapper =
                    new javafx.scene.layout.VBox(2, fila, lblNota);
            return new javafx.scene.layout.HBox(wrapper);
        }

        return fila;
    }


    //UTILIDADES
    private void limpiarFormProducto() {
        cbProducto.setValue(null);
        cbProducto.getEditor().clear();
        txtCantidad.clear();
        txtPrecioUnitario.clear();
        lblStockDisponible.setText("");
        configurarComboProducto();
    }

    private void limpiarFormulario() {
        cbCliente.setValue(null);
        cbCliente.getEditor().clear();
        configurarComboCliente();
        detalleActual.clear();
        txtDescuento.setText("0.00");
        actualizarTotal();
        limpiarFormProducto();
        cargarDatosSesion();
        panelCompatibilidad.setVisible(false);
        panelCompatibilidad.setManaged(false);
        listaResultadosComp.getChildren().clear();
    }
}