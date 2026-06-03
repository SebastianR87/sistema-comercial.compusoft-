package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.model.Empleado;

public class EmpleadoController {

    @FXML private TableView<Empleado> tablaEmpleado;
    @FXML private TableColumn<Empleado, String> colId;
    @FXML private TableColumn<Empleado, String> colNombre;
    @FXML private TableColumn<Empleado, String> colCargo;
    @FXML private TableColumn<Empleado, String> colUsuario;
    @FXML private TableColumn<Empleado, String> colDni;
    @FXML private TableColumn<Empleado, Void> colAcciones;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cbCargo;
    @FXML private TextField txtDni;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnMostrar;
    private boolean passwordVisible = false;

    private EmpleadoDAO dao = new EmpleadoDAO();
    private Empleado empleadoSeleccionado = null;

    @FXML
    public void initialize() {
        // Conecta columnas con atributos del modelo
        colId.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));

        // Carga opciones del ComboBox
        cbCargo.setItems(FXCollections.observableArrayList(
                "Administrador", "Vendedor", "Almacenero"
        ));
        generarId();

        configurarColumnaAcciones();
        cargarTabla();
    }

    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            btnMostrar.setText("🙈");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            btnMostrar.setText("👁");
        }
    }

    private void cargarTabla() {
        ObservableList<Empleado> lista =
                FXCollections.observableArrayList(dao.listar());
        tablaEmpleado.setItems(lista);
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
                    Empleado emp = getTableView().getItems().get(getIndex());
                    empleadoSeleccionado = emp;
                    txtId.setText(emp.getIdEmpleado());
                    txtId.setDisable(true);
                    txtNombre.setText(emp.getNombre());
                    cbCargo.setValue(emp.getCargo());
                    txtDni.setText(emp.getDni());
                    txtUsuario.setText(emp.getUsuario());
                    txtPassword.setText(emp.getPassword());
                });

                btnEliminar.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar empleado " + emp.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            dao.eliminar(emp.getIdEmpleado());
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
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String cargo = cbCargo.getValue();
        String dni = txtDni.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String password = passwordVisible ?
                txtPasswordVisible.getText().trim() :
                txtPassword.getText().trim();

        if (id.isEmpty() || nombre.isEmpty() || cargo == null ||
                dni.isEmpty() || usuario.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
        }

        Empleado e = new Empleado(id, nombre, cargo, usuario, password, dni);

        if (empleadoSeleccionado == null) {
            dao.insertar(e);
        } else {
            dao.actualizar(e);
        }

        cargarTabla();
        limpiar();
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("EMP001");
        } else {
            String prefijo = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero = Integer.parseInt(numeroStr) + 1;
            txtId.setText(String.format("%s%03d", prefijo, numero));
        }
        txtId.setDisable(true);
    }

    @FXML
    private void limpiar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        cbCargo.setValue(null);
        txtDni.clear();
        txtUsuario.clear();
        txtPassword.clear();
        txtPasswordVisible.clear();
        passwordVisible = false;
        txtPassword.setVisible(true);
        txtPassword.setManaged(true);
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
        btnMostrar.setText("👁");
        empleadoSeleccionado = null;
        generarId();
    }


}