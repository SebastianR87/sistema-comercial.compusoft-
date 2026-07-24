package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import pe.utp.dao.ReporteDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.security.Sesion;
import pe.utp.util.RangoFechaUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportesController {

    private static final String VENTAS = "Ventas";
    private static final String COMPRAS = "Compras";

    @FXML private ComboBox<String> cbTipo;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    // Atajos de rango de fechas (Hoy/Últimos 7 días/Este mes/Este año)
    // ToggleGroup para que el atajo activo quede marcado
    // visualmente (ver CSS .btn-neutral:selected). Se limpia solo si
    // el usuario edita Desde/Hasta a mano (ver initialize()), no
    // cuando el propio atajo es el que actualiza esas fechas.
    @FXML private ToggleGroup grupoRangoFechas;
    private boolean actualizandoRangoPreset = false;

    @FXML private Button btnExportarPdf;

    @FXML private GridPane panelResumen;
    @FXML private Label lblTituloTotal;
    @FXML private Label lblTotal;
    @FXML private Label lblTituloCantidad;
    @FXML private Label lblCantidad;
    @FXML private Label lblTituloPromedio;
    @FXML private Label lblPromedio;
    @FXML private StackPane panelKpi4;
    @FXML private Label lblTituloKpi4;
    @FXML private Label lblKpi4;
    @FXML private javafx.scene.Group iconKpi4Persona;
    @FXML private javafx.scene.Group iconKpi4Caja;

    @FXML private VBox panelGraficos;
    @FXML private Label lblTituloTendencia;
    @FXML private LineChart<String, Number> chartTendencia;
    @FXML private PieChart chartCategoria;
    @FXML private Label lblTituloTop;
    @FXML private BarChart<Number, String> chartTop;
    @FXML private NumberAxis ejeXTop;

    @FXML private VBox panelTabla;
    @FXML private TableView<FilaReporte> tablaDetalle;
    @FXML private TableColumn<FilaReporte, String> colFecha;
    @FXML private TableColumn<FilaReporte, String> colComprobante;
    @FXML private TableColumn<FilaReporte, String> colEntidad;
    @FXML private TableColumn<FilaReporte, String> colProducto;
    @FXML private TableColumn<FilaReporte, String> colCantidad;
    @FXML private TableColumn<FilaReporte, String> colPrecio;
    @FXML private TableColumn<FilaReporte, String> colSubtotal;

    // Layout exclusivo para PDF (oculto en pantalla): bloques
    // independientes que exportarPdf() reparte entre páginas. Ver el
    // comentario grande sobre esta sección en Reportes.fxml.
    @FXML private VBox panelImpresionPaginaBloques;
    @FXML private VBox panelImpresionBloques;

    @FXML private VBox panelImpEncabezado;
    @FXML private Label lblImpEmision;
    @FXML private Label lblImpGeneradoPor;
    @FXML private Label lblImpTipoReporte;

    @FXML private VBox panelImpInfo;
    @FXML private Label lblImpTitulo;
    @FXML private Label lblImpPeriodo;

    @FXML private VBox panelImpKpis;
    @FXML private Label lblImpTotalTitulo;
    @FXML private Label lblImpTotal;
    @FXML private Label lblImpCantidadTitulo;
    @FXML private Label lblImpCantidad;
    @FXML private Label lblImpPromedioTitulo;
    @FXML private Label lblImpPromedio;
    @FXML private VBox panelImpKpi4;
    @FXML private Label lblImpTituloKpi4;
    @FXML private Label lblImpValorKpi4;

    @FXML private VBox panelImpChartTendencia;
    @FXML private Label lblImpTituloTendencia;
    @FXML private ImageView imgTendencia;

    // Fila Categoría + Top Productos: ambos gráficos ahora comparten
    // una misma fila (GridPane 50/50) en vez de ir uno debajo del
    // otro. lblImpTituloCategoria tiene fx:id solo para poder medir
    // su alto real al repartir el espacio disponible.
    @FXML private GridPane panelImpFilaCategoriaTop;
    @FXML private VBox panelImpChartCategoria;
    @FXML private Label lblImpTituloCategoria;
    @FXML private ImageView imgCategoria;

    @FXML private VBox panelImpChartTop;
    @FXML private Label lblImpTituloTop;
    @FXML private ImageView imgTop;

    // Espaciador flexible (Region con VBox.vgrow="ALWAYS" puesto en el
    // FXML): se intercala entre la fila de gráficos y el pie de
    // página para que éste quede SIEMPRE pegado al margen inferior de
    // la hoja, sin importar cuánto midan los gráficos.
    @FXML private Region espaciadorBloques;

    @FXML private VBox panelImpFooterBloques;
    @FXML private Label lblPieBloquesFecha;
    @FXML private Label lblPieBloquesPagina;

    // Panel de impresión de la tabla (oculto, solo para PDF)
    // Encabezado idéntico al de la página 1 (mismo logo, tipografía y
    // bloque de emisión/usuario/tipo de reporte), para que ambas
    // páginas del documento compartan la misma identidad visual.
    @FXML private VBox panelTablaImpresion;
    @FXML private Label lblImpTablaEmision;
    @FXML private Label lblImpTablaGeneradoPor;
    @FXML private Label lblImpTablaTipoReporte;
    @FXML private Label lblImpTablaTitulo;
    @FXML private Label lblImpTablaPeriodo;
    @FXML private Label lblPieTablaFecha;
    @FXML private Label lblImpTablaPagina;
    @FXML private GridPane gridTablaImpresion;

    private static final double MARGEN_IMPRESION_PT = 15;

    private static final double PADDING_PAGINA_BLOQUES = 8;

    private final ReporteDAO dao = new ReporteDAO();

    private ReporteDAO.Resumen resumenActual;
    private List<ReporteDAO.FilaDetalleVenta> ventasActuales;
    private List<ReporteDAO.FilaDetalleCompra> comprasActuales;
    private LocalDate desdeActual;
    private LocalDate hastaActual;

    /** Fila genérica para la tabla: sirve tanto para Ventas como Compras. */
    public static class FilaReporte {
        final String fecha, comprobante, entidad, producto, cantidad, precio, subtotal;
        FilaReporte(String fecha, String comprobante, String entidad, String producto,
                    int cantidad, double precio, double subtotal) {
            this.fecha = fecha;
            this.comprobante = comprobante;
            this.entidad = entidad;
            this.producto = producto;
            this.cantidad = String.valueOf(cantidad);
            this.precio = String.format(Locale.US, "S/ %.2f", precio);
            this.subtotal = String.format(Locale.US, "S/ %.2f", subtotal);
        }
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList(VENTAS, COMPRAS));
        cbTipo.setValue(VENTAS);

        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));

        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fecha));
        colComprobante.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().comprobante));
        colEntidad.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().entidad));
        colProducto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().producto));
        colCantidad.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().cantidad));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().precio));
        colSubtotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().subtotal));

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

    @FXML
    private void generarReporte() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        String tipo = cbTipo.getValue();

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

        this.desdeActual = desde;
        this.hastaActual = hasta;

        if (VENTAS.equals(tipo)) {
            generarReporteVentas(desde, hasta);
        } else {
            generarReporteCompras(desde, hasta);
        }

        panelResumen.setVisible(true);
        panelResumen.setManaged(true);
        panelGraficos.setVisible(true);
        panelGraficos.setManaged(true);
        panelTabla.setVisible(true);
        panelTabla.setManaged(true);
        btnExportarPdf.setVisible(true);
        btnExportarPdf.setManaged(true);
    }

    private void generarReporteVentas(LocalDate desde, LocalDate hasta) {
        lblTituloTotal.setText("TOTAL VENDIDO");
        lblTituloCantidad.setText("N° DE VENTAS");
        lblTituloPromedio.setText("TICKET PROMEDIO");
        lblTituloTendencia.setText("TENDENCIA DE VENTAS POR DÍA");
        lblTituloTop.setText("TOP 8 PRODUCTOS MÁS VENDIDOS");
        colEntidad.setText("Cliente");

        ReporteDAO.Resumen resumen = dao.obtenerResumenVentas(desde, hasta);
        this.resumenActual = resumen;
        lblTotal.setText(String.format(Locale.US, "S/ %,.2f", resumen.total));
        lblCantidad.setText(String.valueOf(resumen.cantidadOperaciones));
        lblPromedio.setText(String.format(Locale.US, "S/ %,.2f", resumen.promedio));

        List<ReporteDAO.PuntoTendencia> tendencia = dao.obtenerVentasPorDia(desde, hasta);
        pintarTendencia(tendencia);

        List<ReporteDAO.ProductoTop> top = dao.obtenerTopProductosVendidos(desde, hasta, 8);
        pintarTop(top);

        List<ReporteDAO.CategoriaTotal> categorias = dao.obtenerVentasPorCategoria(desde, hasta);
        pintarCategorias(categorias);

        List<ReporteDAO.FilaDetalleVenta> filas = dao.listarDetalleVentas(desde, hasta);
        this.ventasActuales = filas;
        this.comprasActuales = null;

        // 4to KPI en Ventas: clientes distintos atendidos en el periodo.
        lblTituloKpi4.setText("CLIENTES ATENDIDOS");
        long clientesAtendidos = filas.stream().map(f -> f.cliente).distinct().count();
        lblKpi4.setText(String.valueOf(clientesAtendidos));
        iconKpi4Persona.setVisible(true);
        iconKpi4Persona.setManaged(true);
        iconKpi4Caja.setVisible(false);
        iconKpi4Caja.setManaged(false);

        tablaDetalle.setItems(FXCollections.observableArrayList(
                filas.stream().map(f -> new FilaReporte(
                        f.fecha, f.comprobante, f.cliente, f.producto,
                        f.cantidad, f.precio, f.subtotal
                )).toList()
        ));
    }

    private void generarReporteCompras(LocalDate desde, LocalDate hasta) {
        lblTituloTotal.setText("TOTAL COMPRADO");
        lblTituloCantidad.setText("N° DE COMPRAS");
        lblTituloPromedio.setText("PROMEDIO POR COMPRA");
        lblTituloTendencia.setText("TENDENCIA DE COMPRAS POR DÍA");
        lblTituloTop.setText("TOP 8 PRODUCTOS MÁS COMPRADOS");
        colEntidad.setText("Proveedor");

        ReporteDAO.Resumen resumen = dao.obtenerResumenCompras(desde, hasta);
        this.resumenActual = resumen;
        lblTotal.setText(String.format(Locale.US, "S/ %,.2f", resumen.total));
        lblCantidad.setText(String.valueOf(resumen.cantidadOperaciones));
        lblPromedio.setText(String.format(Locale.US, "S/ %,.2f", resumen.promedio));

        List<ReporteDAO.PuntoTendencia> tendencia = dao.obtenerComprasPorDia(desde, hasta);
        pintarTendencia(tendencia);

        List<ReporteDAO.ProductoTop> top = dao.obtenerTopProductosComprados(desde, hasta, 8);
        pintarTop(top);

        List<ReporteDAO.CategoriaTotal> categorias = dao.obtenerComprasPorCategoria(desde, hasta);
        pintarCategorias(categorias);

        List<ReporteDAO.FilaDetalleCompra> filas = dao.listarDetalleCompras(desde, hasta);
        this.comprasActuales = filas;
        this.ventasActuales = null;

        // 4to KPI en Compras: productos distintos comprados en el periodo.
        lblTituloKpi4.setText("PRODUCTOS COMPRADOS");
        long productosComprados = filas.stream().map(f -> f.producto).distinct().count();
        lblKpi4.setText(String.valueOf(productosComprados));
        iconKpi4Caja.setVisible(true);
        iconKpi4Caja.setManaged(true);
        iconKpi4Persona.setVisible(false);
        iconKpi4Persona.setManaged(false);

        tablaDetalle.setItems(FXCollections.observableArrayList(
                filas.stream().map(f -> new FilaReporte(
                        f.fecha, f.comprobante, f.proveedor, f.producto,
                        f.cantidad, f.precio, f.subtotal
                )).toList()
        ));
    }

    private void pintarTendencia(List<ReporteDAO.PuntoTendencia> puntos) {
        chartTendencia.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (ReporteDAO.PuntoTendencia p : puntos) {
            serie.getData().add(new XYChart.Data<>(p.fecha, p.total));
        }
        chartTendencia.getData().add(serie);
    }

    /**
     * Gráfico de barras HORIZONTAL (eje X = cantidad numérica,
     * eje Y = nombre del producto como categoría). Con muchos
     * productos o nombres largos, las barras horizontales evitan
     * que las etiquetas se superpongan o queden rotadas e
     * ilegibles, a diferencia de un gráfico vertical clásico.
     * Los nombres se truncan visualmente pero el tooltip de cada
     * barra siempre muestra el nombre completo.
     */
    private void pintarTop(List<ReporteDAO.ProductoTop> productos) {
        chartTop.getData().clear();
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        for (ReporteDAO.ProductoTop p : productos) {
            String etiqueta = p.nombre.length() > 22
                    ? p.nombre.substring(0, 20) + "…"
                    : p.nombre;
            XYChart.Data<Number, String> dato = new XYChart.Data<>(p.cantidad, etiqueta);
            serie.getData().add(dato);
            dato.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, new Tooltip(
                            p.nombre + "\n" + p.cantidad + " unidades · S/ " +
                                    String.format(Locale.US, "%.2f", p.total)));
                }
            });
        }
        chartTop.getData().add(serie);

        // "cantidad" siempre es un número entero de unidades: el
        // auto-rango por defecto de NumberAxis puede elegir un
        // tickUnit fraccionario (0.25, 0.5...) cuando el máximo es
        // chico, mostrando "0.75, 1, 1.25". Se fuerza un eje con
        // pasos enteros y se formatean las etiquetas sin decimales
        // como respaldo, sin importar el rango de datos.
        int maxCantidad = productos.stream()
                .mapToInt(p -> p.cantidad)
                .max().orElse(1);
        int tickUnit = Math.max(1, (int) Math.ceil(maxCantidad / 5.0));
        int upperBound = ((maxCantidad / tickUnit) + 1) * tickUnit;

        ejeXTop.setAutoRanging(false);
        ejeXTop.setLowerBound(0);
        ejeXTop.setUpperBound(upperBound);
        ejeXTop.setTickUnit(tickUnit);
        ejeXTop.setMinorTickCount(1);
        ejeXTop.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number valor) {
                return String.valueOf(valor.intValue());
            }
            @Override public Number fromString(String texto) {
                return Integer.parseInt(texto);
            }
        });
    }

    private void pintarCategorias(List<ReporteDAO.CategoriaTotal> categorias) {
        chartCategoria.getData().clear();
        for (ReporteDAO.CategoriaTotal c : categorias) {
            chartCategoria.getData().add(new PieChart.Data(c.nombre, c.total));
        }
    }

    @FXML
    private void limpiar() {
        cbTipo.setValue(VENTAS);
        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));

        panelResumen.setVisible(false);
        panelResumen.setManaged(false);
        panelGraficos.setVisible(false);
        panelGraficos.setManaged(false);
        panelTabla.setVisible(false);
        panelTabla.setManaged(false);
        btnExportarPdf.setVisible(false);
        btnExportarPdf.setManaged(false);

        chartTendencia.getData().clear();
        chartTop.getData().clear();
        chartCategoria.getData().clear();
        tablaDetalle.getItems().clear();

        resumenActual = null;
        ventasActuales = null;
        comprasActuales = null;
    }

    // ===== Atajos rápidos de rango de fechas =====
    // Solo ajustan dpDesde/dpHasta; el usuario sigue presionando
    // "Generar Reporte" para aplicar el filtro (mismo flujo de
    // siempre). La fórmula de cada rango vive en RangoFechaUtil
    // (compartida con Kárdex) para no duplicar el mismo cálculo en
    // dos controllers.

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

    // EXPORTACIÓN A PDF
    //
    // Arquitectura: la pantalla y el PDF son dos layouts totalmente
    // independientes. El dashboard visible no se toca para nada;
    // todo lo de aquí abajo vive en nodos ocultos propios del PDF
    // (ver el bloque grande de comentarios en Reportes.fxml).
    //
    // El documento se imprime SIEMPRE en Portrait (nunca Landscape),
    // en un diseño FIJO de 2 páginas tipo reporte ejecutivo:
    //
    //   Página 1 (una sola, nunca se reparte en varias): encabezado,
    //   información del reporte, KPIs (Resumen Ejecutivo), gráfico
    //   de Tendencia a todo el ancho y, debajo, Categoría + Top
    //   Productos LADO A LADO en una misma fila (50/50, misma
    //   altura).
    //
    //   Página 2 en adelante: SOLO la tabla de movimientos, a todo
    //   el ancho, paginada por filas si no entran todas (nunca
    //   escalada horizontalmente, con los encabezados de columna
    //   repetidos en cada hoja).
    //
    // El alto de encabezado/info/KPIs/pie de página se mide tal
    // cual (nunca se recortan ni se les achica la fuente); lo que
    // sobra de la hoja se reparte entre el bloque de tendencia y la
    // fila inferior, y ESOS DOS gráficos se redimensionan a su caja
    // real ANTES de capturarlos (ver redimensionarYCapturar), para
    // que el propio motor de charts recalcule ejes/leyenda a ese
    // tamaño en vez de estirar una imagen ya capturada.

    @FXML
    private void exportarPdf() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return;
        }

        boolean ok = job.showPrintDialog(tablaDetalle.getScene().getWindow());
        if (!ok) {
            return;
        }

        Printer impresora = job.getPrinter();

        Paper papelA4 = impresora.getPrinterAttributes().getSupportedPapers().stream()
                .filter(p -> p.getName() != null && p.getName().toUpperCase(Locale.ROOT).contains("A4"))
                .findFirst()
                .orElse(Paper.A4);

        // Portrait siempre, nunca Landscape. Margen físico chico y
        // explícito (MARGEN_IMPRESION_PT) en vez de Printer.
        // MarginType.DEFAULT: DEFAULT reserva un margen generoso y
        // variable según el driver (a menudo ~0.75"/54pt por lado),
        // que sumado al padding interno dejaba franjas laterales muy
        // grandes. Con margen explícito en puntos, el ancho realmente
        // imprimible (layout.getPrintableWidth()) queda mucho más
        // cerca del ancho físico total de la hoja A4.
        PageLayout layout = impresora.createPageLayout(
                papelA4,
                PageOrientation.PORTRAIT,
                MARGEN_IMPRESION_PT, MARGEN_IMPRESION_PT,
                MARGEN_IMPRESION_PT, MARGEN_IMPRESION_PT
        );

        double anchoPagina = layout.getPrintableWidth();
        double altoPagina = layout.getPrintableHeight();
        double anchoContenido = anchoPagina - 2 * PADDING_PAGINA_BLOQUES;
        double altoUtilPagina = altoPagina - 2 * PADDING_PAGINA_BLOQUES;

        prepararTextosPagina1();

        panelImpresionPaginaBloques.setVisible(true);
        panelTablaImpresion.setVisible(true);

        // 1) Medir a su tamaño REAL los bloques de contenido fijo
        // (encabezado, info, KPIs) y el pie de página: nunca se
        // recortan ni se les cambia la fuente, así que se miden tal
        // cual y lo que sobra de la hoja se lo reparten los dos
        // gráficos flexibles (tendencia y la fila inferior).
        double spacing = panelImpresionPaginaBloques.getSpacing();
        List<VBox> bloquesFijos = List.of(panelImpEncabezado, panelImpInfo, panelImpKpis);
        double altoFijos = 0;
        for (VBox bloque : bloquesFijos) {
            bloque.setPrefWidth(anchoContenido);
            bloque.applyCss();
            altoFijos += bloque.prefHeight(anchoContenido);
        }

        panelImpFooterBloques.setPrefWidth(anchoContenido);
        panelImpFooterBloques.applyCss();
        double altoFooter = panelImpFooterBloques.prefHeight(anchoContenido);

        // 6 espacios entre bloques (encabezado-info, info-KPIs,
        // KPIs-tendencia, tendencia-fila, fila-espaciador,
        // espaciador-pie) + colchón de seguridad del 3%, mismo
        // criterio ya usado en el resto del documento para no dejar
        // nada apretado contra el borde.
        double altoDisponibleGraficos =
                (altoUtilPagina - altoFijos - altoFooter - 6 * spacing) * 0.97;

        // ── 2) Repartir ese espacio: tendencia es el protagonista
        // (55%), la fila Categoría + Top Productos se queda con el
        // resto (45%).
        double altoBloqueTendencia = altoDisponibleGraficos * 0.55;
        double altoBloqueFila = altoDisponibleGraficos - altoBloqueTendencia;

        double altoLabelTendencia = lblImpTituloTendencia.prefHeight(anchoContenido);
        double altoImagenTendencia = Math.max(80,
                altoBloqueTendencia - altoLabelTendencia - panelImpChartTendencia.getSpacing());

        double hgapFila = panelImpFilaCategoriaTop.getHgap();
        double anchoColumna = (anchoContenido - hgapFila) / 2;
        double altoLabelFila = Math.max(
                lblImpTituloCategoria.prefHeight(anchoColumna),
                lblImpTituloTop.prefHeight(anchoColumna));
        double altoImagenFila = Math.max(80,
                altoBloqueFila - altoLabelFila - panelImpChartCategoria.getSpacing());

        // ── 3) Redimensionar cada gráfico (el nodo REAL del
        // dashboard) a su caja exacta y recién ahí capturarlo: el
        // propio motor de charts recalcula ejes/leyenda para ese
        // tamaño, así que no queda nada distorsionado ni escalado
        // "a la fuerza". Categoría y Top comparten el mismo alto
        // para quedar perfectamente alineados en la fila.
        redimensionarYCapturar(chartTendencia, imgTendencia, anchoContenido, altoImagenTendencia);
        redimensionarYCapturar(chartCategoria, imgCategoria, anchoColumna, altoImagenFila);
        redimensionarYCapturar(chartTop, imgTop, anchoColumna, altoImagenFila);

        // Los bloques viven en panelImpresionBloques solo para que
        // el FXML los declare en algún lado; se sacan de ahí para
        // armar la única página 1 real (un Node solo puede tener un
        // padre a la vez).
        panelImpresionBloques.getChildren().clear();
        panelImpresionPaginaBloques.getChildren().setAll(
                panelImpEncabezado, panelImpInfo, panelImpKpis,
                panelImpChartTendencia, panelImpFilaCategoriaTop, espaciadorBloques, panelImpFooterBloques
        );

        // La tabla se mide aparte (paginación propia por filas) para
        // saber el total de páginas del documento antes de imprimir
        // la primera hoja, y que el pie de página ya diga
        // "Página 1 de N" con el N correcto desde el principio.
        List<FilaReporte> todasFilas = tablaDetalle.getItems();
        int[] infoTabla = medirPaginacionTabla(layout, todasFilas);
        int filasPorPagina = infoTabla[0];
        int totalPaginasTabla = infoTabla[1];
        int totalPaginasGlobal = 1 + totalPaginasTabla;

        String fechaHoraPie = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // ── 4) Página 1: SIEMPRE una sola página con encabezado,
        // KPIs, tendencia y la fila Categoría/Top, calculados para
        // encajar exactos en el alto disponible.
        lblPieBloquesFecha.setText(fechaHoraPie);
        lblPieBloquesPagina.setText("Página 1 de " + totalPaginasGlobal);
        imprimirNodoPlano(job, layout, panelImpresionPaginaBloques, altoPagina);
        panelImpresionPaginaBloques.getChildren().clear();

        // Devuelve los bloques a su holder oculto: deja el árbol de
        // FXML consistente para una próxima exportación en la misma
        // sesión.
        panelImpresionBloques.getChildren().setAll(
                panelImpEncabezado, panelImpInfo, panelImpKpis,
                panelImpChartTendencia, panelImpFilaCategoriaTop, espaciadorBloques, panelImpFooterBloques
        );

        // ── 5) Página(s) 2 en adelante: ÚNICAMENTE la tabla de
        // movimientos, sin escalar horizontalmente -- se prefieren
        // más páginas antes que angostar columnas o perder
        // legibilidad.
        imprimirTablaPaginada(job, layout, todasFilas, filasPorPagina,
                totalPaginasTabla, 2, totalPaginasGlobal, fechaHoraPie);

        job.endJob();
        ocultarPanelesImpresion();
    }

    private void ocultarPanelesImpresion() {
        panelImpresionPaginaBloques.setVisible(false);
        panelTablaImpresion.setVisible(false);
    }

    /**
     * Mide (sin imprimir) cuántas filas caben por página y cuántas
     * páginas ocupará la tabla completa, con el mismo truco de "armar
     * dos veces para medir" usado en el resto del sistema: una vez
     * vacía (para saber cuánto ocupa el encabezado) y una vez con
     * todas las filas (para saber cuánto ocupa cada fila en
     * promedio, a este ancho). numPagina/totalPaginas se pasan en 1
     * porque en esta llamada solo interesa medir el alto, no el
     * texto final que se imprimirá.
     */
    private int[] medirPaginacionTabla(PageLayout layout, List<FilaReporte> todas) {
        double anchoPagina = layout.getPrintableWidth();

        if (todas.isEmpty()) {
            return new int[]{1, 1};
        }

        construirTablaImpresion(List.of(), 1, 1, 1, 1);
        panelTablaImpresion.setPrefWidth(anchoPagina);
        panelTablaImpresion.applyCss();
        double altoEncabezado = panelTablaImpresion.prefHeight(anchoPagina);

        construirTablaImpresion(todas, 1, 1, 1, 1);
        panelTablaImpresion.setPrefWidth(anchoPagina);
        panelTablaImpresion.applyCss();
        double altoConTodas = panelTablaImpresion.prefHeight(anchoPagina);

        double altoPorFila = Math.max(1, (altoConTodas - altoEncabezado) / todas.size());
        double altoDisponibleFilas = layout.getPrintableHeight() - altoEncabezado;

        // Margen de seguridad del 4%: el alto de fila usado aquí es un
        // PROMEDIO medido en pantalla; el motor de impresión puede
        // redondear métricas de fuente de forma ligeramente distinta
        // al medir vs. al imprimir. Sin este colchón, un cálculo justo
        // al límite podía dejar la última fila (o el pie de página,
        // que va inmediatamente después) recortada u ocupando el
        // margen inferior de la hoja. Reservar un 4% evita ese
        // desborde sin sacrificar filas de forma notoria.
        double altoDisponibleConMargen = altoDisponibleFilas * 0.96;
        int filasPorPagina = Math.max(1, (int) Math.floor(altoDisponibleConMargen / altoPorFila));
        int totalPaginas = (int) Math.ceil((double) todas.size() / filasPorPagina);

        return new int[]{filasPorPagina, totalPaginas};
    }

    /**
     * Imprime la tabla ya paginada (filasPorPagina/totalPaginasTabla
     * ya calculados por medirPaginacionTabla), continuando la
     * numeración GLOBAL del documento a partir de paginaGlobalInicio.
     */
    private void imprimirTablaPaginada(PrinterJob job, PageLayout layout, List<FilaReporte> todas,
                                        int filasPorPagina, int totalPaginasTabla,
                                        int paginaGlobalInicio, int totalPaginasGlobal,
                                        String fechaHoraPie) {
        double altoPagina = layout.getPrintableHeight();

        if (todas.isEmpty()) {
            construirTablaImpresion(todas, 1, 1, paginaGlobalInicio, totalPaginasGlobal);
            lblPieTablaFecha.setText(fechaHoraPie);
            imprimirNodoPlano(job, layout, panelTablaImpresion, altoPagina);
            return;
        }

        for (int p = 0; p < totalPaginasTabla; p++) {
            int desde = p * filasPorPagina;
            int hasta = Math.min(desde + filasPorPagina, todas.size());
            construirTablaImpresion(todas.subList(desde, hasta), p + 1, totalPaginasTabla,
                    paginaGlobalInicio + p, totalPaginasGlobal);
            lblPieTablaFecha.setText(fechaHoraPie);
            imprimirNodoPlano(job, layout, panelTablaImpresion, altoPagina);
        }
    }

    private void imprimirNodoPlano(PrinterJob job, PageLayout layout, VBox nodo, double altoObjetivo) {
        if (altoObjetivo <= 0) {
            return;
        }

        double anchoOriginal = nodo.getPrefWidth();
        double altoOriginal = nodo.getPrefHeight();
        double anchoPagina = layout.getPrintableWidth();

        // setPrefWidth()/setPrefHeight() por sí solos NO alcanzan para
        // nodos con managed=false: al no tener un padre que los
        // redimensione automáticamente, el ancho/alto REAL del nodo se
        // queda en su valor anterior (a menudo 0), y todo su contenido
        // se calcula/comprime dentro de ese espacio casi nulo. El
        // resize() explícito fuerza el tamaño real antes de imprimir.
        nodo.setPrefWidth(anchoPagina);
        nodo.setPrefHeight(altoObjetivo);
        nodo.resize(anchoPagina, altoObjetivo);
        nodo.applyCss();
        nodo.layout();

        // snapshot() fuerza una pasada de layout/render síncrona y
        // completa justo antes de imprimir, para que ninguna fila o
        // gráfico quede a medio dibujar.
        nodo.snapshot(new SnapshotParameters(), null);

        job.printPage(layout, nodo);

        nodo.setPrefWidth(anchoOriginal);
        nodo.setPrefHeight(altoOriginal);
        nodo.applyCss();
        nodo.layout();
    }

    /**
     * Llena todos los textos de la página 1 (encabezado, info,
     * KPIs, títulos de los gráficos) con los datos del último
     * reporte generado. No toca los gráficos -- eso lo hace
     * redimensionarYCapturar() en exportarPdf(), una vez que ya se
     * conoce el tamaño real de la hoja (PageLayout).
     */
    private void prepararTextosPagina1() {
        boolean esVenta = ventasActuales != null;

        lblImpEmision.setText("Emisión: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        String usuario = Sesion.getEmpleado() != null
                ? Sesion.getEmpleado().getNombre() : "Sistema";
        lblImpGeneradoPor.setText("Generado por: " + usuario);
        lblImpTipoReporte.setText("Tipo de reporte: " + (esVenta ? "Ventas" : "Compras"));

        lblImpTitulo.setText(esVenta ? "Reporte de Ventas" : "Reporte de Compras");
        lblImpPeriodo.setText("Periodo: " +
                desdeActual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " al " +
                hastaActual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        lblImpTotalTitulo.setText(lblTituloTotal.getText());
        lblImpTotal.setText(lblTotal.getText());
        lblImpCantidadTitulo.setText(lblTituloCantidad.getText());
        lblImpCantidad.setText(lblCantidad.getText());
        lblImpPromedioTitulo.setText(lblTituloPromedio.getText());
        lblImpPromedio.setText(lblPromedio.getText());
        lblImpTituloKpi4.setText(lblTituloKpi4.getText());
        lblImpValorKpi4.setText(lblKpi4.getText());

        lblImpTituloTendencia.setText(lblTituloTendencia.getText());
        lblImpTituloTop.setText(lblTituloTop.getText());
    }

    private void redimensionarYCapturar(Chart chart, ImageView destino, double ancho, double alto) {
        if (ancho <= 0 || alto <= 0) {
            return;
        }

        double prefWOriginal = chart.getPrefWidth();
        double prefHOriginal = chart.getPrefHeight();

        chart.setPrefWidth(ancho);
        chart.setPrefHeight(alto);
        chart.resize(ancho, alto);
        chart.applyCss();
        chart.layout();

        double escalaCaptura = 3.0;
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(new javafx.scene.transform.Scale(escalaCaptura, escalaCaptura));
        destino.setImage(chart.snapshot(params, null));
        destino.setFitWidth(ancho);
        destino.setFitHeight(alto);
        destino.setPreserveRatio(false);

        chart.setPrefWidth(prefWOriginal);
        chart.setPrefHeight(prefHOriginal);
        if (chart.getParent() != null) {
            chart.getParent().requestLayout();
        }
    }

    /**
     * Arma la tabla de impresión como un GridPane "plano" (puros
     * Labels, sin virtualización) a partir de la porción de filas que
     * le toca a esta página. Se usa esto -y no tablaDetalle
     * directamente- porque un TableView es virtualizado: en pantalla
     * solo crea/renderiza las filas que caben en su alto visible, y al
     * imprimir ese nodo tal cual, muchas filas no llegaban a
     * "materializarse" y la página de la tabla salía en blanco.
     *
     * @param filas          las filas a mostrar en ESTA página (puede
     *                       ser una porción del total)
     * @param numPagina      número de página (1-based) dentro de la
     *                       tabla
     * @param totalPaginas   total de páginas que ocupa la tabla
     * @param paginaGlobal   número de página dentro de TODO el
     *                       documento (portada + Top Productos +
     *                       tabla), para el pie de página
     * @param totalPaginasGlobal total de páginas de todo el documento
     */
    private void construirTablaImpresion(List<FilaReporte> filas, int numPagina, int totalPaginas,
                                          int paginaGlobal, int totalPaginasGlobal) {
        // Mismo bloque de emisión/usuario/tipo de reporte que la
        // página 1, para que el encabezado de esta página se vea
        // idéntico (misma identidad visual en todo el documento).
        lblImpTablaEmision.setText(lblImpEmision.getText());
        lblImpTablaGeneradoPor.setText(lblImpGeneradoPor.getText());
        lblImpTablaTipoReporte.setText(lblImpTipoReporte.getText());

        lblImpTablaTitulo.setText(lblImpTitulo.getText());
        lblImpTablaPeriodo.setText(lblImpPeriodo.getText());
        lblImpTablaPagina.setText("Página " + paginaGlobal + " de " + totalPaginasGlobal);

        gridTablaImpresion.getChildren().clear();
        gridTablaImpresion.getColumnConstraints().clear();

        String[] encabezados = {
                colFecha.getText(), colComprobante.getText(), colEntidad.getText(),
                colProducto.getText(), colCantidad.getText(), colPrecio.getText(),
                colSubtotal.getText()
        };
        double[] porcentajes = {11, 14, 19, 22, 10, 12, 12};
        for (double porcentaje : porcentajes) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(porcentaje);
            gridTablaImpresion.getColumnConstraints().add(columna);
        }

        for (int col = 0; col < encabezados.length; col++) {
            Label etiqueta = new Label(encabezados[col]);
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            etiqueta.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #315E9E; -fx-padding: 5 4;");
            gridTablaImpresion.add(etiqueta, col, 0);
        }

        for (int fila = 0; fila < filas.size(); fila++) {
            FilaReporte f = filas.get(fila);
            String[] valores = {
                    f.fecha, f.comprobante, f.entidad, f.producto,
                    f.cantidad, f.precio, f.subtotal
            };
            String fondo = (fila % 2 == 0) ? "white" : "#F8FAFC";
            for (int col = 0; col < valores.length; col++) {
                Label celda = new Label(valores[col]);
                celda.setMaxWidth(Double.MAX_VALUE);
                celda.setStyle("-fx-font-size: 9px; -fx-text-fill: #1F2A44; "
                        + "-fx-background-color: " + fondo + "; -fx-padding: 4;");
                gridTablaImpresion.add(celda, col, fila + 1);
            }
        }
    }
}
