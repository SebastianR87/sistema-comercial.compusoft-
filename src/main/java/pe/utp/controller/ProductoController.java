package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dao.EspecificacionDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.model.Categoria;
import pe.utp.model.Especificacion;
import pe.utp.model.Producto;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductoController implements AccesoControlable {

    @FXML private Label lblModoConsulta;
    @FXML private ScrollPane panelFormulario;
    @FXML private FlowPane panelCards;
    @FXML private HBox panelContenido;
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

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStock;
    @FXML private ComboBox<String> cbEstado;
    @FXML private VBox panelEspecificaciones;

    private ProductoDAO dao = new ProductoDAO();
    private CategoriaDAO categoriaDAO = new CategoriaDAO();
    private EspecificacionDAO especificacionDAO = new EspecificacionDAO();
    private Producto productoSeleccionado = null;
    private Categoria categoriaActual = null;
    private Map<String, TextField> camposEspecificacion = new HashMap<>();
    private boolean soloLectura = false;

    private String obtenerIcono(String nombreCategoria) {
        return switch (nombreCategoria.toUpperCase()) {
            case "PROCESADOR" -> "⚙️";
            case "PLACA MADRE" -> "📋";
            case "MEMORIA RAM", "RAM" -> "💾";
            case "TARJETA DE VIDEO", "GPU" -> "🎮";
            case "FUENTE DE PODER", "FUENTE" -> "⚡";
            case "ALMACENAMIENTO", "DISCO M,2", "DISCO SSD" -> "💿";
            case "DISIPADOR" -> "💨";
            default -> "📦";
        };
    }

    private String obtenerColor(String nombreCategoria) {
        return switch (nombreCategoria.toUpperCase()) {
            case "PROCESADOR" -> "#4361ee";
            case "PLACA MADRE" -> "#3a0ca3";
            case "MEMORIA RAM", "RAM" -> "#7209b7";
            case "TARJETA DE VIDEO", "GPU" -> "#f72585";
            case "FUENTE DE PODER", "FUENTE" -> "#e94560";
            case "ALMACENAMIENTO", "DISCO M,2", "DISCO SSD" -> "#4cc9f0";
            case "DISIPADOR" -> "#4895ef";
            default -> "#6c757d";
        };
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colPrecioCompra.setCellValueFactory(new PropertyValueFactory<>("precioCompra"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Columna nombre con tooltip de specs
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Producto p = getTableView().getItems().get(getIndex());
                    List<Especificacion> specs = especificacionDAO
                            .listarPorProducto(p.getIdProducto());

                    if (!specs.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📋 Especificaciones:\n\n");
                        for (Especificacion esp : specs) {
                            sb.append("• ").append(esp.getClave())
                                    .append(": ").append(esp.getValor()).append("\n");
                        }
                        Tooltip tooltip = new Tooltip(sb.toString());
                        tooltip.setStyle(
                                "-fx-background-color: #1a1a2e;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 13px;" +
                                        "-fx-padding: 12;" +
                                        "-fx-background-radius: 8;"
                        );
                        tooltip.setShowDelay(javafx.util.Duration.millis(300));
                        setTooltip(tooltip);
                    }
                }
            }
        });
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        cbEstado.setItems(FXCollections.observableArrayList("Activo", "Inactivo"));
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
        int cantidad = dao.listarPorCategoria(cat.getIdCategoria()).size();

        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(140);
        card.setPrefHeight(120);
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);"
        );

        Label lblIcono = new Label(icono);
        lblIcono.setStyle("-fx-font-size: 28px;");

        Label lblNombre = new Label(cat.getNombre());
        lblNombre.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblNombre.setWrapText(true);
        lblNombre.setAlignment(Pos.CENTER);

        Label lblCantidad = new Label(cantidad + " productos");
        lblCantidad.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");

        card.getChildren().addAll(lblIcono, lblNombre, lblCantidad);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: derive(" + color + ", -20%);" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5);" +
                        "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);"
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
        cargarCamposEspecificacion(cat.getNombre());
        cargarTabla();
        panelContenido.setVisible(true);
        panelContenido.setManaged(true);
        if (!soloLectura) {
            generarId();
        }
    }

    @FXML
    private void volverCards() {
        panelContenido.setVisible(false);
        panelContenido.setManaged(false);
        limpiar();
        cargarCards();
    }

    private void cargarTabla() {
        ObservableList<Producto> lista = FXCollections.observableArrayList(
                dao.listarPorCategoria(categoriaActual.getIdCategoria())
        );
        tablaProducto.setItems(lista);
    }

    private void cargarCamposEspecificacion(String nombreCategoria) {
        panelEspecificaciones.getChildren().clear();
        camposEspecificacion.clear();

        String upper = nombreCategoria.toUpperCase();
        if (upper.equals("ALMACENAMIENTO") || upper.equals("GABINETE") ||
                upper.equals("DISCO M,2") || upper.equals("DISCO SSD")) return;

        Label titulo = new Label("Especificaciones Técnicas");
        titulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        panelEspecificaciones.getChildren().add(titulo);
        panelEspecificaciones.getChildren().add(new Separator());

        switch (upper) {
            case "PROCESADOR" -> {
                agregarCampoSpec("socket", "Socket (Ej: LGA1700, AM5)");
                agregarCampoSpec("nucleos", "Núcleos (Ej: 8)");
                agregarCampoSpec("frecuencia", "Frecuencia (Ej: 3.6GHz)");
            }
            case "PLACA MADRE" -> {
                agregarCampoSpec("socket", "Socket (Ej: LGA1700, AM5)");
                agregarCampoSpec("tipo_ram", "Tipo RAM (Ej: DDR4, DDR5)");
                agregarCampoSpec("slots_ram", "Slots RAM (Ej: 4)");
            }
            case "MEMORIA RAM", "RAM" -> {
                agregarCampoSpec("tipo_ram", "Tipo RAM (Ej: DDR4, DDR5)");
                agregarCampoSpec("capacidad", "Capacidad (Ej: 16GB)");
                agregarCampoSpec("velocidad", "Velocidad (Ej: 3200MHz)");
            }
            case "TARJETA DE VIDEO", "GPU" -> {
                agregarCampoSpec("watts", "Consumo Watts (Ej: 200)");
                agregarCampoSpec("vram", "VRAM (Ej: 8GB)");
                agregarCampoSpec("conector", "Conector (Ej: PCIe 4.0)");
            }
            case "FUENTE DE PODER", "FUENTE" -> {
                agregarCampoSpec("watts", "Watts (Ej: 650)");
                agregarCampoSpec("certificacion", "Certificación (Ej: 80+ Gold)");
            }
            case "DISIPADOR" -> {
                agregarCampoSpec("socket", "Socket compatible (Ej: LGA1700)");
                agregarCampoSpec("tipo", "Tipo (Ej: Aire, Liquido)");
            }
        }
    }

    private void agregarCampoSpec(String clave, String placeholder) {
        VBox contenedor = new VBox(4);
        Label label = new Label(placeholder.split(" \\(")[0]);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-font-weight: bold;");
        TextField campo = new TextField();
        campo.setPromptText(placeholder);
        contenedor.getChildren().addAll(label, campo);
        panelEspecificaciones.getChildren().add(contenedor);
        camposEspecificacion.put(clave, campo);
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
                btnEliminar.setStyle(
                        "-fx-background-color: #ef233c; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");

                btnEditar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    productoSeleccionado = p;
                    txtId.setText(p.getIdProducto());
                    txtId.setDisable(true);
                    txtNombre.setText(p.getNombre());
                    txtDescripcion.setText(p.getDescripcion());
                    txtPrecioCompra.setText(String.valueOf(p.getPrecioCompra()));
                    txtPrecioVenta.setText(String.valueOf(p.getPrecioVenta()));
                    txtStock.setText(String.valueOf(p.getStock()));
                    cbEstado.setValue(p.getEstado());
                    especificacionDAO.listarPorProducto(p.getIdProducto())
                            .forEach(esp -> {
                                TextField campo = camposEspecificacion.get(esp.getClave());
                                if (campo != null) campo.setText(esp.getValor());
                            });
                });

                btnEliminar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar " + p.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            especificacionDAO.eliminarPorProducto(p.getIdProducto());
                            dao.eliminar(p.getIdProducto());
                            cargarTabla();
                            cargarCards();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(6, btnEditar, btnEliminar);
                    setGraphic(hbox);
                }
            }
        });
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

        if (id.isEmpty() || nombre.isEmpty() ||
                txtPrecioCompra.getText().isEmpty() ||
                txtPrecioVenta.getText().isEmpty() ||
                txtStock.getText().isEmpty() || estado == null) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
        }

        try {
            double precioCompra = Double.parseDouble(txtPrecioCompra.getText().trim());
            double precioVenta = Double.parseDouble(txtPrecioVenta.getText().trim());
            int stock = Integer.parseInt(txtStock.getText().trim());

            Producto p = new Producto(id, categoriaActual, nombre, descripcion,
                    precioCompra, precioVenta, stock, estado);

            if (productoSeleccionado == null) {
                if (dao.insertar(p)) {
                    guardarEspecificaciones(id);
                }
            } else {
                dao.actualizar(p);
                especificacionDAO.eliminarPorProducto(id);
                guardarEspecificaciones(id);
            }

            cargarTabla();
            limpiar();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING,
                    "Precio y stock deben ser números").show();
        }
    }

    private void guardarEspecificaciones(String idProducto) {
        long timestamp = System.currentTimeMillis();
        int contador = 1;
        for (Map.Entry<String, TextField> entry : camposEspecificacion.entrySet()) {
            String valor = entry.getValue().getText().trim();
            if (!valor.isEmpty()) {
                Especificacion esp = new Especificacion();
                esp.setIdEspecificacion("ESP" + timestamp + contador);
                esp.setIdProducto(idProducto);
                esp.setClave(entry.getKey());
                esp.setValor(valor);
                especificacionDAO.insertar(esp);
                contador++;
            }
        }
    }

    @FXML
    private void limpiar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecioCompra.clear();
        txtPrecioVenta.clear();
        txtStock.clear();
        cbEstado.setValue(null);
        panelEspecificaciones.getChildren().clear();
        camposEspecificacion.clear();
        productoSeleccionado = null;
        if (categoriaActual != null) {
            cargarCamposEspecificacion(categoriaActual.getNombre());
        }
        generarId();
    }

    private void generarId() {
        if (categoriaActual == null) return;

        String ultimo = dao.obtenerUltimoId(categoriaActual.getIdCategoria());
        String prefijo = "PROD";

        if (ultimo == null) {
            txtId.setText(prefijo + "001");
        } else {
            // Extrae solo los números del final
            String numeroStr = ultimo.replaceAll("[^0-9]", "");

            // Verifica que no esté vacío antes de parsear
            if (numeroStr.isEmpty()) {
                txtId.setText(prefijo + "001");
            } else {
                int numero = Integer.parseInt(numeroStr) + 1;
                txtId.setText(String.format("%s%03d", prefijo, numero));
            }
        }
        txtId.setDisable(true);
    }
}