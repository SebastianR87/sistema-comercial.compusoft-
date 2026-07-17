package pe.utp.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Empleado;
import pe.utp.model.TipoDocumento;
import pe.utp.security.Sesion;

public class EmpleadoModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera
    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private Label lblCargoCabecera;

    // Campos del formulario
    @FXML private TextField        txtId;
    @FXML private TextField        txtNombre;
    @FXML private ComboBox<String> cbCargo;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
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
    private boolean passwordVisible = false;
    private EmpleadoDAO dao = new EmpleadoDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private Empleado empleado;

    // Bloquea clic/selección en el campo Dirección en modo Ver,
    // sin bloquear el hover (necesario para el Tooltip)
    private final javafx.event.EventHandler<MouseEvent> bloquearClick = MouseEvent::consume;

    @FXML
    public void initialize() {
        cbCargo.setItems(FXCollections.observableArrayList(
                "Administrador", "Vendedor", "Almacenero"
        ));

        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );
        cbTipoDocumento.setOnAction(e -> actualizarPlaceholder());

        // Tooltip con la dirección completa al pasar el mouse
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
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
        if (emp.getIdTipoDocumento() != null) {
            cbTipoDocumento.getItems().stream()
                    .filter(td -> td.getIdTipoDocumento()
                            .equals(emp.getIdTipoDocumento()))
                    .findFirst()
                    .ifPresent(td -> cbTipoDocumento.setValue(td));
        }
        txtNumeroDocumento.setText(emp.getNumeroDocumento() != null
                ? emp.getNumeroDocumento() : "");
        actualizarPlaceholder();
        txtTelefono.setText(emp.getTelefono()  != null ? emp.getTelefono()  : "");
        txtDireccion.setText(emp.getDireccion() != null ? emp.getDireccion() : "");
        txtUsuario.setText(emp.getUsuario());
        // Antes se precargaba la contraseña REAL en texto plano
        // (emp.getPassword()) directo en el campo, visible con solo
        // presionar "Mostrar" (togglePassword()). Ahora el campo
        // queda vacío: dejarlo así significa "no cambiar la
        // contraseña" (ver guardar()); si el usuario quiere una
        // nueva, la escribe desde cero.
        txtPassword.setText("");
        txtPassword.setPromptText("Dejar en blanco para no cambiar");
    }

    private void configurarModoVer() {
        lblTituloCabecera.setText("DETALLE DE EMPLEADO");

        txtNombre.setDisable(true);
        cbCargo.setDisable(true);
        cbTipoDocumento.setDisable(true);
        txtNumeroDocumento.setDisable(true);
        txtTelefono.setDisable(true);
        txtUsuario.setDisable(true);

        txtDireccion.setEditable(false);
        txtDireccion.setFocusTraversable(false);
        txtDireccion.addEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().add("campo-readonly");

        panelPassword.setVisible(false);
        panelPassword.setManaged(false);

        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        // En modo Ver, el único botón visible (Cerrar) toma el
        // color de marca, ya que es la acción principal disponible
        btnCancelar.setText("Cerrar");
        btnCancelar.getStyleClass().setAll("btn-primary");
        btnCancelar.setStyle("");
    }

    private void configurarModoEditar() {
        lblTituloCabecera.setText("EDITAR EMPLEADO");

        txtNombre.setDisable(false);
        cbCargo.setDisable(false);
        cbTipoDocumento.setDisable(false);
        txtNumeroDocumento.setDisable(false);
        txtTelefono.setDisable(false);
        txtUsuario.setDisable(false);

        txtDireccion.setEditable(true);
        txtDireccion.setFocusTraversable(true);
        txtDireccion.removeEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().remove("campo-readonly");

        panelPassword.setVisible(true);
        panelPassword.setManaged(true);
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);

        // En modo Editar, Cancelar vuelve a su estilo neutro
        // (blanco con borde), y Guardar es el que lleva el color
        btnCancelar.setText("Cancelar");
        btnCancelar.getStyleClass().remove("btn-primary");
        btnCancelar.setStyle(
                "-fx-background-color: white; -fx-text-fill: #334155;" +
                        "-fx-border-color: #CBD5E1; -fx-border-radius: 8;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-padding: 9 18; -fx-font-size: 13px;");
    }

    @FXML
    private void actualizarPlaceholder() {
        TipoDocumento tipo = cbTipoDocumento.getValue();
        if (tipo == null) return;
        switch (tipo.getDocumento()) {
            case "DNI":
                txtNumeroDocumento.setPromptText("8 dígitos numéricos");
                break;
            case "RUC":
                txtNumeroDocumento.setPromptText("11 dígitos numéricos");
                break;
            case "Carnet de Extranjería":
                txtNumeroDocumento.setPromptText("9 dígitos numéricos");
                break;
            case "Pasaporte":
                txtNumeroDocumento.setPromptText("6 a 12 caracteres alfanuméricos");
                break;
            default:
                txtNumeroDocumento.setPromptText("Número de documento");
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
        // Antes editar/guardar no comprobaba ningún permiso en código
        // (solo la creación en EmpleadoController.guardar() lo hacía);
        // se agrega la misma guarda aquí como segunda capa de defensa,
        // igual que en btnEditar/btnEstado/btnEliminar.
        if (!pe.utp.security.PermisoService.puedeAcceder(pe.utp.security.Modulo.EMPLEADOS)) {
            pe.utp.security.PermisoUtil.denegado();
            return;
        }

        String nombre = txtNombre.getText().trim();
        String cargo = cbCargo.getValue();
        TipoDocumento tipoDoc = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String usuario = txtUsuario.getText().trim();
        // Campo vacío = "no cambiar la contraseña" (ver cargarDatos()
        // y el bloque de guardado más abajo, que solo llama a
        // empleado.setPassword(...) si esto NO está vacío)
        String password  = passwordVisible
                ? txtPasswordVisible.getText().trim()
                : txtPassword.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios (password ya NO es
        // obligatorio aquí -- vacío es válido y significa "mantener
        // la actual")
        if (nombre.isEmpty() || cargo == null ||
                tipoDoc == null || numeroDocumento.isEmpty() ||
                usuario.isEmpty()) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos obligatorios.");
            return;
        }

        // Validación 2: nombre solo letras
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            CSDialog.warning("Nombre inválido", "El nombre solo debe contener letras.");
            return;
        }

        if (usuario.contains(" ")) {
            CSDialog.warning("Usuario inválido", "El usuario no debe contener espacios.");
            return;
        }

        // Solo letras, números y guiones bajos
        if (!usuario.matches("\\w{4,20}")) {
            CSDialog.warning("Usuario inválido",
                    "El usuario debe tener entre 4 y 20 caracteres y solo letras, números o guión bajo.");
            return;
        }

        // Rango 7-15 dígitos con "+" opcional, igual que EmpleadoController:
        // no asumir solo números peruanos, hay formatos internacionales
        // más cortos que también son válidos.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
            return;
        }

        switch (tipoDoc.getDocumento()) {
            case "DNI":
                if (!numeroDocumento.matches("\\d{8}")) {
                    CSDialog.warning("Documento inválido", "El DNI debe tener exactamente 8 dígitos numéricos.");
                    return;
                }
                break;
            case "RUC":
                if (!numeroDocumento.matches("\\d{11}")) {
                    CSDialog.warning("Documento inválido", "El RUC debe tener exactamente 11 dígitos numéricos.");
                    return;
                }
                break;
            case "Carnet de Extranjería":
                if (!numeroDocumento.matches("\\d{9}")) {
                    CSDialog.warning("Documento inválido", "El Carnet de Extranjería debe tener exactamente 9 dígitos.");
                    return;
                }
                break;
            case "Pasaporte":
                if (!numeroDocumento.matches("[a-zA-Z0-9]{6,12}")) {
                    CSDialog.warning("Documento inválido",
                            "El Pasaporte debe tener entre 6 y 12 caracteres alfanuméricos.");
                    return;
                }
                break;
        }

        // Validación 4: número de documento duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, empleado.getIdEmpleado())) {
            CSDialog.warning("Documento duplicado", "Ya existe otro empleado con ese número de documento.");
            return;
        }

        // Validación 5 (nueva): no permitir que un Administrador activo
        // deje de serlo si es el único que queda -- mismo resguardo que
        // Desactivar/Eliminar en EmpleadoController, pero aplicado aquí
        // porque editar el cargo tiene el mismo efecto práctico que
        // desactivar a un administrador (pierde el permiso de admin).
        boolean eraAdminActivo = "Administrador".equals(empleado.getCargo())
                && "ACTIVO".equals(empleado.getEstado());
        boolean dejaDeSerAdmin = eraAdminActivo && !"Administrador".equals(cargo);
        if (dejaDeSerAdmin) {
            boolean esUnoMismo = Sesion.getEmpleado() != null
                    && Sesion.getEmpleado().getIdEmpleado().equals(empleado.getIdEmpleado());
            if (esUnoMismo) {
                CSDialog.warning("No se puede cambiar el rol",
                        "No puedes quitarte a ti mismo el rol de Administrador " +
                                "mientras tienes la sesión abierta. Pide a otro administrador que lo haga.");
                return;
            }
            if (dao.contarAdministradoresActivos() <= 1) {
                CSDialog.warning("No se puede cambiar el rol",
                        "\"" + empleado.getNombre() + "\" es el único administrador activo. " +
                                "Registra o activa otro administrador antes de cambiarle el rol.");
                return;
            }
        }

        // Actualiza el objeto con los nuevos valores
        empleado.setNombre(nombre);
        empleado.setCargo(cargo);
        empleado.setIdTipoDocumento(tipoDoc.getIdTipoDocumento());
        empleado.setNumeroDocumento(numeroDocumento);
        empleado.setUsuario(usuario);
        // Solo se sobrescribe la contraseña si el usuario escribió
        // una nueva; si dejó el campo en blanco, se conserva la que
        // ya tenía el empleado (empleado.getPassword() no se toca)
        if (!password.isEmpty()) {
            empleado.setPassword(password);
        }
        empleado.setTelefono(telefono);
        empleado.setDireccion(direccion);

        boolean exito = dao.actualizar(empleado);
        if (exito) {
            // Cierra el modal y difiere el mensaje con Platform.runLater
            // (ver comentario detallado en ClienteModalController.guardar()):
            // cerrar este modal y abrir el diálogo en el mismo pulso hacía
            // que el diálogo quedara "abierto" pero sin pintarse.
            cerrarModal();
            Platform.runLater(() ->
                    CSDialog.success("Empleado actualizado", "El empleado fue actualizado correctamente."));
        } else {
            CSDialog.error("Error", "No se pudo actualizar el empleado.");
        }
    }

    @FXML
    private void cancelar() {
        cerrarModal();
    }

    private void cerrarModal() {
        Stage stage = (Stage) txtId.getScene().getWindow();
        stage.close();
    }
}
