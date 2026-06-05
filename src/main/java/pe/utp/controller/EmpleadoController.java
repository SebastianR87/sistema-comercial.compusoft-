package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.model.Empleado;
import pe.utp.security.Modulo;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.model.TipoDocumento;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EmpleadoController implements AccesoControlable {

    @FXML private TableView<Empleado> tablaEmpleado;
    @FXML private TableColumn<Empleado, String> colId;
    @FXML private TableColumn<Empleado, String> colNombre;
    @FXML private TableColumn<Empleado, String> colCargo;
    @FXML private TableColumn<Empleado, String> colTipoDocumento;
    @FXML private TableColumn<Empleado, String> colNumeroDocumento;
    @FXML private TableColumn<Empleado, String>  colEstado;
    @FXML private TableColumn<Empleado, Void> colAcciones;

    // Formulario nuevo empleado
    @FXML private TextField     txtId;
    @FXML private TextField     txtNombre;
    @FXML private ComboBox<String> cbCargo;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
    @FXML private TextField     txtNumeroDocumento;
    @FXML private TextField     txtTelefono;
    @FXML private TextField     txtDireccion;
    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField     txtPasswordVisible;
    @FXML private Button        btnMostrar;

    // Buscador y filtros
    @FXML private TextField     txtBuscar;
    @FXML private ToggleButton  btnTodos;
    @FXML private ToggleButton  btnActivos;
    @FXML private ToggleButton  btnInactivos;

    private boolean passwordVisible = false;

    private EmpleadoDAO dao = new EmpleadoDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();
    private Empleado empleadoSeleccionado = null;

    private ObservableList<Empleado> listaCompleta = FXCollections.observableArrayList();
    private FilteredList<Empleado>   listaFiltrada;

    @FXML
    public void initialize() {
        // Conecta columnas con atributos del modelo
        colId.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colTipoDocumento.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getNombreTipoDocumento() != null
                                ? data.getValue().getNombreTipoDocumento()
                                : ""
                ));
        colNumeroDocumento.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));

        // Carga opciones del ComboBox
        configurarColumnaEstado();
        configurarColumnaAcciones();

        cbCargo.setItems(FXCollections.observableArrayList(
                "Administrador", "Vendedor", "Almacenero"
        ));
        listaFiltrada = new FilteredList<>(listaCompleta, e -> true);
        tablaEmpleado.setItems(listaFiltrada);
        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );
        cbTipoDocumento.setOnAction(e -> actualizarPlaceholder());

        generarId();
        cargarTabla();
        aplicarEstiloFiltros();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        // Solo el administrador accede a este módulo desde el menú.
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

    private void cargarTabla() {
        listaCompleta.setAll(dao.listar());
    }

    /**
     * Configura la columna Estado en colores. Un badge verde para ACTIVO y rojo para INACTIVO.
     */
    private void configurarColumnaEstado() {
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    return;
                }
                // Crea un Label estilizado como badge
                Label badge = new Label(estado);
                if (estado.equals("ACTIVO")) {
                    badge.setStyle(
                            "-fx-background-color: #eaf3de;" +
                                    "-fx-text-fill: #3b6d11;" +
                                    "-fx-background-radius: 99;" +
                                    "-fx-padding: 2 10;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                } else {
                    badge.setStyle(
                            "-fx-background-color: #fcebeb;" +
                                    "-fx-text-fill: #a32d2d;" +
                                    "-fx-background-radius: 99;" +
                                    "-fx-padding: 2 10;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                }
                // Centra el badge en la celda
                setGraphic(badge);
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
        });
        // Conecta colEstado con el atributo estado del modelo
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }


    @FXML
    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();

        // Determina qué filtro de estado está activo
        String filtroEstado = "TODOS";
        if (btnActivos.isSelected())   filtroEstado = "ACTIVO";
        if (btnInactivos.isSelected()) filtroEstado = "INACTIVO";

        // Copia local para usar dentro del lambda
        // (las variables en lambdas deben ser effectively final)
        final String estadoFinal = filtroEstado;

        listaFiltrada.setPredicate(emp -> {
            // Filtro por texto: busca en nombre, DNI y usuario simultáneamente
            boolean coincideTexto = texto.isEmpty()
                    || emp.getNombre().toLowerCase().contains(texto)
                    || emp.getNumeroDocumento().toLowerCase().contains(texto)
                    || emp.getUsuario().toLowerCase().contains(texto);

            // Filtro por estado: si es TODOS acepta cualquier estado
            boolean coincideEstado = estadoFinal.equals("TODOS")
                    || emp.getEstado().equals(estadoFinal);

            // El empleado aparece solo si cumple AMBAS condiciones
            return coincideTexto && coincideEstado;
        });

        // Actualiza el estilo visual de los botones según cuál está activo
        aplicarEstiloFiltros();
    }

    /** Aplica estilos visuales a los botones de filtro */
    private void aplicarEstiloFiltros() {
        String base = "-fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-font-size: 12px; -fx-padding: 6 12;";

        // Estilo inactivo: fondo gris claro
        String estiloNormal = base +
                "-fx-background-color: #f0f2f5; -fx-text-fill: #495057;";

        // Estilos activos: cada botón tiene su propio color
        String estiloTodos     = base + "-fx-background-color: #1a1a2e; -fx-text-fill: white;";
        String estiloActivos   = base + "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
        String estiloInactivos = base + "-fx-background-color: #fcebeb; -fx-text-fill: #a32d2d;";

        // Resetea todos primero, luego resalta el seleccionado
        btnTodos.setStyle(estiloNormal);
        btnActivos.setStyle(estiloNormal);
        btnInactivos.setStyle(estiloNormal);

        if (btnTodos.isSelected())     btnTodos.setStyle(estiloTodos);
        if (btnActivos.isSelected())   btnActivos.setStyle(estiloActivos);
        if (btnInactivos.isSelected()) btnInactivos.setStyle(estiloInactivos);
    }


    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnVer      = new Button("Ver");
            final Button btnEditar   = new Button("Editar");
            final Button btnEstado   = new Button();
            // Botón de eliminación física, solo habilitado si no tiene movimientos
            final Button btnEliminar = new Button("Eliminar");

            {
                btnVer.setStyle(
                        "-fx-background-color: #2dc653; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                );
                btnEditar.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");

                btnEliminar.setStyle(
                        "-fx-background-color: #ef233c; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");

                btnVer.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());
                    abrirModal(emp, EmpleadoModalController.MODO_VER);
                });
                // Botón Editar: abre el modal en modo EDITAR
                // Solo habilitado si el empleado está ACTIVO
                btnEditar.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());
                    abrirModal(emp, EmpleadoModalController.MODO_EDITAR);
                });

                // Botón dinámico: Desactivar o Reactivar según estado
                btnEstado.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());
                    boolean estaActivo = emp.getEstado().equals("ACTIVO");

                    // Mensaje de confirmación descriptivo según la acción
                    String accion  = estaActivo ? "desactivar" : "reactivar";
                    String mensaje = estaActivo
                            ? "¿Desactivar a " + emp.getNombre() + "?\n" +
                              "Ya no podrá ingresar al sistema."
                            : "¿Reactivar a " + emp.getNombre() + "?\n" +
                              "Podrá volver a ingresar al sistema.";

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            mensaje, ButtonType.YES, ButtonType.NO);
                    confirm.setTitle(accion.substring(0,1).toUpperCase()
                            + accion.substring(1) + " empleado");
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            // Cambia al estado opuesto del actual
                            String nuevoEstado = estaActivo ? "INACTIVO" : "ACTIVO";
                            boolean exito = dao.cambiarEstado(
                                    emp.getIdEmpleado(), nuevoEstado);
                            if (exito) {
                                // Recarga la tabla para reflejar el cambio
                                cargarTabla();
                                filtrar();
                            } else {
                                new Alert(Alert.AlertType.ERROR,
                                        "No se pudo cambiar el estado").show();
                            }
                        }
                    });
                });


                btnEliminar.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());

                    ResultadoEliminacion validacionElim = validacion.validarEmpleado(
                            emp.getIdEmpleado(), emp.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }

                    // Sin movimientos: pide confirmación antes de eliminar
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar empleado");
                    confirm.setHeaderText("¿Eliminar a " + emp.getNombre() + "?");
                    confirm.setContentText(
                            "Esta acción eliminará al empleado permanentemente\n" +
                                    "y no se puede deshacer."
                    );
                    confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            boolean exito = dao.eliminar(emp.getIdEmpleado());
                            if (exito) {
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Empleado eliminado correctamente").showAndWait();
                                cargarTabla();
                                filtrar();
                            } else {
                                new Alert(Alert.AlertType.ERROR,
                                        "No se pudo eliminar el empleado").showAndWait();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                // Si la fila está vacía no mostramos nada
                // Todo el código dinámico va dentro del else
                // para evitar IndexOutOfBoundsException en filas vacías
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Empleado emp = getTableView().getItems().get(getIndex());
                boolean activo = emp.getEstado().equals("ACTIVO");

                // Editar solo habilitado para empleados ACTIVOS
                btnEditar.setDisable(!activo);
                btnEditar.setOpacity(activo ? 1.0 : 0.4);

                // Botón dinámico cambia texto y color según estado
                if (activo) {
                    btnEstado.setText("Desactivar");
                    btnEstado.setStyle(
                            "-fx-background-color: #343a40; -fx-text-fill: white;" +
                                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                    );
                } else {
                    btnEstado.setText("Reactivar");
                    btnEstado.setStyle(
                            "-fx-background-color: #dcfce7; -fx-text-fill: #166534;"+
                                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;"
                    );
                }

                // Verifica movimientos para habilitar o deshabilitar Eliminar
                boolean tieneMov = dao.tieneMovimientos(emp.getIdEmpleado());
                btnEliminar.setDisable(tieneMov);
                btnEliminar.setOpacity(tieneMov ? 0.4 : 1.0);

                HBox hbox = new HBox(5, btnVer, btnEditar, btnEstado, btnEliminar);
                hbox.setStyle("-fx-alignment: CENTER-LEFT;");
                setGraphic(hbox);
            }
        });
    }

    private void abrirModal(Empleado emp, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EmpleadoModal.fxml")
            );
            Parent root = loader.load();

            EmpleadoModalController ctrl = loader.getController();
            ctrl.setModo(modo, emp);

            Stage modal = new Stage();
            modal.setTitle(modo.equals(EmpleadoModalController.MODO_VER)
                    ? "Detalle del Empleado"
                    : "Editar Empleado");
            modal.setScene(new Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            // Recarga y mantiene el filtro activo después de cerrar el modal
            cargarTabla();
            filtrar();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la ventana: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void guardar() {
        if (!PermisoService.puedeAcceder(Modulo.EMPLEADOS)) {
            PermisoUtil.denegado();
            return;
        }
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String cargo = cbCargo.getValue();
        TipoDocumento tipoDoc   = cbTipoDocumento.getValue();
        String numeroDocumento  = txtNumeroDocumento.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String password = passwordVisible ?
                txtPasswordVisible.getText().trim() :
                txtPassword.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();


        if (id.isEmpty() || nombre.isEmpty() || cargo == null ||
                tipoDoc == null || numeroDocumento.isEmpty() ||
                usuario.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Completa todos los campos obligatorios").showAndWait();
            return;
        }

        // Valida que el nombre solo tenga letras y espacios
        // [a-zA-ZáéíóúÁÉÍÓÚñÑ]+ = una o más letras incluyendo tildes y ñ
        // (\\s[a-zA-Z...]+)* = seguido de cero o más grupos (espacio + letras)
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            new Alert(Alert.AlertType.WARNING,
                    "El nombre solo debe contener letras").showAndWait();
            return;
        }

        if (usuario.contains(" ")) {
            new Alert(Alert.AlertType.WARNING,
                    "El usuario no debe contener espacios").showAndWait();
            return;
        }

        // Solo letras, números y guiones bajos
        // \\w = [a-zA-Z0-9_], {4,20} = entre 4 y 20 caracteres
        if (!usuario.matches("\\w{4,20}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El usuario debe tener entre 4 y 20 caracteres " +
                            "y solo letras, números o guión bajo").showAndWait();
            return;
        }

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

        if (dao.existeNumeroDocumento(numeroDocumento, null)) {
            new Alert(Alert.AlertType.WARNING,
                    "Ya existe un empleado con ese número de documento").showAndWait();
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            new Alert(Alert.AlertType.WARNING,
                    "El teléfono debe tener exactamente 9 dígitos").showAndWait();
            return;
        }

        Empleado e = new Empleado(id, nombre, cargo, usuario,
                password, tipoDoc.getIdTipoDocumento(), numeroDocumento,
                telefono, direccion, "ACTIVO");

        boolean exito = dao.insertar(e);
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Empleado registrado correctamente").showAndWait();
            cargarTabla();
            filtrar();
            limpiar();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo registrar el empleado").showAndWait();
        }
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("EMP001");
        } else {
            String prefijo = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero = Integer.parseInt(numeroStr) + 1;
            txtId.setText(String.format("%s%03d", prefijo, numero));
        }
        txtId.setDisable(true);
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
    private void limpiar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        cbCargo.setValue(null);
        cbTipoDocumento.setValue(null);
        txtNumeroDocumento.clear();
        txtNumeroDocumento.setPromptText("Selecciona un tipo primero");
        txtUsuario.clear();
        txtPassword.clear();
        txtPasswordVisible.clear();
        passwordVisible = false;
        txtPassword.setVisible(true);
        txtPassword.setManaged(true);
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
        btnMostrar.setText("👁");
        empleadoSeleccionado = null;
        txtTelefono.clear();
        txtDireccion.clear();
        generarId();
    }

}