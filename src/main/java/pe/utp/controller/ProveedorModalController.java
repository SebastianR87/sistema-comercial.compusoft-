package pe.utp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import pe.utp.dao.ProveedorDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Proveedor;
import javafx.scene.control.Label;

public class ProveedorModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRuc;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;

    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private ProveedorDAO dao = new ProveedorDAO();
    private Proveedor proveedor;

    // Bloquea clic/selección en el campo Dirección en modo Ver,
    // sin bloquear el hover (necesario para el Tooltip)
    private final javafx.event.EventHandler<MouseEvent> bloquearClick = MouseEvent::consume;

    @FXML
    public void initialize() {
        // Tooltip con la dirección completa al pasar el mouse
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
    }

    public void setModo(String modo, Proveedor p) {
        this.proveedor = p;
        cargarDatos(p);
        if (modo.equals(MODO_VER)) {
            configurarModoVer();
        } else {
            configurarModoEditar();
        }
    }

    private void cargarDatos(Proveedor p) {
        lblNombreCabecera.setText(p.getNombre());
        txtId.setText(p.getIdProveedor());
        txtNombre.setText(p.getNombre());
        txtRuc.setText(p.getRuc());
        txtTelefono.setText(p.getTelefono()  != null ? p.getTelefono()  : "");
        txtDireccion.setText(p.getDireccion() != null ? p.getDireccion() : "");
    }

    private void configurarModoVer() {
        lblTituloCabecera.setText("DETALLE DE PROVEEDOR");

        txtNombre.setDisable(true);
        txtRuc.setDisable(true);
        txtTelefono.setDisable(true);

        txtDireccion.setEditable(false);
        txtDireccion.setFocusTraversable(false);
        txtDireccion.addEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().add("campo-readonly");

        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        // En modo Ver, el único botón visible (Cerrar) toma el
        // color de marca, ya que es la acción principal disponible
        btnCancelar.setText("Cerrar");
        btnCancelar.getStyleClass().setAll("btn-primary");
        btnCancelar.setStyle("");
    }

    private void configurarModoEditar() {
        lblTituloCabecera.setText("EDITAR PROVEEDOR");

        txtNombre.setDisable(false);
        txtRuc.setDisable(false);
        txtTelefono.setDisable(false);

        txtDireccion.setEditable(true);
        txtDireccion.setFocusTraversable(true);
        txtDireccion.removeEventFilter(MouseEvent.MOUSE_PRESSED, bloquearClick);
        txtDireccion.getStyleClass().remove("campo-readonly");

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
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String ruc = txtRuc.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (nombre.isEmpty() || ruc.isEmpty()) {
            CSDialog.warning("Campos incompletos", "El nombre y el RUC son obligatorios.");
            return;
        }

        // Validación 2: nombre acepta razones sociales
        if (!nombre.matches("[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\.\\,\\-\\_&]+")) {
            CSDialog.warning("Nombre inválido", "El nombre contiene caracteres no permitidos.");
            return;
        }

        // Validación 3: RUC exactamente 11 dígitos
        if (!ruc.matches("\\d{11}")) {
            CSDialog.warning("RUC inválido", "El RUC debe tener exactamente 11 dígitos numéricos.");
            return;
        }

        // Validación 4: teléfono opcional pero validado si se ingresa.
        // Rango 7-15 dígitos con "+" opcional, igual que ProveedorController:
        // no asumir solo números peruanos, hay formatos internacionales
        // más cortos que también son válidos.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
            return;
        }

        // Validación 5: RUC duplicado excluyendo al propio proveedor
        if (dao.existeRuc(ruc, proveedor.getIdProveedor())) {
            CSDialog.warning("RUC duplicado", "Ya existe otro proveedor con ese RUC.");
            return;
        }

        proveedor.setNombre(nombre);
        proveedor.setRuc(ruc);
        proveedor.setTelefono(telefono);
        proveedor.setDireccion(direccion);

        boolean exito = dao.actualizar(proveedor);
        if (exito) {
            // Cierra el modal y difiere el mensaje con Platform.runLater
            // (ver comentario detallado en ClienteModalController.guardar()):
            // cerrar este modal y abrir el diálogo en el mismo pulso hacía
            // que el diálogo quedara "abierto" pero sin pintarse.
            cerrarModal();
            Platform.runLater(() ->
                    CSDialog.success("Proveedor actualizado", "El proveedor fue actualizado correctamente."));
        } else {
            CSDialog.error("Error", "No se pudo actualizar el proveedor.");
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
