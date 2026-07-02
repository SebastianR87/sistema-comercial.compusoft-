package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.model.Categoria;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

public class CategoriaController implements AccesoControlable {

    @FXML private Label lblModoConsulta;
    @FXML private VBox panelFormulario;
    @FXML private TableView<Categoria> tablaCategoria;
    @FXML private TableColumn<Categoria, String> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, Void> colAcciones;
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtBuscar;
    @FXML private Label lblContador;

    private CategoriaDAO dao = new CategoriaDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();
    private ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Categoria> listaFiltrada;
    private Categoria categoriaSeleccionada = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCategoria"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        configurarColumnaAcciones();
        cargarTabla();
        generarId();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        if (!PermisoService.puedeEditarCategoria()) {
            lblModoConsulta.setVisible(true);
            lblModoConsulta.setManaged(true);
            PermisoUtil.ocultar(panelFormulario);
            PermisoUtil.ocultarColumna(colAcciones);
        }
    }

    private void cargarTabla() {
        listaCategorias.setAll(dao.Listar());
        listaFiltrada = new javafx.collections.transformation
                .FilteredList<>(listaCategorias, c -> true);
        tablaCategoria.setItems(listaFiltrada);
        actualizarContador();
    }

    private void actualizarContador() {
        int total = listaFiltrada != null
                ? (int) listaFiltrada.stream().count()
                : listaCategorias.size();
        lblContador.setText(total + " registros");
    }

    @FXML
    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        listaFiltrada.setPredicate(c ->
                texto.isEmpty() ||
                        c.getNombre().toLowerCase().contains(texto) ||
                        c.getIdCategoria().toLowerCase().contains(texto)
        );
        actualizarContador();
    }


    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand;" +
                                "-fx-font-size: 12px; -fx-min-width: 60px;");

                btnEliminar.setStyle(
                        "-fx-background-color: #ef233c; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand;" +
                                "-fx-font-size: 12px; -fx-min-width: 60px;");

                btnEditar.setOnAction(e -> {
                    Categoria c = getTableView().getItems().get(getIndex());
                    categoriaSeleccionada = c;
                    txtId.setText(c.getIdCategoria());
                    txtId.setDisable(true);
                    txtNombre.setText(c.getNombre());
                });

                btnEliminar.setOnAction(e -> {
                    Categoria c = getTableView().getItems().get(getIndex());
                    ResultadoEliminacion validacionElim = validacion.validarCategoria(
                            c.getIdCategoria(), c.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar " + c.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            if (dao.eliminar(c.getIdCategoria())) {
                                cargarTabla();
                            } else {
                                new Alert(Alert.AlertType.ERROR,
                                        "No se pudo eliminar la categoría").show();
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
                    HBox hbox = new HBox(8, btnEditar, btnEliminar);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void guardar() {

        if (!PermisoService.puedeEditarCategoria()) {
            PermisoUtil.denegado();
            return;
        }

        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();

        if (id.isEmpty() || nombre.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Completa todos los campos").show();
            return;
        }

        // VALIDAR CATEGORÍA REPETIDA
        if (categoriaSeleccionada == null) {

            if (dao.existeNombre(nombre)) {
                new Alert(Alert.AlertType.WARNING,
                        "La categoría ya existe.")
                        .showAndWait();
                return;
            }

        } else {

            if (dao.existeNombreExceptoId(nombre, id)) {
                new Alert(Alert.AlertType.WARNING,
                        "La categoría ya existe.")
                        .showAndWait();
                return;
            }
        }

        Categoria c = new Categoria(id, nombre);

        if (categoriaSeleccionada == null) {
            dao.insertar(c);
        } else {
            dao.actualizar(c);
        }

        cargarTabla();
        cancelar();
    }

    @FXML
    private void cancelar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        categoriaSeleccionada = null;
        generarId();
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();

        if (ultimo == null) {
            txtId.setText("CAT001");
        } else {
            String prefijo = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero = Integer.parseInt(numeroStr) + 1;
            String nuevoId = String.format("%s%03d", prefijo, numero);
            txtId.setText(nuevoId);
        }
        txtId.setDisable(true);
    }
}