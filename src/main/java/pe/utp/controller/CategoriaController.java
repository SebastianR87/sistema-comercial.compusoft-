package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.CategoriaDAO;
import pe.utp.model.Categoria;

public class CategoriaController {

    @FXML private TableView<Categoria> tablaCategoria;
    @FXML private TableColumn<Categoria, String> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, Void> colAcciones;
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;

    private CategoriaDAO dao = new CategoriaDAO();
    private Categoria categoriaSeleccionada = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCategoria"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        configurarColumnaAcciones();
        cargarTabla();
    }

    private void cargarTabla() {
        ObservableList<Categoria> lista =
                FXCollections.observableArrayList(dao.Listar());
        tablaCategoria.setItems(lista);
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
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar " + c.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            dao.eliminar(c.getIdCategoria());
                            cargarTabla();
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
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();

        if (id.isEmpty() || nombre.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
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
    }
}