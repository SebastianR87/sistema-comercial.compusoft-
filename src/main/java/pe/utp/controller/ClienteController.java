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

    private ClienteDAO       dao        = new ClienteDAO();
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
        if (btnDni.isSelected())       filtroTipo = "DNI";
        if (btnRuc.isSelected())       filtroTipo = "RUC";
        if (btnPasaporte.isSelected()) filtroTipo = "Pasaporte";
        if (btnCarnet.isSelected())    filtroTipo = "Carnet de Extranjería";

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
                btnVer.setStyle(
                        "-fx-background-color: #2dc653; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                );
                btnEditar.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                );
                btnEliminar.setStyle(
                        "-fx-background-color: #343a40; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                );

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
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar cliente");
                    confirm.setHeaderText("¿Eliminar a " + c.getNombre() + "?");
                    confirm.setContentText(
                            "Esta acción eliminará al cliente permanentemente\n" +
                                    "y no se puede deshacer."
                    );
                    confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            boolean exito = dao.eliminar(c.getIdCliente());
                            if (exito) {
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Cliente eliminado correctamente").showAndWait();
                                cargarTabla();
                                filtrar();
                            } else {
                                new Alert(Alert.AlertType.ERROR,
                                        "No se pudo eliminar el cliente").showAndWait();
                            }
                        }
                    });
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
                boolean tieneMov = dao.tieneMovimientos(c.getIdCliente());
                btnEliminar.setDisable(tieneMov);
                btnEliminar.setOpacity(tieneMov ? 0.4 : 1.0);

                HBox hbox = new HBox(5, btnVer, btnEditar, btnEliminar);
                hbox.setStyle("-fx-alignment: CENTER-LEFT;");
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

            Stage modal = new Stage();
            modal.setTitle(modo.equals(ClienteModalController.MODO_VER)
                    ? "Detalle del Cliente"
                    : "Editar Cliente");
            modal.setScene(new Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            // Recarga y mantiene el filtro después de cerrar el modal
            cargarTabla();
            filtrar();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la ventana: " + ex.getMessage()).show();
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
        String id              = txtId.getText().trim();
        String nombre          = txtNombre.getText().trim();
        TipoDocumento tipoDoc  = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String telefono        = txtTelefono.getText().trim();
        String correo          = txtCorreo.getText().trim();
        String direccion       = txtDireccion.getText().trim();

        // Validación 1: campos obligatorios
        if (id.isEmpty() || nombre.isEmpty() ||
                tipoDoc == null || numeroDocumento.isEmpty()) {
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

        // Validación 4: teléfono opcional pero validado si se ingresa
        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El teléfono debe tener exactamente 9 dígitos").showAndWait();
            return;
        }

        // Validación 5: correo opcional pero validado si se ingresa
        if (!correo.isEmpty() &&
                !correo.matches("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El correo no tiene un formato válido (ej: nombre@dominio.com)")
                    .showAndWait();
            return;
        }

        // Solo verifica duplicado si el correo no está vacío porque es un campo opcional
        if (!correo.isEmpty() && dao.existeCorreo(correo, null)) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe un cliente registrado con ese correo electrónico")
                    .showAndWait();
            return;
        }


        // Validación 6: número de documento duplicado
        if (dao.existeNumeroDocumento(numeroDocumento, null)) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe un cliente con ese número de documento")
                    .showAndWait();
            return;
        }

        // Crea el objeto con el orden correcto del constructor
        Cliente c = new Cliente(id, nombre, telefono, correo,
                direccion, tipoDoc, numeroDocumento);

        boolean exito = dao.insertar(c);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Cliente registrado correctamente").showAndWait();
            cargarTabla();
            filtrar();
            limpiar();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo registrar el cliente").showAndWait();
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