package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.ProveedorDAO;
import pe.utp.model.Proveedor;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

public class ProveedorController implements AccesoControlable {

    @FXML private TableView<Proveedor> tablaProveedor;
    @FXML private TableColumn<Proveedor, String> colId;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colRuc;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colDireccion;
    @FXML private TableColumn<Proveedor, Void> colAcciones;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRuc;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtBuscar;

    private ProveedorDAO dao = new ProveedorDAO();
    private Proveedor proveedorSeleccionado = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProveedor"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRuc.setCellValueFactory(new PropertyValueFactory<>("ruc"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        configurarColumnaAcciones();
        cargarTabla();
        generarId();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        // Almacenero y administrador tienen acceso completo a proveedores.
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("PROV001");
        } else {
            String prefijo = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            if (numeroStr.isEmpty()) {
                txtId.setText("PROV001");
            } else {
                int numero = Integer.parseInt(numeroStr) + 1;
                txtId.setText(String.format("%s%03d", prefijo, numero));
            }
        }
        txtId.setDisable(true);
    }

    private void cargarTabla() {
        ObservableList<Proveedor> lista =
                FXCollections.observableArrayList(dao.listar());
        tablaProveedor.setItems(lista);
    }

    @FXML
    private void buscar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            cargarTabla();
        } else {
            ObservableList<Proveedor> lista =
                    FXCollections.observableArrayList(dao.buscarPorNombre(texto));
            tablaProveedor.setItems(lista);
        }
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
                    Proveedor p = getTableView().getItems().get(getIndex());
                    proveedorSeleccionado = p;
                    txtId.setText(p.getIdProveedor());
                    txtId.setDisable(true);
                    txtNombre.setText(p.getNombre());
                    txtRuc.setText(p.getRuc());
                    txtTelefono.setText(p.getTelefono());
                    txtDireccion.setText(p.getDireccion());
                });

                btnEliminar.setOnAction(e -> {
                    Proveedor p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar proveedor " + p.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            dao.eliminar(p.getIdProveedor());
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
                    HBox hbox = new HBox(6, btnEditar, btnEliminar);
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void guardar() {
        if (!PermisoService.puedeEditarProveedor()) {
            PermisoUtil.denegado();
            return;
        }
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String ruc = txtRuc.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (id.isEmpty() || nombre.isEmpty() || ruc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa los campos obligatorios").show();
            return;
        }

        Proveedor p = new Proveedor(id, nombre, telefono, ruc, direccion);

        if (proveedorSeleccionado == null) {
            dao.insertar(p);
        } else {
            dao.actualizar(p);
        }

        cargarTabla();
        limpiar();
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        txtRuc.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        proveedorSeleccionado = null;
        generarId();
    }
}