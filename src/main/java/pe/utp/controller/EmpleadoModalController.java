package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.model.Empleado;

public class EmpleadoModalController {

    // Constantes de modo para evitar errores de tipeo al comparar
    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera dinámica
    @FXML private VBox  panelCabecera;
    @FXML private Label lblIconoCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private Label lblCargoCabecera;

    // Campos del formulario
    @FXML private TextField        txtId;
    @FXML private TextField        txtNombre;
    @FXML private ComboBox<String> cbCargo;
    @FXML private ComboBox<String> cbTipoDocumento;
    @FXML private TextField        txtNumeroDocumento;
    @FXML private TextField        txtTelefono;
    @FXML private TextField        txtDireccion;
    @FXML private TextField        txtUsuario;
    @FXML private PasswordField    txtPassword;
    @FXML private TextField        txtPasswordVisible;
    @FXML private Button           btnMostrar;

    // Paneles y botones controlables según el modo
    @FXML private VBox   panelPassword;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    // Estado interno
    private boolean     passwordVisible = false;
    private EmpleadoDAO dao             = new EmpleadoDAO();
    private Empleado    empleado;

    @FXML
    public void initialize() {
        cbCargo.setItems(FXCollections.observableArrayList(
                "Administrador", "Vendedor", "Almacenero"
        ));

        cbTipoDocumento.setItems(FXCollections.observableArrayList(
                "DNI", "Carnet de Extranjería", "Pasaporte"
        ));
    }

    public void setModo(String modo, Empleado emp) {
        this.empleado = emp;
        cargarDatos(emp);

        if (modo.equals(MODO_VER)) {
            configurarModoVer();
        } else {
            configurarModoEditar();
        }
    }

    private void cargarDatos(Empleado emp) {
        lblNombreCabecera.setText(emp.getNombre());
        lblCargoCabecera.setText(emp.getCargo());
        txtId.setText(emp.getIdEmpleado());
        txtNombre.setText(emp.getNombre());
        cbCargo.setValue(emp.getCargo());
        cbTipoDocumento.setValue(emp.getTipoDocumento());
        txtNumeroDocumento.setText(emp.getNumeroDocumento() != null
                ? emp.getNumeroDocumento() : "");
        actualizarPlaceholder();
        txtTelefono.setText(emp.getTelefono()  != null ? emp.getTelefono()  : "");
        txtDireccion.setText(emp.getDireccion() != null ? emp.getDireccion() : "");
        txtUsuario.setText(emp.getUsuario());
        txtPassword.setText(emp.getPassword());
    }

    private void configurarModoVer() {
        // Cabecera azul con ícono de ojo
        panelCabecera.setStyle(
                "-fx-background-color: #4361ee; -fx-padding: 24 20 18 20;"
        );
        lblIconoCabecera.setText("👁");

        // Deshabilita todos los campos para solo lectura
        txtNombre.setEditable(false);
        cbCargo.setDisable(true);
        cbTipoDocumento.setDisable(true);
        txtNumeroDocumento.setEditable(false);
        txtTelefono.setEditable(false);
        txtDireccion.setEditable(false);
        txtUsuario.setEditable(false);

        panelPassword.setVisible(false);
        panelPassword.setManaged(false);

        // Oculta el botón Guardar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        btnCancelar.setText("✖ Cerrar");
    }

    private void configurarModoEditar() {
        // Cabecera verde para distinguir visualmente que es edición
        panelCabecera.setStyle(
                "-fx-background-color: #2dc653; -fx-padding: 24 20 18 20;"
        );
        lblIconoCabecera.setText("✏️");

        // Habilita todos los campos para edición
        txtNombre.setEditable(true);
        cbCargo.setDisable(false);
        cbTipoDocumento.setDisable(false);
        txtNumeroDocumento.setEditable(true);
        txtTelefono.setEditable(true);
        txtDireccion.setEditable(true);
        txtUsuario.setEditable(true);

        // Muestra contraseña y botón guardar
        panelPassword.setVisible(true);
        panelPassword.setManaged(true);
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);

