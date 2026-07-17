package pe.utp.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.ProveedorDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Proveedor;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.util.ResultadoEliminacion;

public class ProveedorController implements AccesoControlable {

    // Tabla y columnas
    @FXML private TableView<Proveedor>            tablaProveedor;
    @FXML private TableColumn<Proveedor, String>  colId;
    @FXML private TableColumn<Proveedor, String>  colNombre;
    @FXML private TableColumn<Proveedor, String>  colRuc;
    @FXML private TableColumn<Proveedor, String>  colTelefono;
    @FXML private TableColumn<Proveedor, String>  colDireccion;
    @FXML private TableColumn<Proveedor, Void>    colAcciones;

    // Formulario nuevo proveedor
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRuc;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;

    // Buscador
    @FXML private TextField txtBuscar;

    //DAOs y listas
    private ProveedorDAO dao = new ProveedorDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();

    // FilteredList para filtrar en memoria sin ir a BD
    private ObservableList<Proveedor> listaCompleta =
            FXCollections.observableArrayList();
    private FilteredList<Proveedor> listaFiltrada;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProveedor"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRuc.setCellValueFactory(new PropertyValueFactory<>("ruc"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        // FilteredList conectada a listaCompleta
        listaFiltrada = new FilteredList<>(listaCompleta, p -> true);
        tablaProveedor.setItems(listaFiltrada);

        configurarColumnaAcciones();
        generarId();
        cargarTabla();
        aplicarPermisos();

        // Tooltip con la dirección completa al pasar el mouse
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
    }

    @Override
    public void aplicarPermisos() {
        // Almacenero y administrador tienen acceso completo
    }

    private void cargarTabla() {
        listaCompleta.setAll(dao.listar());
    }

    @FXML
    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        listaFiltrada.setPredicate(p -> {
            if (texto.isEmpty()) return true;
            return p.getNombre().toLowerCase().contains(texto)
                    || p.getRuc().toLowerCase().contains(texto);
        });
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {

            final Button btnVer      = new Button("Ver");
            final Button btnEditar   = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnVer.getStyleClass().add("btn-table-view");
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                btnVer.setOnAction(e -> {
                    Proveedor p = getTableView().getItems().get(getIndex());
                    abrirModal(p, ProveedorModalController.MODO_VER);
                });

                btnEditar.setOnAction(e -> {
                    Proveedor p = getTableView().getItems().get(getIndex());
                    abrirModal(p, ProveedorModalController.MODO_EDITAR);
                });

                btnEliminar.setOnAction(e -> {
                    Proveedor p = getTableView().getItems().get(getIndex());

                    // Usa ValidacionEliminacionDAO para verificar compras
                    // Da mensaje detallado con el número de compras asociadas
                    ResultadoEliminacion resultado = validacion.validarProveedor(
                            p.getIdProveedor(), p.getNombre()
                    );
                    if (!resultado.isPermitido()) {
                        resultado.mostrarAlerta();
                        return;
                    }

                    boolean confirmado = CSDialog.confirm(
                            "Eliminar proveedor",
                            "¿Eliminar a " + p.getNombre() + "?\n" +
                                    "Esta acción eliminará al proveedor permanentemente " +
                                    "y no se puede deshacer.",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        boolean exito = dao.eliminar(p.getIdProveedor());
                        if (exito) {
                            CSDialog.success("Proveedor eliminado", "El proveedor fue eliminado correctamente.");
                            cargarTabla();
                            filtrar();
                        } else {
                            CSDialog.error("Error", "No se pudo eliminar el proveedor.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                // Deshabilita Eliminar si tiene compras registradas
                Proveedor p = getTableView().getItems().get(getIndex());
                ResultadoEliminacion res = validacion.validarProveedor(
                        p.getIdProveedor(), p.getNombre()
                );
                boolean tieneMov = !res.isPermitido();
                btnEliminar.setDisable(tieneMov);
                btnEliminar.setOpacity(tieneMov ? 0.4 : 1.0);

                HBox hbox = new HBox(5, btnVer, btnEditar, btnEliminar);
                hbox.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(hbox);
            }
        });
    }

    private void abrirModal(Proveedor p, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ProveedorModal.fxml")
            );
            Parent root = loader.load();

            ProveedorModalController ctrl = loader.getController();
            ctrl.setModo(modo, p);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(modo.equals(ProveedorModalController.MODO_VER)
                    ? "Detalle del Proveedor"
                    : "Editar Proveedor");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            cargarTabla();
            filtrar();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir la ventana: " + ex.getMessage());
        }
    }

    @FXML
    private void guardar() {
        if (!PermisoService.puedeEditarProveedor()) {
            PermisoUtil.denegado();
            return;
        }

        String id        = txtId.getText().trim();
        String nombre    = txtNombre.getText().trim();
        String ruc       = txtRuc.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (id.isEmpty() || nombre.isEmpty() || ruc.isEmpty()) {
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
        // Rango 7-15 dígitos con "+" opcional: no asumir solo números
        // peruanos, hay formatos internacionales más cortos que
        // también son válidos.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
            return;
        }

        // Validación 5: RUC duplicado
        if (dao.existeRuc(ruc, null)) {
            CSDialog.warning("RUC duplicado", "Ya existe un proveedor con ese RUC.");
            return;
        }

        Proveedor p = new Proveedor(id, nombre, ruc, telefono, direccion);

        boolean exito = dao.insertar(p);
        if (exito) {
            CSDialog.success("Proveedor registrado", "El proveedor fue registrado correctamente.");
            cargarTabla();
            filtrar();
            limpiar();
        } else {
            CSDialog.error("Error", "No se pudo registrar el proveedor.");
        }
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("PROV001");
        } else {
            String prefijo   = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero       = Integer.parseInt(numeroStr) + 1;
            txtId.setText(String.format("%s%03d", prefijo, numero));
        }
        txtId.setDisable(true);
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        txtRuc.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        generarId();
    }
}