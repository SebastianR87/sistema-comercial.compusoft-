package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
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
import pe.utp.dao.ClienteDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

public class ClienteController implements AccesoControlable {

    @FXML private TableView<Cliente> tablaCliente;
    @FXML private TableColumn<Cliente, String>  colId;
    @FXML private TableColumn<Cliente, String>  colNombre;
    @FXML private TableColumn<Cliente, String>  colTipoDoc;
    @FXML private TableColumn<Cliente, String>  colNumDoc;
    @FXML private TableColumn<Cliente, String>  colTelefono;
    @FXML private TableColumn<Cliente, String>  colCorreo;
    @FXML private TableColumn<Cliente, Void>    colAcciones;

    @FXML private TextField txtId;
    @FXML private TextField  txtNombre;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;

    @FXML private TextField    txtBuscar;
    @FXML private ToggleButton btnTodos;
    @FXML private ToggleButton btnDni;
    @FXML private ToggleButton btnRuc;
    @FXML private ToggleButton btnPasaporte;
    @FXML private ToggleButton btnCarnet;

    private ClienteDAO dao = new ClienteDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();

    private ObservableList<Cliente> listaCompleta = FXCollections.observableArrayList();
    private FilteredList<Cliente>   listaFiltrada;

    @FXML
    public void initialize() {
        // Conecta columnas con atributos del modelo
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNumDoc.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));

        colTipoDoc.setCellValueFactory(data -> {
            TipoDocumento td = data.getValue().getTipoDocumento();
            return new SimpleStringProperty(td != null ? td.getDocumento() : "");
        });

        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );

        cbTipoDocumento.setOnAction(e -> actualizarPlaceholder());

        // Inicializa FilteredList conectada a listaCompleta
        listaFiltrada = new FilteredList<>(listaCompleta, c -> true);
        tablaCliente.setItems(listaFiltrada);

        configurarColumnaAcciones();
        generarId();
        cargarTabla();
        aplicarEstiloFiltros();
        aplicarPermisos();

        // Tooltip con la dirección completa al pasar el mouse
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
    }

    @Override
    public void aplicarPermisos() {
        // Vendedor y administrador tienen acceso completo a clientes.
    }

    /** Carga todos los clientes en listaCompleta */
    private void cargarTabla() {
        listaCompleta.setAll(dao.listar());
    }

    /** Filtra la tabla en tiempo real según */
    @FXML
    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();

        // Determina qué tipo de documento está filtrando
        // Si ninguno específico está activo, muestra todos
        String filtroTipo = "TODOS";
        if (btnDni.isSelected()) filtroTipo = "DNI";
        if (btnRuc.isSelected()) filtroTipo = "RUC";
        if (btnPasaporte.isSelected()) filtroTipo = "Pasaporte";
        if (btnCarnet.isSelected()) filtroTipo = "Carnet de Extranjería";

        final String tipoFinal = filtroTipo;

        listaFiltrada.setPredicate(c -> {
            // Filtro por texto en nombre o número de documento
            boolean coincideTexto = texto.isEmpty()
                    || c.getNombre().toLowerCase().contains(texto)
                    || c.getNumeroDocumento().toLowerCase().contains(texto);

            // Filtro por tipo de documento
            // Si es TODOS acepta cualquier tipo
            boolean coincideTipo = tipoFinal.equals("TODOS");
            if (!coincideTipo && c.getTipoDocumento() != null) {
                coincideTipo = c.getTipoDocumento().getDocumento()
                        .equals(tipoFinal);
            }

            return coincideTexto && coincideTipo;
        });

        aplicarEstiloFiltros();
    }

    private void aplicarEstiloFiltros() {
        String base   = "-fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-font-size: 12px; -fx-padding: 6 12;";
        String normal = base + "-fx-background-color: #f0f2f5; " +
                "-fx-text-fill: #495057;";

        // Cada tipo tiene su propio color para distinguirlos visualmente
        String sTodos     = base + "-fx-background-color: #1a1a2e; -fx-text-fill: white;";
        String sDni       = base + "-fx-background-color: #e8f0fe; -fx-text-fill: #1a56db;";
        String sRuc       = base + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
        String sPasaporte = base + "-fx-background-color: #f3e8ff; -fx-text-fill: #6b21a8;";
        String sCarnet    = base + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";

        // Resetea todos primero
        btnTodos.setStyle(normal);
        btnDni.setStyle(normal);
        btnRuc.setStyle(normal);
        btnPasaporte.setStyle(normal);
        btnCarnet.setStyle(normal);

        // Resalta el activo con su color correspondiente
        if (btnTodos.isSelected())    btnTodos.setStyle(sTodos);
        if (btnDni.isSelected())      btnDni.setStyle(sDni);
        if (btnRuc.isSelected())      btnRuc.setStyle(sRuc);
        if (btnPasaporte.isSelected()) btnPasaporte.setStyle(sPasaporte);
        if (btnCarnet.isSelected())   btnCarnet.setStyle(sCarnet);
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
                    Cliente c = getTableView().getItems().get(getIndex());
                    abrirModal(c, ClienteModalController.MODO_VER);
                });

                btnEditar.setOnAction(e -> {
                    Cliente c = getTableView().getItems().get(getIndex());
                    abrirModal(c, ClienteModalController.MODO_EDITAR);
                });

                // Eliminar: verifica movimientos antes de proceder
                btnEliminar.setOnAction(e -> {
                    Cliente c = getTableView().getItems().get(getIndex());

                    ResultadoEliminacion validacionElim = validacion.validarCliente(
                            c.getIdCliente(), c.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }

                    // Sin movimientos: pide confirmación y elimina
                    boolean confirmado = CSDialog.confirm(
                            "Eliminar cliente",
                            "¿Eliminar a " + c.getNombre() + "?\n" +
                                    "Esta acción eliminará al cliente permanentemente " +
                                    "y no se puede deshacer.",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        boolean exito = dao.eliminar(c.getIdCliente());
                        if (exito) {
                            CSDialog.success("Cliente eliminado", "El cliente fue eliminado correctamente.");
                            cargarTabla();
                            filtrar();
                        } else {
                            CSDialog.error("Error", "No se pudo eliminar el cliente.");
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

                // Verifica movimientos para deshabilitar Eliminar si tiene
                Cliente c = getTableView().getItems().get(getIndex());
                ResultadoEliminacion res = validacion.validarCliente(
                        c.getIdCliente(), c.getNombre()
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

    private void abrirModal(Cliente c, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ClienteModal.fxml")
            );
            Parent root = loader.load();

            ClienteModalController ctrl = loader.getController();
            ctrl.setModo(modo, c);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(modo.equals(ClienteModalController.MODO_VER)
                    ? "Detalle del Cliente"
                    : "Editar Cliente");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            // Sin ScrollPane en este modal, el alto ideal lo determina
            // el propio contenido -- no forzamos un alto fijo aquí.
            modal.showAndWait();

            // Recarga y mantiene el filtro después de cerrar el modal
            cargarTabla();
            filtrar();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir la ventana: " + ex.getMessage());
        }
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
    private void guardar() {
        if (!PermisoService.puedeEditarCliente()) {
            PermisoUtil.denegado();
            return;
        }
        String id = txtId.getText().trim();
        String nombre  = txtNombre.getText().trim();
        TipoDocumento tipoDoc  = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios. El correo NO va aquí:
        // el propio FXML lo documenta como opcional ("Correo:
        // opcional pero validado si se ingresa", Cliente.fxml)
        if (id.isEmpty() || nombre.isEmpty() ||
                tipoDoc == null || numeroDocumento.isEmpty()) {
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

        // Validación 4: teléfono opcional pero validado si se ingresa.
        // Rango 7-15 dígitos con "+" opcional: no asumir solo números
        // peruanos, hay formatos internacionales más cortos que
        // también son válidos.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
            return;
        }

        // Solo verifica duplicado si el correo no está vacío porque es un campo opcional
        if (!correo.isEmpty() && dao.existeCorreo(correo, null)) {
            CSDialog.warning("Correo duplicado", "Ya existe un cliente registrado con ese correo electrónico.");
            return;
        }

        // Validación 5: correo opcional pero validado si se ingresa
        if (!correo.isEmpty() &&
                !correo.matches("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}")) {
            CSDialog.warning("Correo inválido", "El correo no tiene un formato válido (ej: nombre@dominio.com).");
            return;
        }

        // Validación 6: número de documento duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, null)) {
            CSDialog.warning("Documento duplicado", "Ya existe un cliente con ese número de documento.");
            return;
        }

        // Crea el objeto con el orden correcto del constructor
        Cliente c = new Cliente(id, nombre, telefono, correo,
                direccion, tipoDoc, numeroDocumento);

        boolean exito = dao.insertar(c);
        if (exito) {
            CSDialog.success("Cliente registrado", "El cliente fue registrado correctamente.");
            cargarTabla();
            filtrar();
            limpiar();
        } else {
            CSDialog.error("Error", "No se pudo registrar el cliente.");
        }
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("CLI001");
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
        cbTipoDocumento.setValue(null);
        txtNumeroDocumento.clear();
        txtNumeroDocumento.setPromptText("Selecciona un tipo primero");
        txtTelefono.clear();
        txtCorreo.clear();
        txtDireccion.clear();
        generarId();
    }
}