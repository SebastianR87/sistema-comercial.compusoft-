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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.*;
import pe.utp.dialog.CSDialog;
import pe.utp.model.*;
import javafx.scene.layout.HBox;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;
import pe.utp.util.FormatoMoneda;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import pe.utp.model.Cotizacion;
import pe.utp.model.DetalleCotizacion;

public class VentaController implements AccesoControlable {

    // Pestaña Nueva Venta
    @FXML private TextField txtNumeroComprobante;
    @FXML private ComboBox<TipoComprobante> cbTipoComprobante;
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<MetodoPago> cbMetodoPago;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtFecha;
    @FXML private VBox panelPagoEfectivo;

    @FXML private ComboBox<Producto> cbProducto;
    @FXML private Label lblStockDisponible;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioUnitario;

    @FXML private TableView<DetalleVenta> tablaDetalle;
    @FXML private TableColumn<DetalleVenta, String>  colProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, Double>  colPrecioUnit;
    @FXML private TableColumn<DetalleVenta, Double>  colSubtotal;
    @FXML private TableColumn<DetalleVenta, Void>    colQuitarDet;

    @FXML private TextField txtMontoPagado;
    @FXML private Label lblVuelto;
    @FXML private TextField txtDescuento;
    @FXML private Label lblSubtotal;
    @FXML private Label lblBaseImponible;
    @FXML private Label lblIgv;
    @FXML private Label lblTotal;

    // Pestaña Historial
    @FXML private TextField txtBuscarHistorial;
    @FXML private TableView<Venta> tablaHistorial;
    @FXML private TableColumn<Venta, String> colIdVenta;
    @FXML private TableColumn<Venta, String> colComprobante;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, String> colEmpleado;
    @FXML private TableColumn<Venta, String> colMetodoPago;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, Void> colAccionesHist;
    @FXML private TableColumn<DetalleVenta, String> colCodigo;

    // DAOs
    private VentaDAO ventaDAO = new VentaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProductoDAO productoDAO = new ProductoDAO();
    private TipoComprobanteDAO tipoCompDAO = new TipoComprobanteDAO();
    private MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    // Carrito de la venta actual
    private ObservableList<DetalleVenta> detalleActual =
            FXCollections.observableArrayList();

    // Historial filtrado
    private ObservableList<Venta> listaHistorial =
            FXCollections.observableArrayList();
    private FilteredList<Venta> listaFiltrada;

    // Constante de IGV: 18%
    private static final double IGV = 0.18;

    // Almacena el total actual para calcular el vuelto sin leer el Label
    private double totalActual = 0;

    @FXML
    public void initialize() {
        configurarTablaDetalle();
        configurarTablaHistorial();
        configurarComboTipoComprobante();
        configurarComboMetodoPago();
        configurarComboCliente();
        configurarComboProducto();
        cargarDatosSesion();
        cargarHistorial();
        actualizarTotal();
    }

    @Override
    public void aplicarPermisos() {
        // Administrador y Vendedor tienen acceso completo
    }

    // CARGA INICIAL
    private void cargarDatosSesion() {
        Empleado emp = Sesion.getEmpleado();
        if (emp != null) txtEmpleado.setText(emp.getNombre());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        txtFecha.setText(LocalDateTime.now().format(fmt));
    }

