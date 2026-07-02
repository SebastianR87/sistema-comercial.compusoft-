package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.dao.ClienteDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;


public class ClienteModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera dinámica
    @FXML private VBox  panelCabecera;
    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private Label lblSubtituloCabecera;

    // Campos del formulario
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;

    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    // DAOs necesarios
    private ClienteDAO dao = new ClienteDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();

    private Cliente cliente;

    @FXML
    public void initialize() {
        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );

        // Cada vez que cambia el tipo actualiza el hint del número
        cbTipoDocumento.setOnAction(e -> actualizarPlaceholder());
    }

    public void setModo(String modo, Cliente c) {
        this.cliente = c;
        cargarDatos(c);

        if (modo.equals(MODO_VER)) {
            configurarModoVer();
        } else {
            configurarModoEditar();
        }
    }

    private void cargarDatos(Cliente c) {
        txtId.setText(c.getIdCliente());
        txtNombre.setText(c.getNombre());

        if (c.getTipoDocumento() != null) {
            cbTipoDocumento.getItems().stream()
                    .filter(td -> td.getIdTipoDocumento()
                            .equals(c.getTipoDocumento().getIdTipoDocumento()))
                    .findFirst()
                    .ifPresent(td -> cbTipoDocumento.setValue(td));
        }
        lblNombreCabecera.setText(c.getNombre());
        lblSubtituloCabecera.setText(
                c.getTipoDocumento() != null
                        ? c.getTipoDocumento().getDocumento()
                        : ""
        );

        txtNumeroDocumento.setText(
                c.getNumeroDocumento() != null ? c.getNumeroDocumento() : ""
        );
        txtTelefono.setText(
                c.getTelefono() != null ? c.getTelefono() : ""
        );
        txtCorreo.setText(
                c.getCorreo() != null ? c.getCorreo() : ""
        );
        txtDireccion.setText(
                c.getDireccion() != null ? c.getDireccion() : ""
        );

        // Actualiza el placeholder del número según el tipo cargado
        actualizarPlaceholder();
    }

    private void configurarModoVer() {
        panelCabecera.setStyle(
                "-fx-background-color: #4361ee; -fx-padding: 24 20 18 20;"
        );
        lblTituloCabecera.setText("DETALLE DE CLIENTE");

        txtNombre.setEditable(false);
        cbTipoDocumento.setDisable(true);
        txtNumeroDocumento.setEditable(false);
        txtTelefono.setEditable(false);
        txtCorreo.setEditable(false);
        txtDireccion.setEditable(false);

        // Oculta el botón Guardar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        btnCancelar.setText("✖ Cerrar");
    }


    private void configurarModoEditar() {
        panelCabecera.setStyle(
                "-fx-background-color: #2dc653; -fx-padding: 24 20 18 20;"
        );
        lblTituloCabecera.setText("EDITAR CLIENTE");

        // Habilita todos los campos
        txtNombre.setEditable(true);
        cbTipoDocumento.setDisable(false);
        txtNumeroDocumento.setEditable(true);
        txtTelefono.setEditable(true);
        txtCorreo.setEditable(true);
        txtDireccion.setEditable(true);

        // Muestra el botón Guardar
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);

        btnCancelar.setText("✖ Cancelar");
    }

    /** Validación de tipo de documento
     */
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

    /**
     * Valida los campos y guarda los cambios en BD.
     * Solo se ejecuta en modo EDITAR porque en modo VER
     * el botón Guardar está oculto.
     */
    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        TipoDocumento tipoDoc  = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (nombre.isEmpty() || tipoDoc == null || numeroDocumento.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Completa los campos obligatorios: nombre, tipo y número de documento")
                    .showAndWait();
            return;
        }

        // Validación 2: nombre solo letras y espacios
        if (!nombre.matches(
                "[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            new Alert(Alert.AlertType.WARNING,
                    "El nombre solo debe contener letras").showAndWait();
            return;
        }

        // Validación 3: formato del documento según tipo
        switch (tipoDoc.getDocumento()) {
            case "DNI":
                if (!numeroDocumento.matches("\\d{8}")) {
                    new Alert(Alert.AlertType.WARNING,
                            "El DNI debe tener exactamente 8 dígitos numéricos")
                            .showAndWait();
                    return;
                }
                break;
            case "RUC":
                if (!numeroDocumento.matches("\\d{11}")) {
                    new Alert(Alert.AlertType.WARNING,
                            "El RUC debe tener exactamente 11 dígitos numéricos")
                            .showAndWait();
                    return;
                }
                break;
            case "Carnet de Extranjería":
                // Exactamente 9 dígitos numéricos
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

        // Validación 4: teléfono solo números si no está vacío
        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El teléfono debe tener exactamente 9 dígitos").showAndWait();
            return;
        }

        // Validación 5: formato de correo si no está vacío
        // El correo es opcional pero si se ingresa debe tener formato válido
        // [\\w.+-]+ = letras, números, puntos, +, -
        // @ = arroba obligatoria
        // [\\w-]+ = dominio
        // \\.[a-zA-Z]{2,} = extensión de al menos 2 letras (.com, .pe, etc)
        if (!correo.isEmpty() &&
                !correo.matches("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El correo no tiene un formato válido (ej: nombre@dominio.com)")
                    .showAndWait();
            return;
        }

        if (!correo.isEmpty() && dao.existeCorreo(correo, cliente.getIdCliente())) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe otro cliente con ese correo electrónico")
                    .showAndWait();
            return;
        }

        // Validación 6: número de documento duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, cliente.getIdCliente())) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe otro cliente con ese número de documento")
                    .showAndWait();
            return;
        }

        // Actualiza el objeto con los nuevos valores
        cliente.setNombre(nombre);
        cliente.setTipoDocumento(tipoDoc);
        cliente.setNumeroDocumento(numeroDocumento);
        cliente.setTelefono(telefono);
        cliente.setCorreo(correo);
        cliente.setDireccion(direccion);

        boolean exito = dao.actualizar(cliente);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Cliente actualizado correctamente").showAndWait();
            cerrarModal();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo actualizar el cliente").showAndWait();
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