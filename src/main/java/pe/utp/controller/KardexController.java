package pe.utp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.print.Paper;
import pe.utp.dao.KardexDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Producto;
import pe.utp.security.Sesion;
import pe.utp.util.RangoFechaUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KardexController implements AccesoControlable {

    // Margen físico chico y explícito para el PDF (mismo valor que
    // Reportes): reemplaza Printer.MarginType.DEFAULT, que reserva un
    // margen generoso y variable según el driver de impresión y deja
    // franjas laterales grandes.
    private static final double MARGEN_IMPRESION_PT = 15;

    // Selector
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    // Atajos de rango de fechas (Hoy/Últimos 7 días/Este mes/Este
    // año): ToggleGroup para que el atajo activo quede marcado
    // visualmente (ver CSS .btn-neutral:selected). Se limpia solo si
    // el usuario edita Desde/Hasta a mano (ver initialize()), no
    // cuando el propio atajo es el que actualiza esas fechas.
    @FXML private ToggleGroup grupoRangoFechas;
    private boolean actualizandoRangoPreset = false;

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

    // ===== Panel de impresión (oculto, solo para PDF) =====
    @FXML private VBox panelImpresionKardex;
    @FXML private Label lblImpKardexEmision;
    @FXML private Label lblImpKardexGeneradoPor;
    @FXML private Label lblImpKardexPeriodo;
    @FXML private Label lblImpKardexCategoria;
    @FXML private Label lblImpKardexProducto;
    @FXML private Label lblImpKardexCodigo;
    @FXML private Label lblImpKardexStockMin;
    @FXML private Label lblImpKardexStockMax;
    @FXML private Label lblImpKardexExistencia;
    @FXML private Label lblImpKardexTotalEntradas;
    @FXML private Label lblImpKardexTotalSalidas;
    @FXML private Label lblImpKardexSaldo;
    @FXML private Label lblImpKardexPagina;
    @FXML private Label lblPieKardexFecha;
    @FXML private GridPane gridImpresionKardex;

    private KardexDAO kardexDAO  = new KardexDAO();
    private ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        configurarTabla();
        configurarComboProducto();
        // Fecha por defecto: primer día del mes actual hasta hoy
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        // Si el usuario edita Desde/Hasta directamente (no vía un
        // atajo de rango), el atajo que hubiera quedado marcado ya no
        // representa el rango real: se des-selecciona para no mentir
        // visualmente sobre qué filtro está activo.
        dpDesde.valueProperty().addListener((obs, anterior, nuevo) -> limpiarSeleccionRangoSiEsManual());
        dpHasta.valueProperty().addListener((obs, anterior, nuevo) -> limpiarSeleccionRangoSiEsManual());
    }

    private void limpiarSeleccionRangoSiEsManual() {
        if (!actualizandoRangoPreset && grupoRangoFechas.getSelectedToggle() != null) {
            grupoRangoFechas.selectToggle(null);
        }
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
                    setText(String.format(Locale.US, "S/ %.2f", item));
                    setStyle("-fx-alignment: CENTER-RIGHT;" +
                            (bold ? "-fx-font-weight: bold;" : ""));
                }
            }
        };
    }

    /**
     * Imprime el Kárdex completo: tabla de movimientos.
     * Usa PrinterJob de JavaFX igual que en el comprobante de venta.
     */
    @FXML
    private void imprimirKardex() {
        panelImpresionKardex.setVisible(true);

        javafx.print.PrinterJob job =
                javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            panelImpresionKardex.setVisible(false);
            return;
        }

        boolean ok = job.showPrintDialog(
                tablaKardex.getScene().getWindow());
        if (!ok) {
            panelImpresionKardex.setVisible(false);
            return;
        }

        // PORTRAIT, igual que Reportes (Ventas/Compras): antes este
        // reporte usaba Landscape, justificado por su densidad de 11
        // columnas (6 monetarias). Ese análisis quedó superado por un
        // rediseño explícito de la tabla de impresión (columnas más
        // angostas, sin prefijo "S/" repetido en cada celda, fecha sin
        // hora -- ver construirTablaImpresionKardex) que sí entra
        // cómodo en el ancho imprimible de A4 Portrait (~565pt con
        // este margen). Mantener Portrait en los tres reportes da una
        // identidad visual consistente en todo el sistema.
        //
        // A4 forzado explícitamente + margen físico chico
        // (MARGEN_IMPRESION_PT) en vez de Printer.MarginType.DEFAULT:
        // mismo fix ya aplicado en Reportes, por la misma razón
        // (DEFAULT deja franjas laterales grandes y variables según
        // el driver de impresión).
        javafx.print.Printer impresora = job.getPrinter();
        Paper papelA4 = impresora.getPrinterAttributes().getSupportedPapers().stream()
                .filter(p -> p.getName() != null && p.getName().toUpperCase(Locale.ROOT).contains("A4"))
                .findFirst()
                .orElse(Paper.A4);

        javafx.print.PageLayout layout = impresora.createPageLayout(
                papelA4,
                javafx.print.PageOrientation.PORTRAIT,
                MARGEN_IMPRESION_PT, MARGEN_IMPRESION_PT,
                MARGEN_IMPRESION_PT, MARGEN_IMPRESION_PT
        );

        imprimirTablaKardexPaginada(job, layout);

        job.endJob();
        panelImpresionKardex.setVisible(false);
    }

    /**
     * Reparte los movimientos en tantas páginas horizontales como haga
     * falta para que el texto no tenga que encogerse hasta volverse
     * ilegible (un producto con mucho movimiento en el periodo puede
     * tener muchas más filas de las que caben cómodamente en una sola
     * hoja).
     *
     * Arma la tabla dos veces solo para MEDIR: una vez vacía (para
     * saber cuánto ocupa el encabezado con la ficha del producto) y
     * una vez con todos los movimientos (para saber cuánto ocupa cada
     * fila en promedio, a este ancho). Con esos dos datos se calcula
     * cuántas filas entran por página, en vez de adivinar un número
     * fijo que se rompería si más adelante cambia el tamaño de fuente.
     */
    private void imprimirTablaKardexPaginada(javafx.print.PrinterJob job, javafx.print.PageLayout layout) {
        List<KardexDAO.MovimientoKardex> todos = tablaKardex.getItems();
        double anchoPagina = layout.getPrintableWidth();
        double altoPagina = layout.getPrintableHeight();

        if (todos.isEmpty()) {
            construirTablaImpresionKardex(todos, 1, 1);
            imprimirNodoKardex(job, layout, altoPagina);
            return;
        }

        construirTablaImpresionKardex(List.of(), 1, 1);
        panelImpresionKardex.setPrefWidth(anchoPagina);
        panelImpresionKardex.applyCss();
        double altoEncabezado = panelImpresionKardex.prefHeight(anchoPagina);

        construirTablaImpresionKardex(todos, 1, 1);
        panelImpresionKardex.setPrefWidth(anchoPagina);
        panelImpresionKardex.applyCss();
        double altoConTodos = panelImpresionKardex.prefHeight(anchoPagina);

        double altoPorFila = Math.max(1, (altoConTodos - altoEncabezado) / todos.size());
        double altoDisponibleFilas = altoPagina - altoEncabezado;

        // Colchón de seguridad del 4% (mismo fix ya aplicado en
        // Reportes): el alto de fila es un PROMEDIO medido en
        // pantalla, y sin este margen un cálculo justo al límite podía
        // dejar la última fila de cada página recortada u ocupando el
        // margen inferior de la hoja.
        double altoDisponibleConMargen = altoDisponibleFilas * 0.96;
        int filasPorPagina = Math.max(1, (int) Math.floor(altoDisponibleConMargen / altoPorFila));

        int totalPaginas = (int) Math.ceil((double) todos.size() / filasPorPagina);
        for (int pagina = 0; pagina < totalPaginas; pagina++) {
            int desde = pagina * filasPorPagina;
            int hasta = Math.min(desde + filasPorPagina, todos.size());
            construirTablaImpresionKardex(todos.subList(desde, hasta), pagina + 1, totalPaginas);
            imprimirNodoKardex(job, layout, altoPagina);
        }
    }

    /**
     * Imprime panelImpresionKardex a su tamaño REAL, sin ningún
     * escalado: SIEMPRE al alto COMPLETO de la página (altoObjetivo),
     * no al alto natural de su contenido. Esto es lo que permite que
     * el espaciador flexible (Region con VBox.vgrow="ALWAYS") puesto
     * antes del pie de página se expanda de verdad y empuje el footer
     * hasta el margen inferior, sin importar cuántas filas tenga esa
     * página en particular -- mismo patrón ya usado en Reportes.
     *
     * filasPorPagina ya se calcula (con su colchón de seguridad del
     * 4%) para que el contenido de CADA página quepa completo dentro
     * de este alto, así que ya no hace falta un Scale de emergencia
     * como antes: ese Scale podía además achicar visualmente el texto
     * de la última página si el cálculo de filas se pasaba un poco,
     * dando un resultado inconsistente con el resto del documento.
     */
    private void imprimirNodoKardex(javafx.print.PrinterJob job, javafx.print.PageLayout layout, double altoObjetivo) {
        if (altoObjetivo <= 0) {
            return;
        }

        double anchoOriginal = panelImpresionKardex.getPrefWidth();
        double altoOriginal = panelImpresionKardex.getPrefHeight();
        double anchoPagina = layout.getPrintableWidth();

        panelImpresionKardex.setPrefWidth(anchoPagina);
        panelImpresionKardex.setPrefHeight(altoObjetivo);
        panelImpresionKardex.resize(anchoPagina, altoObjetivo);
        panelImpresionKardex.applyCss();
        panelImpresionKardex.layout();

        // panelImpresionKardex usa un GridPane estático (ver
        // construirTablaImpresionKardex) en vez del TableView en vivo,
        // así que no depende de la virtualización de tablaKardex para
        // dibujarse completo. snapshot() fuerza una pasada de
        // layout/render síncrona y completa justo antes de imprimir.
        panelImpresionKardex.snapshot(new SnapshotParameters(), null);

        job.printPage(layout, panelImpresionKardex);

        panelImpresionKardex.setPrefWidth(anchoOriginal);
        panelImpresionKardex.setPrefHeight(altoOriginal);
        panelImpresionKardex.applyCss();
        panelImpresionKardex.layout();
    }

    /**
     * Arma la tabla de impresión como un GridPane "plano" (puros
     * Labels, sin virtualización) a partir de la porción de
     * movimientos que le toca a esta página. Ver el comentario en el
     * FXML sobre por qué no se imprime tablaKardex directamente.
     *
     * @param movimientos  los movimientos a mostrar en ESTA página
     *                     (puede ser una porción del total, ver
     *                     imprimirTablaKardexPaginada)
     * @param numPagina    número de página (1-based)
     * @param totalPaginas total de páginas que ocupa la tabla completa
     */
    private void construirTablaImpresionKardex(
            List<KardexDAO.MovimientoKardex> movimientos, int numPagina, int totalPaginas) {
        // Encabezado: se reutilizan los mismos datos que ya están en
        // pantalla (ficha del producto y totales), igual que se hace
        // en Reportes con prepararPortada(). No se incluye un campo de
        // "Proveedor" único: un kárdex reúne movimientos de varios
        // proveedores distintos a lo largo del periodo, así que un
        // solo valor no representaría bien esa columna (sí aparece el
        // detalle de cada movimiento en la tabla).
        lblImpKardexEmision.setText("EMISIÓN: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String usuario = Sesion.getEmpleado() != null
                ? Sesion.getEmpleado().getNombre() : "Sistema";
        lblImpKardexGeneradoPor.setText("Generado por: " + usuario);

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde != null && hasta != null) {
            lblImpKardexPeriodo.setText(
                    desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " al " +
                            hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        lblImpKardexCategoria.setText(lblCategoria.getText());
        lblImpKardexProducto.setText(lblProducto.getText());
        // Código: mismo producto ya seleccionado en el combo (idProducto),
        // no una consulta ni un campo nuevo -- solo se muestra en el PDF.
        Object productoSeleccionado = cbProducto.getValue();
        lblImpKardexCodigo.setText(
                productoSeleccionado instanceof Producto p ? p.getIdProducto() : "");
        lblImpKardexStockMin.setText(lblStockMinimo.getText());
        lblImpKardexStockMax.setText(lblStockMaximo.getText());
        lblImpKardexExistencia.setText(lblExistenciaActual.getText());

        lblImpKardexTotalEntradas.setText(lblTotalEntradas.getText());
        lblImpKardexTotalSalidas.setText(lblTotalSalidas.getText());
        lblImpKardexSaldo.setText(lblSaldoActual.getText());

        lblImpKardexPagina.setText(totalPaginas > 1
                ? "Página " + numPagina + " de " + totalPaginas
                : "");
        lblPieKardexFecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        gridImpresionKardex.getChildren().clear();
        gridImpresionKardex.getColumnConstraints().clear();

        // Porcentajes retuneados para Portrait (antes pensados para
        // el ancho mayor de Landscape): sin el prefijo "S/ " repetido
        // en cada celda (ver formatoDecimal) y con la fecha sin hora
        // (ver más abajo), cada valor monetario cabe cómodo en columnas
        // más angostas. Suma exacta 100.
        double[] porcentajes = {9, 12, 7, 9, 9, 7, 9, 9, 7, 10, 12};
        for (double porcentaje : porcentajes) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(porcentaje);
            gridImpresionKardex.getColumnConstraints().add(columna);
        }

        // Fila 0: encabezados de grupo, cada uno con su propio color
        // (verde=entra, rojo/rosa=sale, azul=saldo) -- los mismos tres
        // colores ya usados en las tarjetas de resumen en pantalla y
        // en el PDF, para que la agrupación se distinga de un vistazo
        // en vez de los tres bloques en el mismo tono oscuro plano.
        agregarEncabezadoGrupo("", 0, 0, 2, "#64748B");
        agregarEncabezadoGrupo("ENTRADAS", 2, 0, 3, "#166534");
        agregarEncabezadoGrupo("SALIDAS", 5, 0, 3, "#E94560");
        agregarEncabezadoGrupo("EXISTENCIAS", 8, 0, 3, "#315E9E");

        // Fila 1: encabezados de columna. Existencias usa "C. Promedio"
        // y "Valor Total" (no "C. Unitario"/"Total" genérico): saldoCosto
        // es un costo PROMEDIO acumulado, no el costo de un movimiento
        // puntual como en Entradas/Salidas.
        String[] encabezados = {
                "Fecha", "Detalle", "Cantidad", "C. Unitario", "Total",
                "Cantidad", "C. Unitario", "Total", "Cantidad", "C. Promedio", "Valor Total"
        };
        for (int col = 0; col < encabezados.length; col++) {
            Label etiqueta = new Label(encabezados[col]);
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            String bordeGrupo = (col == 4 || col == 7)
                    ? "-fx-border-color: transparent #FFFFFF55 transparent transparent; -fx-border-width: 0 1 0 0; "
                    : "";
            etiqueta.setStyle("-fx-font-size: 7.5px; -fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #315E9E; -fx-padding: 4 3; -fx-alignment: CENTER; "
                    + bordeGrupo);
            etiqueta.setAlignment(javafx.geometry.Pos.CENTER);
            gridImpresionKardex.add(etiqueta, col, 1);
        }

        for (int fila = 0; fila < movimientos.size(); fila++) {
            KardexDAO.MovimientoKardex m = movimientos.get(fila);
            // Solo la fecha (sin hora): m.fecha llega como
            // "dd/MM/yyyy HH:mm" desde KardexDAO (la hora ahí sirve de
            // desempate al ordenar movimientos del mismo día, no es
            // información que el reporte impreso necesite mostrar).
            String soloFecha = m.fecha != null && m.fecha.contains(" ")
                    ? m.fecha.substring(0, m.fecha.indexOf(' ')) : m.fecha;
            String[] valores = {
                    soloFecha, m.tipo + " " + m.documento,
                    formatoEntero(m.entradaCant), formatoDecimal(m.entradaCosto), formatoDecimal(m.entradaTotal),
                    formatoEntero(m.salidaCant), formatoDecimal(m.salidaCosto), formatoDecimal(m.salidaTotal),
                    formatoEntero(m.saldoCant), formatoDecimal(m.saldoCosto), formatoDecimal(m.saldoTotal)
            };
            String fondo = (fila % 2 == 0) ? "white" : "#F8FAFC";
            // Detalle en verde/rojo según sea un movimiento de entrada
            // o de salida (mismo criterio que ya usan los totales:
            // entradaCant/salidaCant, no una nueva regla de negocio),
            // para que el tipo de movimiento se distinga sin tener que
            // leer las columnas numéricas.
            String colorDetalle = m.entradaCant != 0 ? "#166534"
                    : (m.salidaCant != 0 ? "#E94560" : "#1F2A44");
            for (int col = 0; col < valores.length; col++) {
                Label celda = new Label(valores[col]);
                celda.setMaxWidth(Double.MAX_VALUE);
                String colorTexto = (col == 1) ? colorDetalle : "#1F2A44";
                String pesoFuente = (col == 1) ? "bold" : "normal";
                String bordeGrupo = (col == 4 || col == 7)
                        ? "-fx-border-color: transparent #E2E8F0 transparent transparent; -fx-border-width: 0 1 0 0; "
                        : "";
                celda.setStyle("-fx-font-size: 8px; -fx-text-fill: " + colorTexto + "; "
                        + "-fx-font-weight: " + pesoFuente + "; "
                        + "-fx-background-color: " + fondo + "; -fx-padding: 3; " + bordeGrupo);
                if (col >= 2) {
                    celda.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                }
                gridImpresionKardex.add(celda, col, fila + 2);
            }
        }
    }

    private void agregarEncabezadoGrupo(String texto, int columna, int fila, int span, String color) {
        Label etiqueta = new Label(texto);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        etiqueta.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-background-color: " + color + "; -fx-padding: 4 3; -fx-alignment: CENTER;");
        etiqueta.setAlignment(javafx.geometry.Pos.CENTER);
        gridImpresionKardex.add(etiqueta, columna, fila);
        GridPane.setColumnSpan(etiqueta, span);
    }

    /** Igual que celdaEntera(): vacío si es 0, para no confundir con datos reales. */
    private String formatoEntero(int valor) {
        return valor == 0 ? "" : String.valueOf(valor);
    }

    /**
     * Igual que celdaDecimal(): vacío si es 0. Sin el prefijo "S/ ":
     * en una tabla de 11 columnas angostas repetirlo en cada celda
     * monetaria costaba espacio horizontal real; la unidad (Soles) ya
     * queda indicada una sola vez en el título de la sección de la
     * tabla (ver Kardex.fxml). Con separador de miles para que un
     * total grande siga siendo legible en una columna angosta.
     */
    private String formatoDecimal(double valor) {
        return valor == 0 ? "" : String.format(Locale.US, "%,.2f", valor);
    }

    @FXML
    private void generarKardex() {
        Object val = cbProducto.getValue();
        Producto prod = (val instanceof Producto p) ? p : null;

        if (prod == null) {
            CSDialog.warning("Producto no seleccionado", "Selecciona un producto para generar el Kárdex.");
            return;
        }

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) {
            CSDialog.warning("Rango de fechas incompleto", "Selecciona el rango de fechas (Desde / Hasta).");
            return;
        }
        if (desde.isAfter(hasta)) {
            CSDialog.warning("Rango de fechas inválido", "La fecha 'Desde' no puede ser posterior a 'Hasta'.");
            return;
        }
        if (hasta.isAfter(LocalDate.now())) {
            CSDialog.warning("Fecha inválida", "La fecha 'Hasta' no puede ser una fecha futura.");
            return;
        }

        // Obtiene datos completos del producto (con categoría, min, max)
        Producto datos = kardexDAO.obtenerProducto(prod.getIdProducto());
        if (datos == null) return;

        // Obtiene TODOS los movimientos sin filtro (se necesita antes
        // de pintar la ficha: "Existencia Actual" ya no lee
        // datos.getStock()).
        List<KardexDAO.MovimientoKardex> todos =
                kardexDAO.listarMovimientos(prod.getIdProducto());

        // "Existencia Actual" = saldo acumulado del ÚLTIMO movimiento
        // del histórico COMPLETO (no filtrado por rango de fechas),
        // es decir el mismo cálculo fila-por-fila que ya usa la
        // columna "Existencias" de la tabla y "Saldo Final" -- NUNCA
        // datos.getStock().
        //
        // Antes esta ficha leía producto.stock, una columna aparte que
        // Compra/Venta van sumando/restando en cada operación. En la
        // práctica esa columna puede desincronizarse del histórico real
        // de lotes (una compra anulada mal revertida, un dato heredado
        // de antes de migrar al costeo FIFO, una fila corregida a mano
        // en la base de datos, etc.) y quedar así de forma PERMANENTE,
        // sin que nada la corrija -- el usuario detectó exactamente
        // este caso: dos compras (2 y 8/10 unidades) que la propia
        // tabla del Kárdex sumaba bien, pero la ficha mostraba un
        // número mayor y persistente. Al derivar "Existencia Actual"
        // del mismo histórico que ya alimenta la tabla, ambos números
        // quedan atados a una única fuente de verdad: si alguna vez
        // vuelven a no coincidir, es porque la tabla también está mal,
        // no porque haya dos cálculos corriendo en paralelo.
        int existenciaReal = todos.isEmpty()
                ? 0 : todos.get(todos.size() - 1).saldoCant;

        // Carga la ficha del producto
        lblProducto.setText(datos.getNombre());
        lblCategoria.setText(
                datos.getCategoria() != null ? datos.getCategoria().getNombre() : "—"
        );
        String stockStr = String.valueOf(existenciaReal);
        int fontSize = stockStr.length() <= 3 ? 28 :
                stockStr.length() <= 5 ? 24 : 20;
        lblExistenciaActual.setStyle(
                "-fx-font-size: " + fontSize + "px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " +
                        (datos.getStockMinimo() > 0 &&
                                existenciaReal < datos.getStockMinimo()
                                ? "#E94560" : "#315E9E") + ";"
        );
        lblExistenciaActual.setText(stockStr);

        String minStr = String.valueOf(datos.getStockMinimo());
        int fontMin = minStr.length() <= 3 ? 28 :
                minStr.length() <= 5 ? 24 : 20;
        lblStockMinimo.setStyle(
                "-fx-font-size: " + fontMin + "px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #92400E;"
        );
        lblStockMinimo.setText(minStr);

        String maxStr = String.valueOf(datos.getStockMaximo());
        int fontMax = maxStr.length() <= 3 ? 28 :
                maxStr.length() <= 5 ? 24 : 20;
        lblStockMaximo.setStyle(
                "-fx-font-size: " + fontMax + "px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #166534;"
        );
        lblStockMaximo.setText(maxStr);

        // Alerta visual si stock bajo mínimo
        // Mantiene el font-size calculado arriba y solo cambia el color
        if (datos.getStockMinimo() > 0 &&
                existenciaReal < datos.getStockMinimo()) {
            lblExistenciaActual.setStyle(
                    "-fx-font-size: " + fontSize + "px;" +
                            "-fx-font-weight: bold; -fx-text-fill: #E94560;"
            );
            lblExistenciaActual.setText(stockStr + " ⚠");
        }

        List<KardexDAO.MovimientoKardex> movimientos = todos.stream()
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

        tablaKardex.setItems(
                FXCollections.observableArrayList(movimientos));

        // Calcula totales
        int totalEntradas = movimientos.stream()
                .mapToInt(m -> m.entradaCant).sum();
        int totalSalidas  = movimientos.stream()
                .mapToInt(m -> m.salidaCant).sum();

        // "Saldo al corte" = existencia acumulada REAL al final del
        // rango seleccionado, tomada del saldo que KardexDAO ya
        // acumula fila por fila sobre el histórico COMPLETO (no un
        // neto entradas-salidas de solo el rango filtrado). Ese neto
        // era el bug: si el producto ya tenía stock antes de "desde",
        // el neto del periodo no incluye ese arrastre y el número no
        // cuadraba con "Existencia Actual" ni con la columna
        // Existencias de la propia tabla. Si el rango no tiene
        // movimientos propios, se usa el saldo del último movimiento
        // anterior a "hasta" (no 0).
        int saldo = !movimientos.isEmpty()
                ? movimientos.get(movimientos.size() - 1).saldoCant
                : saldoAcumuladoHasta(todos, hasta);

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

    /**
     * Saldo acumulado (histórico completo, sin filtrar) justo hasta
     * la fecha "hasta", para cuando el rango elegido no tiene ningún
     * movimiento propio. "todos" ya viene ordenado ascendente por
     * fecha (ver KardexDAO.listarMovimientos), así que basta recorrer
     * y quedarse con el último saldoCant cuya fecha no sea posterior
     * a "hasta".
     */
    private int saldoAcumuladoHasta(List<KardexDAO.MovimientoKardex> todos, LocalDate hasta) {
        int saldo = 0;
        for (KardexDAO.MovimientoKardex m : todos) {
            try {
                LocalDate fechaMov = java.time.LocalDateTime.parse(m.fecha,
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")).toLocalDate();
                if (fechaMov.isAfter(hasta)) break;
                saldo = m.saldoCant;
            } catch (Exception ignored) { }
        }
        return saldo;
    }

    // ===== Atajos rápidos de rango de fechas =====
    // Solo ajustan dpDesde/dpHasta; el usuario sigue presionando
    // "Generar" para aplicar el filtro (mismo flujo de siempre). La
    // fórmula de cada rango vive en RangoFechaUtil (compartida con
    // Reportes) para no duplicar el mismo cálculo en dos controllers.

    @FXML
    private void rangoHoy() {
        aplicarRango(RangoFechaUtil.hoy());
    }

    @FXML
    private void rangoUltimos7Dias() {
        aplicarRango(RangoFechaUtil.ultimos7Dias());
    }

    @FXML
    private void rangoEsteMes() {
        aplicarRango(RangoFechaUtil.esteMes());
    }

    @FXML
    private void rangoEsteAnio() {
        aplicarRango(RangoFechaUtil.esteAnio());
    }

    private void aplicarRango(LocalDate[] rango) {
        actualizandoRangoPreset = true;
        dpDesde.setValue(rango[0]);
        dpHasta.setValue(rango[1]);
        actualizandoRangoPreset = false;
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