        btnCancelar.setText("✖ Cancelar");
    }

    @FXML
    private void actualizarPlaceholder() {
        String tipo = cbTipoDocumento.getValue();
        if (tipo == null) return;
        switch (tipo) {
            case "DNI":
                txtNumeroDocumento.setPromptText("8 dígitos numéricos");
                break;
            case "Carnet de Extranjería":
                txtNumeroDocumento.setPromptText("9 dígitos numéricos");
                break;
            case "Pasaporte":
                txtNumeroDocumento.setPromptText("6 a 12 caracteres alfanuméricos");
                break;
        }
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

    /** Valida y guarda los cambios del empleado en BD. */
    @FXML
    private void guardar() {
        String nombre    = txtNombre.getText().trim();
        String cargo     = cbCargo.getValue();
        String tipoDocumento   = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String usuario   = txtUsuario.getText().trim();
        String password  = passwordVisible
                ? txtPasswordVisible.getText().trim()
                : txtPassword.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (nombre.isEmpty() || cargo == null ||
                tipoDocumento == null || numeroDocumento.isEmpty() ||
                usuario.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Completa todos los campos obligatorios").showAndWait();
            return;
        }

        // Validación 2: nombre solo letras
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            new Alert(Alert.AlertType.WARNING,
                    "El nombre solo debe contener letras").showAndWait();
            return;
        }

        if (usuario.contains(" ")) {
            new Alert(Alert.AlertType.WARNING,
                    "El usuario no debe contener espacios").showAndWait();
            return;
        }

        // Solo letras, números y guiones bajos
        // \\w = [a-zA-Z0-9_], {4,20} = entre 4 y 20 caracteres
        if (!usuario.matches("\\w{4,20}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El usuario debe tener entre 4 y 20 caracteres " +
                            "y solo letras, números o guión bajo").showAndWait();
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El teléfono debe tener exactamente 9 dígitos").showAndWait();
            return;
        }

        // Validación 3: formato del documento según tipo
        switch (tipoDocumento) {
            case "DNI":
                if (!numeroDocumento.matches("\\d{8}")) {
                    new Alert(Alert.AlertType.WARNING,
                            "El DNI debe tener exactamente 8 dígitos numéricos")
                            .showAndWait();
                    return;
                }
                break;
            case "Carnet de Extranjería":
                if (!numeroDocumento.matches("\\d{9}")) {
                    new Alert(Alert.AlertType.WARNING,
                            "El Carnet de Extranjería debe tener exactamente 9 dígitos")
                            .showAndWait();
                    return;
                }
                break;
            case "Pasaporte":
                if (!numeroDocumento.matches("[a-zA-Z0-9]{6,12}")) {
                    new Alert(Alert.AlertType.WARNING,
                            "El Pasaporte debe tener entre 6 y 12 caracteres alfanuméricos")
                            .showAndWait();
                    return;
                }
                break;
        }

        // Validación 4: número de documento duplicado
        // Excluye el propio ID para que al editar no detecte su propio número como duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, empleado.getIdEmpleado())) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe otro empleado con ese número de documento").showAndWait();
            return;
        }

        // Actualiza el objeto con los nuevos valores
        empleado.setNombre(nombre);
        empleado.setCargo(cargo);
        empleado.setTipoDocumento(tipoDocumento);
        empleado.setNumeroDocumento(numeroDocumento);
        empleado.setUsuario(usuario);
        empleado.setPassword(password);
        empleado.setTelefono(telefono);
        empleado.setDireccion(direccion);

        boolean exito = dao.actualizar(empleado);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Empleado actualizado correctamente").showAndWait();
            cerrarModal();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo actualizar el empleado").showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        cerrarModal();
    }

    /**
     * Cierra la ventana modal obteniendo la referencia
     * a la ventana actual desde cualquier campo del formulario.
     */
    private void cerrarModal() {
        Stage stage = (Stage) txtId.getScene().getWindow();
        stage.close();
    }
}
