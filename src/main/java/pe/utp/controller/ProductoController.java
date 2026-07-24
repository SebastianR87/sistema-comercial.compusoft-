package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dao.LoteDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.util.FormatoMoneda;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.model.Categoria;
import pe.utp.model.Producto;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

import java.util.List;

public class ProductoController implements AccesoControlable {

    @FXML private Label lblModoConsulta;
    @FXML private ScrollPane  panelFormulario;
    @FXML private FlowPane panelCards;
    @FXML private HBox panelContenido;
    @FXML private VBox panelCards_contenedor;
    @FXML private HBox panelBreadcrumb;
    @FXML private Label lblBreadcrumbCategoria;
    @FXML private TableView<Producto> tablaProducto;
    @FXML private TableColumn<Producto, String> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Double> colPrecioCompra;
    @FXML private TableColumn<Producto, Double> colPrecioVenta;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private TableColumn<Producto, Void> colAcciones;
    @FXML private Label lblTituloFormulario;
    @FXML private Label lblTituloTabla;
    @FXML private Label lblContadorProductos;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtStockMaximo;
    @FXML private ComboBox<String> cbEstado;
    @FXML private TextField txtBuscarProducto;

    private ProductoDAO dao = new ProductoDAO();
    private LoteDAO loteDAO = new LoteDAO(pe.utp.Conexion.ConexionDB.getConexion());
    private CategoriaDAO categoriaDAO = new CategoriaDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();
    private Categoria categoriaActual = null;
    private ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Producto> listaFiltradaProductos;
    private boolean soloLectura = false;

    // Íconos
    private String obtenerIcono(String nombreCategoria) {
        return switch (nombreCategoria.toUpperCase()) {
            case "PROCESADOR" -> "⚙️";
            case "PLACA MADRE" -> "📋";
            case "MEMORIA RAM", "RAM" -> "💾";
            case "TARJETA DE VIDEO", "GPU" -> "🎮";
            case "FUENTE DE PODER", "FUENTE" -> "⚡";
            case "ALMACENAMIENTO", "DISCO M,2",
                 "DISCO SSD" -> "💿";
            case "DISIPADOR" -> "💨";
            default  -> "📦";
        };
    }

    private String obtenerColor(String nombreCategoria) {
        return switch (nombreCategoria.toUpperCase()) {
            case "PROCESADOR" -> "#4361ee";
            case "PLACA MADRE" -> "#3a0ca3";
            case "MEMORIA RAM", "RAM"  -> "#7209b7";
            case "TARJETA DE VIDEO", "GPU" -> "#f72585";
            case "FUENTE DE PODER", "FUENTE" -> "#e94560";
            case "ALMACENAMIENTO",
                 "DISCO M,2", "DISCO SSD" -> "#4cc9f0";
            case "DISIPADOR" -> "#4895ef";
            default -> "#6c757d";
        };
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(
                new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colPrecioCompra.setCellValueFactory(
                new PropertyValueFactory<>("precioCompra"));
        colPrecioCompra.setCellFactory(FormatoMoneda.celda());
        colPrecioVenta.setCellValueFactory(
                new PropertyValueFactory<>("precioVenta"));
        colPrecioVenta.setCellFactory(FormatoMoneda.celda());
        colStock.setCellValueFactory(
                new PropertyValueFactory<>("stock"));
        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        cbEstado.setItems(FXCollections.observableArrayList(
                "Activo", "Inactivo"
        ));

        configurarColumnaAcciones();
        cargarCards();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        soloLectura = !PermisoService.puedeEditarProducto();
        if (soloLectura) {
            lblModoConsulta.setVisible(true);
            lblModoConsulta.setManaged(true);
            PermisoUtil.ocultar(panelFormulario);
            PermisoUtil.ocultarColumna(colAcciones);
        }
    }

    private void cargarCards() {
        panelCards.getChildren().clear();
        List<Categoria> categorias = categoriaDAO.Listar();
        for (Categoria cat : categorias) {
            panelCards.getChildren().add(crearCard(cat));
        }
    }

    private VBox crearCard(Categoria cat) {
        String color = obtenerColor(cat.getNombre());
        String icono = obtenerIcono(cat.getNombre());
        int cantidad = dao.listarPorCategoria(
                cat.getIdCategoria()).size();

        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(150);
        card.setPrefHeight(130);
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);"
        );

