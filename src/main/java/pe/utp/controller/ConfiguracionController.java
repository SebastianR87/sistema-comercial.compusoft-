package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.MetodoPagoDAO;
import pe.utp.dao.TipoComprobanteDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.model.MetodoPago;
import pe.utp.model.TipoComprobante;
import pe.utp.model.TipoDocumento;
import pe.utp.security.Modulo;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

public class ConfiguracionController implements AccesoControlable {

    // ===== TIPO DOCUMENTO =====
    @FXML private TableView<TipoDocumento> tablaTipoDoc;
    @FXML private TableColumn<TipoDocumento, String> colIdTipoDoc;
    @FXML private TableColumn<TipoDocumento, String> colDocumento;
    @FXML private TableColumn<TipoDocumento, Void> colAccTipoDoc;
    @FXML private TextField txtIdTipoDoc;
    @FXML private TextField txtDocumento;

    // ===== TIPO COMPROBANTE =====
    @FXML private TableView<TipoComprobante> tablaTipoComp;
    @FXML private TableColumn<TipoComprobante, String> colIdTipoComp;
    @FXML private TableColumn<TipoComprobante, String> colNombreComp;
    @FXML private TableColumn<TipoComprobante, Void> colAccTipoComp;
    @FXML private TextField txtIdTipoComp;
    @FXML private TextField txtNombreComp;

    // ===== METODO PAGO =====
    @FXML private TableView<MetodoPago> tablaMetodoPago;
    @FXML private TableColumn<MetodoPago, String> colIdMetodoPago;
    @FXML private TableColumn<MetodoPago, String> colMetodoPago;
    @FXML private TableColumn<MetodoPago, Void> colAccMetodoPago;
    @FXML private TextField txtIdMetodoPago;
    @FXML private TextField txtMetodoPago;
    @FXML private Label lblTituloTipoDoc;
    @FXML private Label lblTituloTipoComp;
    @FXML private Label lblTituloMetodoPago;

    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private TipoComprobanteDAO tipoCompDAO = new TipoComprobanteDAO();
    private MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();

    private TipoDocumento tipoDocSeleccionado = null;
    private TipoComprobante tipoCompSeleccionado = null;
    private MetodoPago metodoPagoSeleccionado = null;

    @FXML
    public void initialize() {
        // Tipo Documento
        colIdTipoDoc.setCellValueFactory(new PropertyValueFactory<>("idTipoDocumento"));
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("documento"));
        configurarAcciones(colAccTipoDoc, "tipoDoc");
        cargarTipoDoc();
        generarIdTipoDoc();

        // Tipo Comprobante
        colIdTipoComp.setCellValueFactory(new PropertyValueFactory<>("idTipoComprobante"));
        colNombreComp.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        configurarAcciones(colAccTipoComp, "tipoComp");
        cargarTipoComp();
        generarIdTipoComp();