    /**
     * Tipo de comprobante: Boleta o Factura.
     * Al elegir uno, regenera el número de comprobante
     * con el prefijo correspondiente (B001 o F001).
     */
    private void configurarComboTipoComprobante() {
        List<TipoComprobante> tipos = tipoCompDAO.listar();
        cbTipoComprobante.setItems(FXCollections.observableArrayList(tipos));

        cbTipoComprobante.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String prefijo = obtenerPrefijoComprobante(newV.getNombre());
            txtNumeroComprobante.setText(
                    ventaDAO.generarNumeroComprobante(prefijo)
            );
        });

        // Selecciona Boleta por defecto si existe
        tipos.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Boleta"))
                .findFirst()
                .ifPresent(cbTipoComprobante::setValue);
    }

    /**
     * Determina el prefijo del número de comprobante
     * según el nombre del tipo: Boleta → B001, Factura → F001.
     * Cualquier otro tipo usa "T001" como genérico.
     */
    private String obtenerPrefijoComprobante(String nombreTipo) {
        if (nombreTipo.equalsIgnoreCase("Boleta")) return "B001";
        if (nombreTipo.equalsIgnoreCase("Factura")) return "F001";
        return "T001";
    }

    private void configurarComboMetodoPago() {
        cbMetodoPago.setItems(
                FXCollections.observableArrayList(metodoPagoDAO.listar())
        );

        // Muestra el campo de monto pagado/vuelto solo si el método de pago es efectivo
        cbMetodoPago.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                panelPagoEfectivo.setVisible(false);
                panelPagoEfectivo.setManaged(false);
                return;
            }
            boolean esEfectivo = newV.getMetodoDePago()
                    .equalsIgnoreCase("Efectivo");
            panelPagoEfectivo.setVisible(esEfectivo);
            panelPagoEfectivo.setManaged(esEfectivo);

            // Si no es efectivo, limpia los campos y el vuelto
            // porque ya no son relevantes
            if (!esEfectivo) {
                txtMontoPagado.clear();
                lblVuelto.setText("S/ 0.00");
            }
        });
    }

    /** ComboBox de cliente con búsqueda integrada, igual que Proveedor en Compra. */
    private void configurarComboCliente() {
        List<Cliente> todos = clienteDAO.listar();
        ObservableList<Cliente> listaClientes =
                FXCollections.observableArrayList(todos);

        cbCliente.setEditable(true);
        cbCliente.setItems(listaClientes);

        cbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getNombre() + " - " + c.getNumeroDocumento();
            }
            @Override
            public Cliente fromString(String s) {
                return listaClientes.stream()
                        .filter(c -> (c.getNombre() + " - " + c.getNumeroDocumento()).equals(s))
                        .findFirst().orElse(null);
            }
        });

        cbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Cliente sel = null;
            try {
                Object v = cbCliente.getValue();
                if (v instanceof Cliente cv) sel = cv;
            } catch (Exception ignored) {}

            String textoSel = sel != null
                    ? sel.getNombre() + " - " + sel.getNumeroDocumento() : null;
            if (sel != null && textoSel.equals(newVal)) return;

            if (sel != null) cbCliente.setValue(null);

            String texto = newVal == null ? "" : newVal.toLowerCase();
            ObservableList<Cliente> filtrados = FXCollections.observableArrayList(
                    todos.stream()
                            .filter(c ->
                                    texto.isEmpty() ||
                                            c.getNombre().toLowerCase().contains(texto) ||
                                            c.getNumeroDocumento().toLowerCase().contains(texto))
                            .toList()
            );
            cbCliente.setItems(filtrados);

            if (!cbCliente.isShowing() && !texto.isEmpty()) cbCliente.show();
        });
    }

    /** ComboBox de producto con búsqueda integrada. Solo productos con stock > 0. */
    private void configurarComboProducto() {
        List<Producto> todos = productoDAO.listar().stream()
                .filter(p -> "Activo".equals(p.getEstado()) && p.getStock() > 0)
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

        cbProducto.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Producto sel = null;
            try {
                Object v = cbProducto.getValue();
                if (v instanceof Producto pv) sel = pv;
            } catch (Exception ignored) {}

            if (sel != null && sel.getNombre().equals(newVal)) return;
            if (sel != null) cbProducto.setValue(null);

            String texto = newVal == null ? "" : newVal.toLowerCase();
            ObservableList<Producto> filtrados = FXCollections.observableArrayList(
                    todos.stream()
                            .filter(p ->
                                    texto.isEmpty() ||
                                            p.getNombre().toLowerCase().contains(texto) ||
                                            p.getIdProducto().toLowerCase().contains(texto))
                            .toList()
            );
            cbProducto.setItems(filtrados);

            if (!cbProducto.isShowing() && !texto.isEmpty()) cbProducto.show();
        });

        // Al seleccionar producto: rellena precio y muestra stock disponible
        cbProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Producto p) {
                txtPrecioUnitario.setText(String.format("%.2f", p.getPrecioVenta()));
                // Descuenta lo que ya está en el carrito para mostrar el stock real disponible
                int enCarrito = detalleActual.stream()
                        .filter(d -> d.getProducto().getIdProducto()
                                .equals(p.getIdProducto()))
                        .mapToInt(DetalleVenta::getCantidad)
                        .sum();
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

    // TABLA DETALLE (CARRITO)
    private void
    configurarTablaDetalle() {
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
        colPrecioUnit.setCellFactory(FormatoMoneda.celda());
        colSubtotal.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject()
        );
        colSubtotal.setCellFactory(FormatoMoneda.celda());

        colQuitarDet.setCellFactory(col -> new TableCell<>() {
            final Button btnQuitar = new Button("✕");
            {
                btnQuitar.setStyle(
                        "-fx-background-color: #fcebeb; -fx-text-fill: #a32d2d;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;"
                );
                btnQuitar.setOnAction(e -> {
                    DetalleVenta det = getTableView().getItems().get(getIndex());
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

    /**
     * Agrega un producto al detalle.
     * Valida stock disponible considerando lo que ya está
     * en el carrito (si el mismo producto se agrega dos veces).
     */
    @FXML
    private void agregarProducto() {
        Object val = cbProducto.getValue();
        Producto producto = (val instanceof Producto p) ? p : null;

        String cantStr   = txtCantidad.getText().trim();
        String precioStr = txtPrecioUnitario.getText().trim();

        if (producto == null || cantStr.isEmpty() || precioStr.isEmpty()) {
            CSDialog.warning("Producto no seleccionado",
                    "Selecciona un producto de la lista. Escribe para buscar y luego haz clic en la opción.");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantStr);
            double precio = Double.parseDouble(precioStr.replace(",", "."));

            if (cantidad <= 0) {
                CSDialog.warning("Cantidad inválida", "La cantidad debe ser mayor a cero.");
                return;
            }
            if (precio <= 0) {
                CSDialog.warning("Precio inválido", "El precio debe ser mayor a cero.");
                return;
            }

            // Calcula cuánto de este producto ya está en el carrito
            int cantidadEnCarrito = detalleActual.stream()
                    .filter(d -> d.getProducto().getIdProducto()
                            .equals(producto.getIdProducto()))
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();

            // Valida contra el stock real del producto
            if (cantidadEnCarrito + cantidad > producto.getStock()) {
                CSDialog.warning("Stock insuficiente",
                        "Stock insuficiente para \"" + producto.getNombre() + "\". " +
                                "Disponible: " + producto.getStock() + " unidades. " +
                                "Ya tienes " + cantidadEnCarrito + " en el carrito.");
                return;
            }

            // Si el producto ya está en el detalle CON EL MISMO PRECIO,
            // suma la cantidad. Si el precio es distinto, se agrega como
            // línea nueva (ej: descuento manual a una unidad puntual) --
            // así cada línea refleja el precio real al que se vendió.
            for (DetalleVenta det : detalleActual) {
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

            String idDetalle = "DTV" + String.format("%03d", detalleActual.size() + 1);
            detalleActual.add(new DetalleVenta(idDetalle, null, producto, cantidad, precio));
            actualizarTotal();
            limpiarFormProducto();

        } catch (NumberFormatException e) {
            CSDialog.warning("Datos inválidos", "Cantidad y precio deben ser números válidos.");
        }
    }

    // TOTALES: subtotal, descuento, IGV, total
    @FXML
    private void actualizarTotal() {
        double subtotal = detalleActual.stream()
                .mapToDouble(DetalleVenta::getSubtotal)
                .sum();

        double descuento = 0;
        try {
            descuento = Double.parseDouble(
                    txtDescuento.getText().trim().replace(",", ".")
            );
        } catch (NumberFormatException ignored) {}

        if (descuento < 0) descuento = 0;
        if (descuento > subtotal) descuento = subtotal;

        // Base e IGV se calculan sobre el subtotal (antes de descuento)
        // igual que en una boleta SUNAT: Op. Gravada + IGV son
        // el desglose de los productos vendidos
        double base = subtotal / (1 + IGV);
        double igv  = subtotal - base;

        // El descuento se aplica al final, sobre el importe total
        double total = subtotal - descuento;

        lblBaseImponible.setText(String.format("S/ %.2f", base));
        lblIgv.setText(String.format("S/ %.2f", igv));
        lblTotal.setText(String.format("S/ %.2f", total));

        // Subtotal como referencia interna (gris, pequeño)
        lblSubtotal.setText("Subtotal productos: S/ " + String.format("%.2f", subtotal));

        // Guarda el total para que calcularVuelto() lo use directamente
        totalActual = total;
        // Actualiza el vuelto si ya hay un monto pagado ingresado
        if (txtMontoPagado != null &&
                !txtMontoPagado.getText().trim().isEmpty()) {
            calcularVuelto();
        }

    }

    @FXML
    private void calcularVuelto() {
        try {
            double pagado = Double.parseDouble(
                    txtMontoPagado.getText().trim().replace(",", ".")
            );
            // Usa totalActual en vez de leer el Label
            // para evitar errores de parsing con el formato "S/ 50.00"
            double vuelto = pagado - totalActual;
            if (vuelto < 0) {
                lblVuelto.setText("S/ --.-- (insuficiente)");
                lblVuelto.setStyle(
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                                "-fx-text-fill: #e94560;");
            } else {
                lblVuelto.setText(String.format("S/ %.2f", vuelto));
                lblVuelto.setStyle(
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                                "-fx-text-fill: #495057;");
            }
        } catch (NumberFormatException ignored) {
            lblVuelto.setText("S/ 0.00");
        }
    }


    // REGISTRAR VENTA
    @FXML
    private void registrarVenta() {
        if (!PermisoService.puedeEditarVenta()) {
            PermisoUtil.denegado();
            return;
        }

        Object clienteObj = cbCliente.getValue();
        Cliente cliente = (clienteObj instanceof Cliente c) ? c : null;

        TipoComprobante tipoComp = cbTipoComprobante.getValue();
        MetodoPago metodoPago = cbMetodoPago.getValue();

        // Validación 1: campos obligatorios de la cabecera
        if (cliente == null) {
            CSDialog.warning("Cliente no seleccionado",
                    "Selecciona un cliente del listado. Escribe el nombre o documento y haz clic en una opción.");
            return;
        }
        if (tipoComp == null || metodoPago == null) {
            CSDialog.warning("Campos incompletos", "Selecciona el tipo de comprobante y método de pago.");
            return;
        }

        // Validación 2: al menos un producto
        if (detalleActual.isEmpty()) {
            CSDialog.warning("Detalle vacío", "Agrega al menos un producto al detalle.");
            return;
        }

        // Validación 3: Factura solo se puede emitir a clientes con RUC
        if (tipoComp.getNombre().equalsIgnoreCase("Factura")) {
            String tipoDoc = cliente.getTipoDocumento() != null
                    ? cliente.getTipoDocumento().getDocumento() : "";
            if (!tipoDoc.equalsIgnoreCase("RUC")) {
                CSDialog.warning("Comprobante inválido",
                        "La Factura solo puede emitirse a clientes con RUC. " +
                                "El cliente seleccionado tiene " + tipoDoc + ". " +
                                "Selecciona Boleta o elige un cliente con RUC.");
                return;
            }
        }

        // Recalcula montos finales
        double subtotal = detalleActual.stream()
                .mapToDouble(DetalleVenta::getSubtotal).sum();
        double descuento = 0;
        try {
            descuento = Double.parseDouble(
                    txtDescuento.getText().trim().replace(",", ".")
            );
        } catch (NumberFormatException ignored) {}
        // Antes, si el descuento superaba el subtotal, aquí se
        // reseteaba a 0 (cobrando el subtotal COMPLETO), mientras que
        // actualizarTotal() -- la vista previa en pantalla que el
        // vendedor ve antes de guardar -- lo recorta al subtotal
        // (mostrando total S/0.00). Es decir: la pantalla mostraba un
        // total y el guardado real cobraba OTRO mayor, sin avisar.
        // Ahora ambos usan el mismo recorte (clamp al subtotal), para
        // que lo que se ve en pantalla sea exactamente lo que se
        // guarda.
        if (descuento < 0) descuento = 0;
        if (descuento > subtotal) descuento = subtotal;

        double total = subtotal - descuento;

        String idVenta = generarId();
        String numComp = txtNumeroComprobante.getText();

        Venta venta = new Venta(
                idVenta, cliente, Sesion.getEmpleado(),
                tipoComp, metodoPago, numComp,
                LocalDateTime.now(), descuento, total
        );

        boolean esEfectivo = metodoPago.getMetodoDePago()
                .equalsIgnoreCase("Efectivo");
        if (esEfectivo) {
            try {
                double pagado = Double.parseDouble(
                        txtMontoPagado.getText().trim().replace(",", ".")
                );
                // Validación: el monto pagado no puede ser menor al total
                if (pagado < total) {
                    CSDialog.warning("Monto insuficiente",
                            "El monto pagado (S/ " + String.format("%.2f", pagado) +
                                    ") es menor al total (S/ " + String.format("%.2f", total) + "). " +
                                    "Por favor ingresa el monto correcto.");
                    return;
                }
                venta.setMontoPagado(pagado);
                venta.setVuelto(pagado - total);
            } catch (NumberFormatException e) {
                CSDialog.warning("Monto pagado requerido", "Ingresa el monto pagado por el cliente.");
                return;
            }
        } else {
            // Para Tarjeta/Yape/Transferencia el pago es exacto
            venta.setMontoPagado(total);
            venta.setVuelto(0);
        }

        // Genera IDs definitivos del detalle
        List<DetalleVenta> detallesFinales = new ArrayList<>();
        int i = 1;
        for (DetalleVenta det : detalleActual) {
            det.setIdDetalleVenta(idVenta + "-D" + String.format("%03d", i++));
            detallesFinales.add(det);
        }

        String resultado = ventaDAO.registrarVenta(venta, detallesFinales);

        if (resultado.equals("OK")) {
            // Verifica si algún producto quedó bajo el stock mínimo
            StringBuilder alertaMin = new StringBuilder();
            for (DetalleVenta det : detallesFinales) {
                Producto actualizado = productoDAO.buscarPorId(
                        det.getProducto().getIdProducto()
                );
                if (actualizado != null &&
                        actualizado.getStockMinimo() > 0 &&
                        actualizado.getStock() < actualizado.getStockMinimo()) {
                    alertaMin.append("• ").append(actualizado.getNombre())
                            .append(": stock actual ").append(actualizado.getStock())
                            .append(" (mínimo: ").append(actualizado.getStockMinimo())
                            .append(")\n");
                }
            }

            String msg = "Venta " + numComp + " registrada correctamente.\n" +
                    "El stock ha sido actualizado.";

            if (alertaMin.length() > 0) {
                msg += "\n\n⚠ Stock bajo mínimo:\n" + alertaMin +
                        "\nConsidera realizar una compra pronto.";
            }

            CSDialog.success("Venta registrada", msg);
            limpiarFormulario();
            cargarHistorial();

        } else if (resultado.startsWith("STOCK_INSUFICIENTE:")) {
            String[] partes = resultado.split(":");
            CSDialog.warning("Stock insuficiente",
                    "Stock insuficiente para \"" + partes[1] + "\". " +
                            "Disponible: " + partes[2] + " unidades.\n\n" +
                            "Otro usuario pudo haber vendido este producto. " +
                            "Actualiza el carrito e intenta nuevamente.");
            limpiarFormulario();
        } else {
            CSDialog.error("Error", "No se pudo registrar la venta. Revisa la consola para más detalles.");
        }
    }

    // HISTORIAL
    private void configurarTablaHistorial() {
        colIdVenta.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        colComprobante.setCellValueFactory(new PropertyValueFactory<>("numeroComprobante"));

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
        colMetodoPago.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getMetodoPago() != null
                                ? data.getValue().getMetodoPago().getMetodoDePago() : ""
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
                    Venta v = getTableView().getItems().get(getIndex());
                    mostrarDetalleVenta(v);
                });

                btnAnular.getStyleClass().add("btn-table-delete");
                btnAnular.setOnAction(e -> {
                    Venta v = getTableView().getItems().get(getIndex());
                    confirmarYAnular(v);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Venta v = getTableView().getItems().get(getIndex());
                boolean yaAnulada = "ANULADA".equalsIgnoreCase(v.getEstado());
                boolean puedeAnular = PermisoService.puedeAnularVenta();

                btnAnular.setVisible(puedeAnular);
                btnAnular.setManaged(puedeAnular);
                btnAnular.setDisable(yaAnulada);

                contenedor.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(contenedor);
            }
        });

        listaFiltrada = new FilteredList<>(listaHistorial, v -> true);
        tablaHistorial.setItems(listaFiltrada);
    }

    /** Abre el modal con formato de boleta del historial. */
    private void mostrarDetalleVenta(Venta v) {
        List<DetalleVenta> detalle = ventaDAO.listarDetalle(v.getIdVenta());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/VentaDetalleModal.fxml")
            );
            Parent root = loader.load();

            VentaDetalleModalController ctrl = loader.getController();
            ctrl.cargarDatos(v, detalle, IGV);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle("Comprobante de Venta");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir el detalle: " + ex.getMessage());
        }
    }

    private void confirmarYAnular(Venta v) {
        if (!PermisoService.puedeAnularVenta()) {
            PermisoUtil.denegado();
            return;
        }

        if ("ANULADA".equalsIgnoreCase(v.getEstado())) {
            CSDialog.info("Venta anulada", "Esta venta ya está anulada.");
            return;
        }

        boolean confirmado = CSDialog.confirm(
                "Confirmar anulación",
                "¿Anular la venta " + v.getNumeroComprobante() + "?\n\n" +
                        "Esta acción devolverá el stock de los productos vendidos. " +
                        "La venta quedará marcada como ANULADA y no se eliminará del historial.",
                "Anular", "Cancelar", true, true);
        if (!confirmado) return;

        String resultado = ventaDAO.anularVenta(v.getIdVenta());

        switch (resultado) {
            case "OK" -> {
                CSDialog.success("Venta anulada",
                        "Venta " + v.getNumeroComprobante() + " anulada correctamente. " +
                                "El stock ha sido devuelto.");
                cargarHistorial();
            }
            case "YA_ANULADA" -> CSDialog.warning("Venta anulada", "Esta venta ya estaba anulada.");
            case "NO_EXISTE" -> CSDialog.error("Error", "No se encontró la venta.");
            default -> CSDialog.error("Error", "No se pudo anular la venta. Revisa la consola para más detalles.");
        }
    }

    @FXML
    private void filtrarHistorial() {
        String texto = txtBuscarHistorial.getText().trim().toLowerCase();
        listaFiltrada.setPredicate(v -> {
            if (texto.isEmpty()) return true;
            boolean enComp = v.getNumeroComprobante().toLowerCase().contains(texto);
            boolean enCliente = v.getCliente() != null &&
                    v.getCliente().getNombre().toLowerCase().contains(texto);
            return enComp || enCliente;
        });
    }

    private void cargarHistorial() {
        listaHistorial.setAll(ventaDAO.listar());
    }

    // Generación del ID
    private String generarId() {
        String ultimo = ventaDAO.obtenerUltimoId();
        if (ultimo == null) return "VEN001";
        String prefijo   = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        int numero = numeroStr.isEmpty() ? 1 : Integer.parseInt(numeroStr) + 1;
        return String.format("%s%03d", prefijo, numero);
    }

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

        cbMetodoPago.setValue(null);
        panelPagoEfectivo.setVisible(false);
        panelPagoEfectivo.setManaged(false);
        txtMontoPagado.clear();
        lblVuelto.setText("S/ 0.00");
        detalleActual.clear();
        txtDescuento.setText("0.00");
        actualizarTotal();
        limpiarFormProducto();
        cargarDatosSesion();

        // Regenera comprobante con el tipo seleccionado
        TipoComprobante t = cbTipoComprobante.getValue();
        if (t != null) {
            txtNumeroComprobante.setText(
                    ventaDAO.generarNumeroComprobante(obtenerPrefijoComprobante(t.getNombre()))
            );
        }
    }

    /**
     * Pre-carga el formulario de venta con los datos de una cotización.
     * Se llama desde CotizacionController al hacer clic en "→ Venta".
     * El vendedor solo necesita elegir tipo de comprobante y método de pago.
     */
    public void preCargarDesdeCotizacion(Cotizacion cotizacion,
                                         List<DetalleCotizacion> detalleCot) {
        // Pre-selecciona el cliente
        // Busca el cliente completo en el ComboBox para que
        // el StringConverter lo muestre correctamente
        cbCliente.getItems().stream()
                .filter(c -> c instanceof Cliente &&
                        ((Cliente) c).getIdCliente()
                                .equals(cotizacion.getCliente().getIdCliente()))
                .findFirst()
                .ifPresent(cbCliente::setValue);

        // Si no lo encontró (por el filtrado del combo), lo setea directo
        if (cbCliente.getValue() == null) {
            cbCliente.getEditor().setText(
                    cotizacion.getCliente().getNombre() + " - " +
                            cotizacion.getCliente().getNumeroDocumento()
            );
        }

        // Pre-carga los productos de la cotización en el carrito
        detalleActual.clear();
        int i = 1;
        for (DetalleCotizacion dc : detalleCot) {
            DetalleVenta dv = new DetalleVenta(
                    "TEMP" + String.format("%03d", i++),
                    null,
                    dc.getProducto(),
                    dc.getCantidad(),
                    dc.getPrecio()
            );
            detalleActual.add(dv);
        }

        // Pre-carga el descuento
        txtDescuento.setText(
                String.format("%.2f", cotizacion.getDescuento())
        );

        // Recalcula los totales con los productos pre-cargados
        actualizarTotal();

        // Verifica si el precio de la cotización difiere del precio actual
        // Puede pasar si entre la cotización y la conversión a venta
        // hubo una compra que actualizó el CPP y por ende el precio de venta
        StringBuilder preciosDiferentes = new StringBuilder();
        for (DetalleCotizacion dc : detalleCot) {
            Producto actual = productoDAO.buscarPorId(
                    dc.getProducto().getIdProducto()
            );
            if (actual != null &&
                    Math.abs(dc.getPrecio() - actual.getPrecioVenta()) > 0.01) {
                preciosDiferentes.append("• ")
                        .append(dc.getProducto().getNombre())
                        .append(": cotizado S/ ")
                        .append(String.format("%.2f", dc.getPrecio()))
                        .append(" → precio actual S/ ")
                        .append(String.format("%.2f", actual.getPrecioVenta()))
                        .append("\n");
            }
        }

        // Construye el aviso con o sin diferencias de precio
        String avisoPrecios = preciosDiferentes.length() > 0
                ? "\n\n⚠ Precios cambiaron desde la cotización:\n" +
                  preciosDiferentes +
                  "Verifica si usar el precio cotizado o el actual."
                : "";

        CSDialog.info("Cotización cargada",
                "Cotización " + cotizacion.getIdCotizacion() + " cargada.\n\n" +
                        "Productos y cliente pre-cargados.\n" +
                        "Solo falta seleccionar:\n" +
                        "  • Tipo de comprobante (Boleta/Factura)\n" +
                        "  • Método de pago\n" +
                        avisoPrecios);
    }
}