        Label lblIcono = new Label(icono);
        lblIcono.setStyle("-fx-font-size: 22px;");

        Label lblNombre = new Label(cat.getNombre());
        lblIcono.setStyle("-fx-font-size: 32px;");
        lblNombre.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        lblNombre.setWrapText(true);
        lblNombre.setAlignment(Pos.CENTER);

        Label lblCantidad = new Label(cantidad + " productos");
        lblCantidad.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;"
        );

        card.getChildren().addAll(lblIcono, lblNombre, lblCantidad);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: derive(" + color + ",-20%);" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.25),15,0,0,5);" +
                        "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);"
        ));

        card.setOnMouseClicked(e -> abrirCategoria(cat));
        return card;
    }

    private void abrirCategoria(Categoria cat) {
        categoriaActual = cat;
        lblTituloFormulario.setText(soloLectura
                ? "Consulta: " + cat.getNombre()
                : "Nuevo " + cat.getNombre());
        lblTituloTabla.setText("Lista de " + cat.getNombre() + "s");
        cargarTabla();

        // Oculta las cards y muestra el breadcrumb en su lugar
        // para liberar espacio vertical para tabla y formulario
        panelCards_contenedor.setVisible(false);
        panelCards_contenedor.setManaged(false);

        panelBreadcrumb.setVisible(true);
        panelBreadcrumb.setManaged(true);
        lblBreadcrumbCategoria.setText(cat.getNombre());

        panelContenido.setVisible(true);
        panelContenido.setManaged(true);
        if (!soloLectura) generarId();
    }


    @FXML
    private void filtrarProductos() {
        String texto = txtBuscarProducto.getText().trim().toLowerCase();
        listaFiltradaProductos.setPredicate(p ->
                texto.isEmpty() ||
                        p.getNombre().toLowerCase().contains(texto) ||
                        p.getIdProducto().toLowerCase().contains(texto)
        );
        actualizarContadorProductos();
    }

    @FXML
    private void volverCards() {
        // Limpia el buscador al volver a las cards
        if (txtBuscarProducto != null) txtBuscarProducto.clear();

        panelContenido.setVisible(false);
        panelContenido.setManaged(false);
        panelBreadcrumb.setVisible(false);
        panelBreadcrumb.setManaged(false);
        panelCards_contenedor.setVisible(true);
        panelCards_contenedor.setManaged(true);

        limpiar();
        cargarCards();
    }

    private void cargarTabla() {
        List<Producto> productos = dao.listarPorCategoria(categoriaActual.getIdCategoria());

        // precio_compra ya no se guarda en producto (viene de los
        // lotes FIFO); se calcula aquí el costo promedio de
        // referencia de los lotes activos, solo para mostrarlo en
        // la tabla -- no se usa para el consumo real de stock.
        for (Producto p : productos) {
            p.setPrecioCompra(loteDAO.obtenerCostoPromedio(p.getIdProducto()));
        }

        listaProductos.setAll(productos);
        listaFiltradaProductos = new javafx.collections.transformation
                .FilteredList<>(listaProductos, p -> true);
        tablaProducto.setItems(listaFiltradaProductos);
        actualizarContadorProductos();
    }

    private void actualizarContadorProductos() {
        int total = listaFiltradaProductos != null
                ? (int) listaFiltradaProductos.stream().count()
                : listaProductos.size();
        lblContadorProductos.setText(total + " productos");
    }

    /** Configura la columna de acciones con 3 botones: */
    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {

            final Button btnVer      = new Button("Ver");
            final Button btnEditar   = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnVer.getStyleClass().add("btn-table-view");
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                // Ver: abre el modal en modo solo lectura
                btnVer.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    abrirModal(p, ProductoModalController.MODO_VER);
                });

                // Editar: abre el modal en modo edición
                btnEditar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    abrirModal(p, ProductoModalController.MODO_EDITAR);
                });

                // Eliminar: verifica movimientos antes de proceder
                btnEliminar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    ResultadoEliminacion res = validacion.validarProducto(
                            p.getIdProducto(), p.getNombre()
                    );
                    if (!res.isPermitido()) {
                        res.mostrarAlerta();
                        return;
                    }
                    // Ejemplo de reemplazo de Alert por CSDialog: confirm()
                    // con foco inicial en "Cancelar" (focoEnCancelar=true)
                    // porque eliminar es una acción destructiva -- así un
                    // Enter reflejo no la dispara por accidente.
                    boolean confirmado = CSDialog.confirm(
                            "Eliminar producto",
                            "¿Eliminar " + p.getNombre() + "?",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        if (dao.eliminar(p.getIdProducto())) {
                            cargarTabla();
                            cargarCards();
                        } else {
                            CSDialog.error("Error", "No se pudo eliminar el producto");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                // Deshabilita Eliminar si el producto tiene movimientos
                Producto p = getTableView().getItems().get(getIndex());
                btnEliminar.setDisable(false);
                btnEliminar.setOpacity(1.0);

                HBox hbox = new HBox(5, btnVer, btnEditar, btnEliminar);
                hbox.setAlignment(Pos.CENTER);
                setGraphic(hbox);
            }
        });
    }

    /**
     * Abre el modal de producto en modo VER o EDITAR.
     * Después de cerrar recarga la tabla y las cards.
     */
    private void abrirModal(Producto p, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ProductoModal.fxml")
            );
            Parent root = loader.load();

            ProductoModalController ctrl = loader.getController();
            ctrl.setModo(modo, p);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(modo.equals(ProductoModalController.MODO_VER)
                    ? "Detalle del Producto"
                    : "Editar Producto");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            // Alto fijo: sin esto, el Stage crece al alto preferido de
            // TODO el contenido (incluida la tabla de lotes), ocupando
            // toda la pantalla en laptops. Con alto fijo, el ScrollPane
            // interno del modal se encarga de scrollear el exceso.
            modal.setHeight(640);
            modal.showAndWait();

            // Recarga tabla y cards para reflejar cambios
            cargarTabla();
            cargarCards();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir la ventana: " + ex.getMessage());
        }
    }

    @FXML
    private void guardar() {
        if (!PermisoService.puedeEditarProducto()) {
            PermisoUtil.denegado();
            return;
        }

        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cbEstado.getValue();

        // Validación 1: campos obligatorios
        // precio_compra ya no se pide, se define con la primera compra
        if (id.isEmpty() || nombre.isEmpty() ||
                txtPrecioVenta.getText().isEmpty() || estado == null) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos obligatorios.");
            return;
        }

        try {
            // precio_compra inicia en 0, se definirá con la
            // primera compra real registrada en el sistema
            double precioCompra = 0;
            // .replace(",", ".") porque el teclado numérico de algunos
            // usuarios escribe coma decimal (ej. "150,50"); antes solo
            // el modal de edición (ProductoModalController) tenía este
            // reemplazo -- aquí, en el alta, un precio con coma tiraba
            // NumberFormatException y el producto no se podía crear.
            double precioVenta  = Double.parseDouble(
                    txtPrecioVenta.getText().trim().replace(",", "."));

            // Validación: precio de venta no negativo
            // No se compara contra precio_compra porque este es 0 al crear
            if (precioVenta < 0) {
                CSDialog.warning("Precio inválido", "El precio de venta no puede ser negativo.");
                return;
            }

            // Stock mínimo y máximo: valores de referencia para alertas
            // de reposición (Compra) y sobre-stock (Kardex más adelante)
            int stockMinimo;
            int stockMaximo;
            try {
                stockMinimo = Integer.parseInt(txtStockMinimo.getText().trim());
                stockMaximo = Integer.parseInt(txtStockMaximo.getText().trim());
            } catch (NumberFormatException ex) {
                CSDialog.warning("Datos inválidos",
                        "Stock mínimo y máximo deben ser números válidos.\nEjemplo: 5 y 50");
                return;
            }

            if (stockMinimo < 0 || stockMaximo < 0) {
                CSDialog.warning("Datos inválidos", "El stock mínimo y máximo no pueden ser negativos.");
                return;
            }

            if (stockMaximo > 0 && stockMinimo > stockMaximo) {
                CSDialog.warning("Datos inválidos", "El stock mínimo no puede ser mayor al máximo.");
                return;
            }

            // Verifica que el ID no exista antes de insertar
            if (dao.buscarPorId(id) != null) {
                CSDialog.warning("ID duplicado",
                        "Ya existe un producto con ese ID.\nUsa Limpiar para generar un nuevo ID.");
                return;
            }

            // Verifica que el nombre no esté repetido (ver comentario
            // en ProductoDAO.existeNombre): antes se podían crear dos
            // productos con el mismo nombre, generando ambigüedad al
            // buscarlos o seleccionarlos en Venta/Compra.
            if (dao.existeNombre(nombre, null)) {
                CSDialog.warning("Producto duplicado", "Ya existe un producto con ese nombre.");
                return;
            }

            // El stock SIEMPRE inicia en 0 al crear un producto.
            Producto p = new Producto(id, categoriaActual, nombre,
                    descripcion, precioCompra, precioVenta, 0, estado);
            p.setStockMinimo(stockMinimo);
            p.setStockMaximo(stockMaximo);

            boolean exito = dao.insertar(p);
            if (exito) {
                // "Registrado correctamente" es un mensaje de ÉXITO, no
                // solo información neutra -- success() (verde) comunica
                // mejor el resultado que info() (azul), a diferencia del
                // Alert.AlertType.INFORMATION original que no distinguía
                // entre ambos casos.
                CSDialog.success("Producto registrado", "El producto fue registrado correctamente.");
                cargarTabla();
                cargarCards();
                limpiar();
            } else {
                CSDialog.error("Error", "No se pudo registrar el producto.");
            }

        } catch (NumberFormatException e) {
            CSDialog.warning("Datos inválidos",
                    "Precio y stock deben ser números válidos.\nEjemplo precio: 150.50   Ejemplo stock: 10");
        }
    }

    @FXML
    private void limpiar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecioVenta.clear();
        txtStockMinimo.clear();
        txtStockMaximo.clear();
        cbEstado.setValue(null);
        if (categoriaActual != null && !soloLectura) generarId();
    }

    private void generarId() {
        if (categoriaActual == null) return;

        String nombre  = categoriaActual.getNombre().trim().toUpperCase();
        String prefijo = nombre.length() >= 3
                ? nombre.substring(0, 3).replaceAll("[^A-Z]", "")
                : nombre.replaceAll("[^A-Z]", "");

        // Si el prefijo quedó vacío (nombre con caracteres raros) usa PROD
        if (prefijo.isEmpty()) prefijo = "PRO";

        String ultimo = dao.obtenerUltimoId(categoriaActual.getIdCategoria());

        if (ultimo == null) {
            txtId.setText(String.format("%s001", prefijo));
        } else {
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero = numeroStr.isEmpty()
                    ? 1
                    : Integer.parseInt(numeroStr) + 1;
            txtId.setText(String.format("%s%03d", prefijo, numero));
        }
        txtId.setDisable(true);
    }
}