        // Metodo Pago
        colIdMetodoPago.setCellValueFactory(new PropertyValueFactory<>("idMetodoPago"));
        colMetodoPago.setCellValueFactory(new PropertyValueFactory<>("metodoDePago"));
        configurarAcciones(colAccMetodoPago, "metodoPago");
        cargarMetodoPago();
        generarIdMetodoPago();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        // Solo el administrador accede a este módulo desde el menú.
    }

    private void mostrarErrorEliminacion() {
        CSDialog.error("Error", "No se pudo eliminar el registro.");
    }

    private boolean verificarAcceso() {
        if (!PermisoService.puedeAcceder(Modulo.CONFIGURACION)) {
            PermisoUtil.denegado();
            return false;
        }
        return true;
    }

    // ===== GENERAR IDs =====
    private void generarIdTipoDoc() {
        String ultimo = tipoDocDAO.obtenerUltimoId();
        txtIdTipoDoc.setText(generarSiguienteId(ultimo, "TD"));
        txtIdTipoDoc.setDisable(true);
    }

    private void generarIdTipoComp() {
        String ultimo = tipoCompDAO.obtenerUltimoId();
        txtIdTipoComp.setText(generarSiguienteId(ultimo, "TC"));
        txtIdTipoComp.setDisable(true);
    }

    private void generarIdMetodoPago() {
        String ultimo = metodoPagoDAO.obtenerUltimoId();
        txtIdMetodoPago.setText(generarSiguienteId(ultimo, "MP"));
        txtIdMetodoPago.setDisable(true);
    }

    private String generarSiguienteId(String ultimo, String prefijoDefault) {
        if (ultimo == null) return prefijoDefault + "001";
        String prefijo = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        if (numeroStr.isEmpty()) return prefijoDefault + "001";
        int numero = Integer.parseInt(numeroStr) + 1;
        return String.format("%s%03d", prefijo, numero);
    }

    // ===== CARGAR TABLAS =====
    private void cargarTipoDoc() {
        tablaTipoDoc.setItems(FXCollections.observableArrayList(tipoDocDAO.listar()));
    }

    private void cargarTipoComp() {
        tablaTipoComp.setItems(FXCollections.observableArrayList(tipoCompDAO.listar()));
    }

    private void cargarMetodoPago() {
        tablaMetodoPago.setItems(FXCollections.observableArrayList(metodoPagoDAO.listar()));
    }

    // ===== GUARDAR =====
    @FXML
    private void guardarTipoDoc() {
        if (!verificarAcceso()) return;
        String id = txtIdTipoDoc.getText().trim();
        String documento = txtDocumento.getText().trim();
        // Validación 1: campos obligatorios
        if (id.isEmpty() || documento.isEmpty()) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos.");
            return;
        }

        // Validación 2: solo letras y espacios
        // Ej: "DNI", "Carnet de Extranjería"
        if (!documento.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            CSDialog.warning("Documento inválido", "El documento solo puede contener letras y espacios.");
            return;
        }

        // Validación 3: duplicado
        String idExcluir = tipoDocSeleccionado != null
                ? tipoDocSeleccionado.getIdTipoDocumento() : null;
        if (tipoDocDAO.existeDocumento(documento, idExcluir)) {
            CSDialog.warning("Documento duplicado", "Ya existe un tipo de documento con ese nombre.");
            return;
        }

        TipoDocumento td = new TipoDocumento(id, documento);
        boolean exito = (tipoDocSeleccionado == null)
                ? tipoDocDAO.insertar(td)
                : tipoDocDAO.actualizar(td);

        if (exito) {
            String msg = tipoDocSeleccionado == null
                    ? "Tipo de documento registrado correctamente."
                    : "Tipo de documento actualizado correctamente.";
            CSDialog.success("Guardado", msg);
            cargarTipoDoc();
            limpiarTipoDoc();
        } else {
            CSDialog.error("Error", "No se pudo guardar el tipo de documento.");
        }
    }

    @FXML
    private void guardarTipoComp() {
        if (!verificarAcceso()) return;
        String id = txtIdTipoComp.getText().trim();
        String nombre = txtNombreComp.getText().trim();

        if (id.isEmpty() || nombre.isEmpty()) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos.");
            return;
        }

        // Solo letras y espacios.
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            CSDialog.warning("Nombre inválido", "El nombre solo puede contener letras y espacios.");
            return;
        }

        String idExcluir = tipoCompSeleccionado != null
                ? tipoCompSeleccionado.getIdTipoComprobante() : null;
        if (tipoCompDAO.existeNombre(nombre, idExcluir)) {
            CSDialog.warning("Comprobante duplicado", "Ya existe un tipo de comprobante con ese nombre.");
            return;
        }

        TipoComprobante tc = new TipoComprobante(id, nombre);
        boolean exito = (tipoCompSeleccionado == null)
                ? tipoCompDAO.insertar(tc)
                : tipoCompDAO.actualizar(tc);

        if (exito) {
            String msg = tipoCompSeleccionado == null
                    ? "Tipo de comprobante registrado correctamente."
                    : "Tipo de comprobante actualizado correctamente.";
            CSDialog.success("Guardado", msg);
            cargarTipoComp();
            limpiarTipoComp();
        } else {
            CSDialog.error("Error", "No se pudo guardar el tipo de comprobante.");
        }
    }

    @FXML
    private void guardarMetodoPago() {
        if (!verificarAcceso()) return;
        String id = txtIdMetodoPago.getText().trim();
        String metodo = txtMetodoPago.getText().trim();
        if (id.isEmpty() || metodo.isEmpty()) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos.");
            return;
        }

        // Letras, números y espacios. Ej: "Tarjeta de Crédito", "Yape"
        if (!metodo.matches("[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]+")) {
            CSDialog.warning("Método inválido", "El método contiene caracteres no permitidos.");
            return;
        }

        String idExcluir = metodoPagoSeleccionado != null
                ? metodoPagoSeleccionado.getIdMetodoPago() : null;
        if (metodoPagoDAO.existeMetodo(metodo, idExcluir)) {
            CSDialog.warning("Método duplicado", "Ya existe un método de pago con ese nombre.");
            return;
        }

        MetodoPago mp = new MetodoPago(id, metodo);
        boolean exito = (metodoPagoSeleccionado == null)
                ? metodoPagoDAO.insertar(mp)
                : metodoPagoDAO.actualizar(mp);

        if (exito) {
            String msg = metodoPagoSeleccionado == null
                    ? "Método de pago registrado correctamente."
                    : "Método de pago actualizado correctamente.";
            CSDialog.success("Guardado", msg);
            cargarMetodoPago();
            limpiarMetodoPago();
        } else {
            CSDialog.error("Error", "No se pudo guardar el método de pago.");
        }
    }

    // ===== LIMPIAR =====
    @FXML
    private void limpiarTipoDoc() {
        txtDocumento.clear();
        tipoDocSeleccionado = null;
        lblTituloTipoDoc.setText("➕ Nuevo Tipo Documento");
        generarIdTipoDoc();
    }

    @FXML
    private void limpiarTipoComp() {
        txtNombreComp.clear();
        tipoCompSeleccionado = null;
        lblTituloTipoComp.setText("➕ Nuevo Tipo Comprobante");
        generarIdTipoComp();
    }

    @FXML
    private void limpiarMetodoPago() {
        txtMetodoPago.clear();
        metodoPagoSeleccionado = null;
        lblTituloMetodoPago.setText("➕ Nuevo Método de Pago");
        generarIdMetodoPago();
    }

    // ===== CONFIGURAR ACCIONES =====
    private <T> void configurarAcciones(TableColumn<T, Void> col, String tipo) {
        col.setCellFactory(c -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                // Mismas clases de estilo que la columna Acciones de
                // Categoría (.btn-table-edit / .btn-table-delete en
                // style.css): antes esta tabla tenía sus propios
                // colores sueltos (#4361ee/#ef233c) en vez de la
                // paleta de marca (#315E9E/#E94560), así que Acciones
                // se veía distinto según el módulo.
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                btnEditar.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    switch (tipo) {
                        case "tipoDoc" -> {
                            TipoDocumento td = (TipoDocumento) item;
                            tipoDocSeleccionado = td;
                            txtIdTipoDoc.setText(td.getIdTipoDocumento());
                            txtIdTipoDoc.setDisable(true);
                            txtDocumento.setText(td.getDocumento());
                            lblTituloTipoDoc.setText("Editar Tipo Documento");
                        }
                        case "tipoComp" -> {
                            TipoComprobante tc = (TipoComprobante) item;
                            tipoCompSeleccionado = tc;
                            txtIdTipoComp.setText(tc.getIdTipoComprobante());
                            txtIdTipoComp.setDisable(true);
                            txtNombreComp.setText(tc.getNombre());
                            lblTituloTipoComp.setText("Editar Tipo Comprobante");
                        }
                        case "metodoPago" -> {
                            MetodoPago mp = (MetodoPago) item;
                            metodoPagoSeleccionado = mp;
                            txtIdMetodoPago.setText(mp.getIdMetodoPago());
                            txtIdMetodoPago.setDisable(true);
                            txtMetodoPago.setText(mp.getMetodoDePago());
                            lblTituloMetodoPago.setText("Editar Método de Pago");
                        }
                    }
                });

                btnEliminar.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    boolean confirmado = CSDialog.confirm(
                            "Eliminar registro", "¿Eliminar este registro?",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        switch (tipo) {
                            case "tipoDoc" -> {
                                TipoDocumento td = (TipoDocumento) item;
                                ResultadoEliminacion r = validacion.validarTipoDocumento(
                                        td.getIdTipoDocumento(), td.getDocumento());
                                if (!r.isPermitido()) {
                                    r.mostrarAlerta();
                                    return;
                                }
                                if (tipoDocDAO.eliminar(td.getIdTipoDocumento())) {
                                    CSDialog.success("Eliminado", "El tipo de documento fue eliminado correctamente.");
                                    cargarTipoDoc();
                                } else {
                                    mostrarErrorEliminacion();
                                }
                            }
                            case "tipoComp" -> {
                                TipoComprobante tc = (TipoComprobante) item;
                                ResultadoEliminacion r = validacion.validarTipoComprobante(
                                        tc.getIdTipoComprobante(), tc.getNombre());
                                if (!r.isPermitido()) {
                                    r.mostrarAlerta();
                                    return;
                                }
                                if (tipoCompDAO.eliminar(tc.getIdTipoComprobante())) {
                                    CSDialog.success("Eliminado", "El tipo de comprobante fue eliminado correctamente.");
                                    cargarTipoComp();
                                } else {
                                    mostrarErrorEliminacion();
                                }
                            }
                            case "metodoPago" -> {
                                MetodoPago mp = (MetodoPago) item;
                                ResultadoEliminacion r = validacion.validarMetodoPago(
                                        mp.getIdMetodoPago(), mp.getMetodoDePago());
                                if (!r.isPermitido()) {
                                    r.mostrarAlerta();
                                    return;
                                }
                                if (metodoPagoDAO.eliminar(mp.getIdMetodoPago())) {
                                    CSDialog.success("Eliminado", "El método de pago fue eliminado correctamente.");
                                    cargarMetodoPago();
                                } else {
                                    mostrarErrorEliminacion();
                                }
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, btnEditar, btnEliminar);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }
}