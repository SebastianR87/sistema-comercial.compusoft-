package pe.utp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pe.utp.dao.KardexDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.model.Producto;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class KardexController implements AccesoControlable {

    // Selector
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    // Ficha
    @FXML private VBox  panelFicha;
    @FXML private Label lblProducto;
    @FXML private Label lblCategoria;
    @FXML private Label lblExistenciaActual;
    @FXML private Label lblStockMinimo;
    @FXML private Label lblStockMaximo;

    // Tabla
    @FXML private VBox  panelTabla;
    @FXML private TableView<KardexDAO.MovimientoKardex> tablaKardex;

    @FXML private TableColumn<KardexDAO.MovimientoKardex, String>  colFecha;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, String>  colDetalle;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Integer> colEntradaCant;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colEntradaCosto;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colEntradaTotal;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Integer> colSalidaCant;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colSalidaCosto;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colSalidaTotal;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Integer> colSaldoCant;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colSaldoCosto;
    @FXML private TableColumn<KardexDAO.MovimientoKardex, Double>  colSaldoTotal;

    // Totales
    @FXML private Label lblTotalEntradas;
    @FXML private Label lblTotalSalidas;
    @FXML private Label lblSaldoActual;
    @FXML private Button btnImprimir;

    private KardexDAO kardexDAO  = new KardexDAO();
    private ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        configurarTabla();
        configurarComboProducto();
        // Fecha por defecto: primer día del mes actual hasta hoy
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
    }

    @Override
    public void aplicarPermisos() {}

    private void configurarComboProducto() {
        List<Producto> todos = productoDAO.listar();
        ObservableList<Producto> lista = FXCollections.observableArrayList(todos);

        cbProducto.setEditable(true);
        cbProducto.setItems(lista);
        cbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto p) {
                return p == null ? "" : p.getIdProducto() + " - " + p.getNombre();
            }
            @Override public Producto fromString(String s) {
                return lista.stream()
                        .filter(p -> (p.getIdProducto() + " - " + p.getNombre()).equals(s))
                        .findFirst().orElse(null);
            }
        });

        cbProducto.getEditor().textProperty().addListener((obs, old, newVal) -> {
            Producto sel = null;
            try { Object v = cbProducto.getValue(); if (v instanceof Producto pv) sel = pv; }
            catch (Exception ignored) {}
            String textoSel = sel != null
                    ? sel.getIdProducto() + " - " + sel.getNombre() : null;
            if (sel != null && textoSel.equals(newVal)) return;
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
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().fecha));
        colDetalle.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().tipo + "\n" + d.getValue().documento));

        // Entradas — celdas vacías si es 0
        colEntradaCant.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().entradaCant).asObject());
        colEntradaCant.setCellFactory(col -> celdaEntera());

        colEntradaCosto.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().entradaCosto).asObject());
        colEntradaCosto.setCellFactory(col -> celdaDecimal(false));

        colEntradaTotal.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().entradaTotal).asObject());
        colEntradaTotal.setCellFactory(col -> celdaDecimal(false));

        // Salidas
        colSalidaCant.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().salidaCant).asObject());
        colSalidaCant.setCellFactory(col -> celdaEntera());

        colSalidaCosto.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().salidaCosto).asObject());
        colSalidaCosto.setCellFactory(col -> celdaDecimal(false));

        colSalidaTotal.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().salidaTotal).asObject());
        colSalidaTotal.setCellFactory(col -> celdaDecimal(false));

        // Existencias/Saldo
        colSaldoCant.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().saldoCant).asObject());

        colSaldoCosto.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().saldoCosto).asObject());
        colSaldoCosto.setCellFactory(col -> celdaDecimal(true));

        colSaldoTotal.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().saldoTotal).asObject());
        colSaldoTotal.setCellFactory(col -> celdaDecimal(true));
    }

    /**
     * Celda que muestra el entero pero deja vacío si es 0.
     * Así entradas/salidas con valor 0 no se confunden con datos reales.
     */
    private TableCell<KardexDAO.MovimientoKardex, Integer> celdaEntera() {
        return new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        };
    }

    /**
     * Celda decimal que muestra vacío si es 0.
     * bold=true para la columna de Existencias.
     */
    private TableCell<KardexDAO.MovimientoKardex, Double> celdaDecimal(boolean bold) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                } else {
                    setText(String.format("S/ %.2f", item));
                    setStyle("-fx-alignment: CENTER-RIGHT;" +
                            (bold ? "-fx-font-weight: bold;" : ""));
                }
            }
        };
    }

    /**
     * Imprime el Kárdex completo: ficha + tabla de movimientos.
     * Usa PrinterJob de JavaFX igual que en el comprobante de venta.
     */
    @FXML
    private void imprimirKardex() {
        javafx.print.PrinterJob job =
                javafx.print.PrinterJob.createPrinterJob();
        if (job == null) return;

        boolean ok = job.showPrintDialog(
                tablaKardex.getScene().getWindow());
        if (!ok) return;

        javafx.print.PageLayout layout = job.getPrinter()
                .createPageLayout(
                        javafx.print.Paper.A4,
                        javafx.print.PageOrientation.LANDSCAPE,
                        javafx.print.Printer.MarginType.HARDWARE_MINIMUM
                );

        // Imprime directamente el panelTabla sin moverlo
        // para que la ventana no quede en blanco
        double anchoOriginal = panelTabla.getPrefWidth();
        panelTabla.setPrefWidth(layout.getPrintableWidth());
        panelTabla.applyCss();
        panelTabla.layout();

        boolean exito = job.printPage(layout, panelTabla);

        // Restaura el ancho original
        panelTabla.setPrefWidth(anchoOriginal);
        panelTabla.applyCss();
        panelTabla.layout();

        if (exito) job.endJob();
    }

    @FXML
    private void generarKardex() {
        Object val = cbProducto.getValue();
        Producto prod = (val instanceof Producto p) ? p : null;

        if (prod == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona un producto para generar el Kárdex.")
                    .showAndWait();
            return;
        }

        // Obtiene datos completos del producto (con categoría, min, max)
        Producto datos = kardexDAO.obtenerProducto(prod.getIdProducto());
        if (datos == null) return;

        // Carga la ficha del producto
        lblProducto.setText(datos.getNombre());
        lblCategoria.setText(
                datos.getCategoria() != null ? datos.getCategoria().getNombre() : "—"
        );
        String stockStr = String.valueOf(datos.getStock());
        int fontSize = stockStr.length() <= 3 ? 28 :
                stockStr.length() <= 5 ? 24 : 20;
        lblExistenciaActual.setStyle(
                "-fx-font-size: " + fontSize + "px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " +
                        (datos.getStockMinimo() > 0 &&
                                datos.getStock() < datos.getStockMinimo()
                                ? "#e94560" : "#4361ee") + ";"
        );
        lblExistenciaActual.setText(stockStr);

        String minStr = String.valueOf(datos.getStockMinimo());
        int fontMin = minStr.length() <= 3 ? 28 :
                minStr.length() <= 5 ? 24 : 20;
        lblStockMinimo.setStyle(
                "-fx-font-size: " + fontMin + "px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #e67e00;"
        );
        lblStockMinimo.setText(minStr);

        String maxStr = String.valueOf(datos.getStockMaximo());
        int fontMax = maxStr.length() <= 3 ? 28 :
                maxStr.length() <= 5 ? 24 : 20;
        lblStockMaximo.setStyle(
                "-fx-font-size: " + fontMax + "px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #2dc653;"
        );
        lblStockMaximo.setText(maxStr);

        // Alerta visual si stock bajo mínimo
        // Mantiene el font-size calculado arriba y solo cambia el color
        if (datos.getStockMinimo() > 0 &&
                datos.getStock() < datos.getStockMinimo()) {
            lblExistenciaActual.setStyle(
                    "-fx-font-size: " + fontSize + "px;" +
                            "-fx-font-weight: bold; -fx-text-fill: #e94560;"
            );
            lblExistenciaActual.setText(stockStr + " ⚠");
        }

        // Obtiene todos los movimientos sin filtro
        List<KardexDAO.MovimientoKardex> todos =
                kardexDAO.listarMovimientos(prod.getIdProducto());

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        List<KardexDAO.MovimientoKardex> movimientos;

        if (desde != null && hasta != null) {
            movimientos = todos.stream()
                    .filter(m -> {
                        try {
                            java.time.LocalDateTime ldt =
                                    java.time.LocalDateTime.parse(m.fecha,
                                            java.time.format.DateTimeFormatter
                                                    .ofPattern("dd/MM/yyyy HH:mm"));
                            LocalDate fechaMov = ldt.toLocalDate();
                            return !fechaMov.isBefore(desde) &&
                                    !fechaMov.isAfter(hasta);
                        } catch (Exception ignored) { return true; }
                    })
                    .collect(Collectors.toList());
        } else {
            movimientos = todos;
        }

        tablaKardex.setItems(
                FXCollections.observableArrayList(movimientos));

        // Calcula totales
        int totalEntradas = movimientos.stream()
                .mapToInt(m -> m.entradaCant).sum();
        int totalSalidas  = movimientos.stream()
                .mapToInt(m -> m.salidaCant).sum();
        int saldo = totalEntradas - totalSalidas;

        lblTotalEntradas.setText(totalEntradas + " UND");
        lblTotalSalidas.setText(totalSalidas + " UND");
        lblSaldoActual.setText(saldo + " UND");

        // Muestra los paneles
        panelFicha.setVisible(true);
        panelFicha.setManaged(true);
        panelTabla.setVisible(true);
        panelTabla.setManaged(true);
        btnImprimir.setVisible(true);
        btnImprimir.setManaged(true);
    }

    @FXML
    private void limpiar() {
        cbProducto.setValue(null);
        cbProducto.getEditor().clear();
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        panelFicha.setVisible(false);
        panelFicha.setManaged(false);
        panelTabla.setVisible(false);
        panelTabla.setManaged(false);
        btnImprimir.setVisible(false);
        btnImprimir.setManaged(false);
        tablaKardex.getItems().clear();
    }
}