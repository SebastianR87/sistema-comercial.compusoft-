package pe.utp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Categoria;

/** Modal único para Crear, Ver y Editar una Categoría */
public class CategoriaModalController {

    public static final String MODO_CREAR  = "CREAR";
    public static final String MODO_EDITAR = "EDITAR";

    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;

    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final CategoriaDAO dao = new CategoriaDAO();
    private Categoria categoria;
    private String modo;

    public void setModo(String modo, Categoria c) {
        this.modo = modo;

        if (modo.equals(MODO_CREAR)) {
            this.categoria = new Categoria();
            this.categoria.setIdCategoria(generarId());
            configurarModoCrear();
        } else {
            this.categoria = c;
            configurarModoEditar();
        }
        cargarDatos(this.categoria);
    }

    private void cargarDatos(Categoria c) {
        lblNombreCabecera.setText(
                c.getNombre() != null && !c.getNombre().isEmpty()
                        ? c.getNombre() : "Nueva categoría");
        txtId.setText(c.getIdCategoria());
        txtNombre.setText(c.getNombre());
    }

    private void configurarModoCrear() {
        lblTituloCabecera.setText("NUEVA CATEGORÍA");
        btnGuardar.setText("Guardar");
    }

    private void configurarModoEditar() {
        lblTituloCabecera.setText("EDITAR CATEGORÍA");
        btnGuardar.setText("Guardar cambios");
    }

    /** Mismo cálculo de correlativo que antes vivía en CategoriaController.generarId(). */
    private String generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            return "CAT001";
        }
        String prefijo   = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        int numero        = Integer.parseInt(numeroStr) + 1;
        return String.format("%s%03d", prefijo, numero);
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();

        // Validación 1: campo obligatorio
        if (nombre.isEmpty()) {
            CSDialog.warning("Campos incompletos", "El nombre de la categoría es obligatorio.");
            return;
        }

        // Validación 2: solo letras, números, espacios y puntuación básica
        // (igual que Proveedor, para permitir nombres como "Tarjetas de Video (RTX)")
        if (!nombre.matches("[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\.\\,\\-\\_&()]+")) {
            CSDialog.warning("Nombre inválido", "El nombre contiene caracteres no permitidos.");
            return;
        }

        // Validación 3: nombre duplicado (distingue si es alta o edición,
        // igual que la validación que ya existía en CategoriaController)
        boolean duplicado = modo.equals(MODO_CREAR)
                ? dao.existeNombre(nombre)
                : dao.existeNombreExceptoId(nombre, categoria.getIdCategoria());
        if (duplicado) {
            CSDialog.warning("Categoría duplicada", "Ya existe una categoría con ese nombre.");
            return;
        }

        categoria.setNombre(nombre);

        boolean exito = modo.equals(MODO_CREAR)
                ? dao.insertar(categoria)
                : dao.actualizar(categoria);

        if (exito) {
            String titulo  = modo.equals(MODO_CREAR) ? "Categoría creada" : "Categoría actualizada";
            String mensaje = modo.equals(MODO_CREAR)
                    ? "La categoría fue creada correctamente."
                    : "La categoría fue actualizada correctamente.";
            // Cierra el modal y difiere el mensaje con Platform.runLater
            // (mismo motivo que en los demás *ModalController: cerrar este
            // modal y abrir el diálogo en el mismo pulso hacía que el
            // diálogo quedara "abierto" pero sin pintarse).
            cerrarModal();
            Platform.runLater(() -> CSDialog.success(titulo, mensaje));
        } else {
            CSDialog.error("Error", "No se pudo guardar la categoría.");
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
