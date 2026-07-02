package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.dao.ProveedorDAO;
import pe.utp.model.Proveedor;
import javafx.scene.control.Label;

public class ProveedorModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    @FXML private VBox panelCabecera;
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
        panelCabecera.setStyle(
                "-fx-background-color: #4361ee; -fx-padding: 24 20 18 20;"
        );
        lblTituloCabecera.setText("DETALLE DE PROVEEDOR");
        txtNombre.setEditable(false);
        txtRuc.setEditable(false);
        txtTelefono.setEditable(false);
        txtDireccion.setEditable(false);
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);
        btnCancelar.setText("✖ Cerrar");
    }

    private void configurarModoEditar() {
        panelCabecera.setStyle(
                "-fx-background-color: #2dc653; -fx-padding: 24 20 18 20;"
        );
        lblTituloCabecera.setText("EDITAR PROVEEDOR");
        txtNombre.setEditable(true);
        txtRuc.setEditable(true);
        txtTelefono.setEditable(true);
        txtDireccion.setEditable(true);
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);
        btnCancelar.setText("✖ Cancelar");
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String ruc = txtRuc.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (nombre.isEmpty() || ruc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "El nombre y el RUC son obligatorios").showAndWait();
            return;
        }

        // Validación 2: nombre acepta razones sociales
        if (!nombre.matches("[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\.\\,\\-\\_&]+")) {
            new Alert(Alert.AlertType.WARNING,
                    "El nombre contiene caracteres no permitidos").showAndWait();
            return;
        }

        // Validación 3: RUC exactamente 11 dígitos
        if (!ruc.matches("\\d{11}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El RUC debe tener exactamente 11 dígitos numéricos")
                    .showAndWait();
            return;
        }

        // Validación 4: teléfono 9 dígitos si se ingresa
        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El teléfono debe tener exactamente 9 dígitos").showAndWait();
            return;
        }

        // Validación 5: RUC duplicado excluyendo al propio proveedor
        if (dao.existeRuc(ruc, proveedor.getIdProveedor())) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe otro proveedor con ese RUC").showAndWait();
            return;
        }

        proveedor.setNombre(nombre);
        proveedor.setRuc(ruc);
        proveedor.setTelefono(telefono);
        proveedor.setDireccion(direccion);

        boolean exito = dao.actualizar(proveedor);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Proveedor actualizado correctamente").showAndWait();
            cerrarModal();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo actualizar el proveedor").showAndWait();
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