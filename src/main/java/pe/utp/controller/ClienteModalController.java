package pe.utp.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import pe.utp.dao.ClienteDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;


public class ClienteModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera
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

    // Bloquea clic/selección en el campo Dirección en modo Ver,
    // sin bloquear el hover (necesario para el Tooltip)
    private final javafx.event.EventHandler<MouseEvent> bloquearClick = MouseEvent::consume;

    @FXML
    public void initialize() {
        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );

        // Cada vez que cambia el tipo actualiza el hint del número
        cbTipoDocumento.setOnAction(e -> actualizarPlaceholder());

        // Tooltip que muestra la dirección completa al pasar el mouse,
        // útil cuando el texto es más largo que el ancho del campo
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
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
        lblTituloCabecera.setText("DETALLE DE CLIENTE");

        txtNombre.setDisable(true);
        cbTipoDocumento.setDisable(true);
        txtNumeroDocumento.setDisable(true);
        txtTelefono.setDisable(true);
        txtCorreo.setDisable(true);

        // Dirección: setEditable (no setDisable) para que el mouse
        // siga activo y el Tooltip con el texto completo funcione.
        // Se bloquea el clic para que no se sienta "editable".
        txtDireccion.setEditable(false);
        txtDireccion.setFocusTraversable(false);
        txtDireccion.addEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().add("campo-readonly");

        // Oculta el botón Guardar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        // En modo Ver, el único botón visible (Cerrar) toma el
        // color de marca, ya que es la acción principal disponible
        btnCancelar.setText("Cerrar");
        btnCancelar.getStyleClass().setAll("btn-primary");
        btnCancelar.setStyle("");
    }


    private void configurarModoEditar() {
        lblTituloCabecera.setText("EDITAR CLIENTE");

        // Habilita todos los campos
        txtNombre.setDisable(false);
        cbTipoDocumento.setDisable(false);
        txtNumeroDocumento.setDisable(false);
        txtTelefono.setDisable(false);
        txtCorreo.setDisable(false);

        txtDireccion.setEditable(true);
        txtDireccion.setFocusTraversable(true);
        txtDireccion.removeEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().remove("campo-readonly");

        // Muestra el botón Guardar (coral, definido en el FXML)
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

        // Validación 1: campos obligatorios (correo NO va aquí, es opcional)
        if (nombre.isEmpty() || tipoDoc == null || numeroDocumento.isEmpty()) {
            CSDialog.warning("Campos incompletos",
                    "Completa los campos obligatorios: nombre, tipo y número de documento.");
            return;
        }

        // Validación 2: nombre solo letras y espacios
        if (!nombre.matches(
                "[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            CSDialog.warning("Nombre inválido", "El nombre solo debe contener letras.");
            return;
        }

        // Validación 3: formato del documento según tipo
        switch (tipoDoc.getDocumento()) {
            case "DNI":
                if (!numeroDocumento.matches("\\d{8}")) {
                    CSDialog.warning("Documento inválido",
                            "El DNI debe tener exactamente 8 dígitos numéricos.");
                    return;
                }
                break;
            case "RUC":
                if (!numeroDocumento.matches("\\d{11}")) {
                    CSDialog.warning("Documento inválido",
                            "El RUC debe tener exactamente 11 dígitos numéricos.");
                    return;
                }
                break;
            case "Carnet de Extranjería":
                // Exactamente 9 dígitos numéricos
                if (!numeroDocumento.matches("\\d{9}")) {
                    CSDialog.warning("Documento inválido",
                            "El Carnet de Extranjería debe tener exactamente 9 dígitos.");
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

        // Validación 4: teléfono opcional pero validado si se ingresa.
        // Rango 7-15 dígitos con "+" opcional, igual que ClienteController:
        // no asumir solo números peruanos, hay formatos internacionales
        // más cortos que también son válidos.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
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
            CSDialog.warning("Correo inválido",
                    "El correo no tiene un formato válido (ej: nombre@dominio.com).");
            return;
        }

        if (!correo.isEmpty() && dao.existeCorreo(correo, cliente.getIdCliente())) {
            CSDialog.warning("Correo duplicado",
                    "Ya existe otro cliente con ese correo electrónico.");
            return;
        }

        // Validación 6: número de documento duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, cliente.getIdCliente())) {
            CSDialog.warning("Documento duplicado",
                    "Ya existe otro cliente con ese número de documento.");
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
            // Cierra el modal y difiere el mensaje con Platform.runLater:
            // así el mensaje aparece sobre la ventana principal (ya
            // enfocada) en vez de apilarse encima del modal. Mostrar el
            // nuevo diálogo (su propio showAndWait) en el MISMO pulso en
            // el que se cierra este modal (también con showAndWait) hace
            // que JavaFX intente anidar un bucle nuevo mientras el
            // anterior todavía está saliendo -- el diálogo queda "abierto"
            // pero nunca se pinta. Con runLater, el mensaje se abre recién
            // en el siguiente pulso, cuando el modal ya terminó de cerrar.
            cerrarModal();
            Platform.runLater(() ->
                    CSDialog.success("Cliente actualizado", "El cliente fue actualizado correctamente."));
        } else {
            CSDialog.error("Error", "No se pudo actualizar el cliente.");
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