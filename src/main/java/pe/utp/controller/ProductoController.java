package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.model.Categoria;
import pe.utp.model.Producto;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

import java.util.List;

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

    private ProductoDAO dao = new ProductoDAO();
    private CategoriaDAO categoriaDAO = new CategoriaDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();
    private Producto productoSeleccionado = null;
    private Categoria categoriaActual = null;
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
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecioCompra.setCellValueFactory(new PropertyValueFactory<>("precioCompra"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

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
                });

                btnEliminar.setOnAction(e -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    ResultadoEliminacion validacionElim = validacion.validarProducto(
                            p.getIdProducto(), p.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar " + p.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            if (dao.eliminar(p.getIdProducto())) {
                                cargarTabla();
                                cargarCards();
                            } else {
                                new Alert(Alert.AlertType.ERROR,
                                        "No se pudo eliminar el producto").show();
                            }
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
                dao.insertar(p);
            } else {
                dao.actualizar(p);
            }

            cargarTabla();
            cargarCards();
            limpiar();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING,
                    "Precio y stock deben ser números").show();
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
        productoSeleccionado = null;
        if (categoriaActual != null && !soloLectura) {
            generarId();
        }
    }

    private void generarId() {
        if (categoriaActual == null) return;

        String ultimo = dao.obtenerUltimoId(categoriaActual.getIdCategoria());
        String prefijo = "PROD";

        if (ultimo == null) {
            txtId.setText(prefijo + "001");
        } else {
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
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
