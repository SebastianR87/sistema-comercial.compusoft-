package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.model.Empleado;
import pe.utp.security.Modulo;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;
import pe.utp.security.Sesion;
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
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cbCargo;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnMostrar;

    // Buscador y filtros
    @FXML private TextField txtBuscar;
    @FXML private ToggleButton btnTodos;
    @FXML private ToggleButton btnActivos;
    @FXML private ToggleButton btnInactivos;

    private boolean passwordVisible = false;

    private EmpleadoDAO dao = new EmpleadoDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();

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

        // Tooltip con la dirección completa al pasar el mouse
        Tooltip ttDireccion = new Tooltip();
        ttDireccion.textProperty().bind(txtDireccion.textProperty());
        txtDireccion.setTooltip(ttDireccion);
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

    /** Configura la columna Estado en colores. Un badge verde para ACTIVO y rojo para INACTIVO.*/
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
        if (btnActivos.isSelected()) filtroEstado = "ACTIVO";
        if (btnInactivos.isSelected()) filtroEstado = "INACTIVO";

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

        if (btnTodos.isSelected()) btnTodos.setStyle(estiloTodos);
        if (btnActivos.isSelected()) btnActivos.setStyle(estiloActivos);
        if (btnInactivos.isSelected()) btnInactivos.setStyle(estiloInactivos);
    }


    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnVer = new Button("Ver");
            final Button btnEditar = new Button("Editar");
            final Button btnEstado = new Button();
            // Botón de eliminación física, solo habilitado si no tiene movimientos
            final Button btnEliminar = new Button("Eliminar");

            {
                btnVer.getStyleClass().add("btn-table-view");
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                btnVer.setOnAction(e -> {
                    Empleado emp = getTableView().getItems().get(getIndex());
                    abrirModal(emp, EmpleadoModalController.MODO_VER);
                });
                // Botón Editar: abre el modal en modo EDITAR
                // Solo habilitado si el empleado está ACTIVO
                btnEditar.setOnAction(e -> {
                    // Antes solo guardar() (alta) verificaba el permiso
                    // de Modulo.EMPLEADOS -- editar/desactivar/eliminar
                    // no lo comprobaban en código, dependían solo de que
                    // el menú ocultara la pantalla para otros roles. Se
                    // agrega aquí como segunda capa de defensa.
                    if (!PermisoService.puedeAcceder(Modulo.EMPLEADOS)) {
                        PermisoUtil.denegado();
                        return;
                    }
                    Empleado emp = getTableView().getItems().get(getIndex());
                    abrirModal(emp, EmpleadoModalController.MODO_EDITAR);
                });

                // Botón dinámico: Desactivar o Reactivar según estado
                btnEstado.setOnAction(e -> {
                    if (!PermisoService.puedeAcceder(Modulo.EMPLEADOS)) {
                        PermisoUtil.denegado();
                        return;
                    }
                    Empleado emp = getTableView().getItems().get(getIndex());
                    boolean estaActivo = emp.getEstado().equals("ACTIVO");

                    // Solo al DESACTIVAR hace falta protegerse: reactivar
                    // nunca deja al sistema en un estado peor.
                    if (estaActivo) {
                        // Guard 1: no permitir que el usuario se
                        // desactive a sí mismo -- perdería acceso al
                        // sistema de inmediato (y si es el único admin,
                        // nadie más podría revertirlo).
                        if (esUsuarioDeLaSesionActual(emp)) {
                            CSDialog.warning("No se puede desactivar",
                                    "No puedes desactivar tu propio usuario mientras tienes " +
                                            "la sesión abierta. Pide a otro administrador que lo haga.");
                            return;
                        }
                        // Guard 2: no dejar el sistema sin NINGÚN
                        // administrador activo, sin importar de quién
                        // se trate.
                        if ("Administrador".equals(emp.getCargo())
                                && dao.contarAdministradoresActivos() <= 1) {
                            CSDialog.warning("No se puede desactivar",
                                    "\"" + emp.getNombre() + "\" es el único administrador activo. " +
                                            "Activa o registra otro administrador antes de desactivar este.");
                            return;
                        }
                    }

                    // Mensaje de confirmación descriptivo según la acción
                    String accion  = estaActivo ? "Desactivar" : "Reactivar";
                    String mensaje = estaActivo
                            ? "¿Desactivar a " + emp.getNombre() + "?\n" +
                              "Ya no podrá ingresar al sistema."
                            : "¿Reactivar a " + emp.getNombre() + "?\n" +
                              "Podrá volver a ingresar al sistema.";

                    // peligroso = estaActivo: solo DESACTIVAR se pinta en
                    // rojo (es la acción riesgosa); reactivar usa el navy
                    // neutro porque nunca deja al sistema en peor estado.
                    boolean confirmado = CSDialog.confirm(
                            accion + " empleado", mensaje, accion, "Cancelar", estaActivo, estaActivo);
                    if (confirmado) {
                        // Cambia al estado opuesto del actual
                        String nuevoEstado = estaActivo ? "INACTIVO" : "ACTIVO";
                        boolean exito = dao.cambiarEstado(
                                emp.getIdEmpleado(), nuevoEstado);
                        if (exito) {
                            // Recarga la tabla para reflejar el cambio
                            cargarTabla();
                            filtrar();
                        } else {
                            CSDialog.error("Error", "No se pudo cambiar el estado.");
                        }
                    }
                });


                btnEliminar.setOnAction(e -> {
                    if (!PermisoService.puedeAcceder(Modulo.EMPLEADOS)) {
                        PermisoUtil.denegado();
                        return;
                    }
                    Empleado emp = getTableView().getItems().get(getIndex());

                    ResultadoEliminacion validacionElim = validacion.validarEmpleado(
                            emp.getIdEmpleado(), emp.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }

                    // Mismos dos resguardos que Desactivar: eliminar es
                    // aún más grave (es irreversible), así que aplican
                    // con más razón.
                    if (esUsuarioDeLaSesionActual(emp)) {
                        CSDialog.warning("No se puede eliminar",
                                "No puedes eliminar tu propio usuario mientras tienes " +
                                        "la sesión abierta. Pide a otro administrador que lo haga.");
                        return;
                    }
                    if ("Administrador".equals(emp.getCargo())
                            && "ACTIVO".equals(emp.getEstado())
                            && dao.contarAdministradoresActivos() <= 1) {
                        CSDialog.warning("No se puede eliminar",
                                "\"" + emp.getNombre() + "\" es el único administrador activo. " +
                                        "Activa o registra otro administrador antes de eliminar este.");
                        return;
                    }

                    // Sin movimientos: pide confirmación antes de eliminar
                    boolean confirmado = CSDialog.confirm(
                            "Eliminar empleado",
                            "¿Eliminar a " + emp.getNombre() + "?\n" +
                                    "Esta acción eliminará al empleado permanentemente " +
                                    "y no se puede deshacer.",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        boolean exito = dao.eliminar(emp.getIdEmpleado());
                        if (exito) {
                            CSDialog.success("Empleado eliminado", "El empleado fue eliminado correctamente.");
                            cargarTabla();
                            filtrar();
                        } else {
                            CSDialog.error("Error", "No se pudo eliminar el empleado.");
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

                Empleado emp = getTableView().getItems().get(getIndex());
                boolean activo = emp.getEstado().equals("ACTIVO");

                // Editar solo habilitado para empleados ACTIVOS
                btnEditar.setDisable(!activo);
                btnEditar.setOpacity(activo ? 1.0 : 0.4);

                // Botón dinámico cambia texto y color según estado
                if (activo) {
                    btnEstado.setText("Desactivar");
                    // Naranja "Condicional" del módulo de Compatibilidad
                    // (#e67e00), para que el usuario asocie el mismo
                    // color con "estado intermedio / hay que revisar".
                    btnEstado.setStyle(
                            "-fx-background-color: #e67e00; -fx-text-fill: white;" +
                                    "-fx-font-weight: bold; -fx-padding: 5 12;" +
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
                ResultadoEliminacion res = validacion.validarEmpleado(
                        emp.getIdEmpleado(), emp.getNombre()
                );
                boolean tieneMov = !res.isPermitido();
                btnEliminar.setDisable(tieneMov);
                btnEliminar.setOpacity(tieneMov ? 0.4 : 1.0);
                HBox hbox = new HBox(5, btnVer, btnEditar, btnEstado, btnEliminar);
                hbox.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(hbox);
            }
        });
    }

    /**
     * true si "emp" es el mismo empleado que tiene la sesión abierta
     * ahora mismo. Se usa para bloquear que alguien se desactive o se
     * elimine a sí mismo por accidente (perdería acceso de inmediato,
     * y si era el único administrador, nadie más podría revertirlo).
     */
    private boolean esUsuarioDeLaSesionActual(Empleado emp) {
        Empleado actual = Sesion.getEmpleado();
        return actual != null && actual.getIdEmpleado().equals(emp.getIdEmpleado());
    }

    private void abrirModal(Empleado emp, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EmpleadoModal.fxml")
            );
            Parent root = loader.load();

            EmpleadoModalController ctrl = loader.getController();
            ctrl.setModo(modo, emp);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(modo.equals(EmpleadoModalController.MODO_VER)
                    ? "Detalle del Empleado"
                    : "Editar Empleado");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.setHeight(620);
            modal.showAndWait();

            // Recarga y mantiene el filtro activo después de cerrar el modal
            cargarTabla();
            filtrar();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir la ventana: " + ex.getMessage());
        }
    }

    @FXML
    private void guardar() {
        if (!PermisoService.puedeAcceder(pe.utp.security.Modulo.EMPLEADOS)) {
            PermisoUtil.denegado();
            return;
        }
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String cargo = cbCargo.getValue();
        TipoDocumento tipoDoc = cbTipoDocumento.getValue();
        String numeroDocumento = txtNumeroDocumento.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String password = passwordVisible ?
                txtPasswordVisible.getText().trim() :
                txtPassword.getText().trim();
        String telefono  = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();


        if (id.isEmpty() || nombre.isEmpty() || cargo == null ||
                tipoDoc == null || numeroDocumento.isEmpty() ||
                usuario.isEmpty() || password.isEmpty()) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos obligatorios.");
            return;
        }

        // Ve que el nombre solo tenga letras y espacios
        // [a-zA-ZáéíóúÁÉÍÓÚñÑ]+ = es un formato una o más letras incluyendo tildes y ñ
        // (\\s[a-zA-Z...]+)* = seguido de cero o más grupos (espacio + letras)
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(\\s[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*")) {
            CSDialog.warning("Nombre inválido", "El nombre solo debe contener letras.");
            return;
        }

        if (usuario.contains(" ")) {
            CSDialog.warning("Usuario inválido", "El usuario no debe contener espacios.");
            return;
        }

        // Solo letras, números y guiones bajos
        // \\w = [a-zA-Z0-9_], {4,20} = entre 4 y 20 caracteres
        if (!usuario.matches("\\w{4,20}")) {
            CSDialog.warning("Usuario inválido",
                    "El usuario debe tener entre 4 y 20 caracteres y solo letras, números o guión bajo.");
            return;
        }

        // RUC corregido a 11 dígitos (antes exigía 9, que es el
        // formato de Carnet de Extranjería -- probable copia/pega; el
        // mensaje de error ya decía "11 dígitos" pero la validación
        // real pedía 9, así que un RUC válido de 11 dígitos nunca
        // pasaba este formulario).
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

        if (dao.existeNumeroDocumento(numeroDocumento, null)) {
            CSDialog.warning("Documento duplicado", "Ya existe un empleado con ese número de documento.");
            return;
        }

        // Rango 7-15 dígitos: no asumir solo números peruanos (9 dígitos),
        // hay formatos internacionales válidos más cortos (ej. fijos de
        // 7-8 dígitos en otros países), con "+" de prefijo opcional.
        if (!telefono.isEmpty() && !telefono.matches("^\\+?[0-9]{7,15}$")) {
            CSDialog.warning("Teléfono inválido",
                    "Ingrese un teléfono válido (7 a 15 dígitos, con prefijo internacional opcional).");
            return;
        }

        Empleado e = new Empleado(id, nombre, cargo, usuario,
                password, tipoDoc.getIdTipoDocumento(), numeroDocumento,
                telefono, direccion, "ACTIVO");

        boolean exito = dao.insertar(e);
        if (exito) {
            CSDialog.success("Empleado registrado", "El empleado fue registrado correctamente.");
            cargarTabla();
            filtrar();
            limpiar();
        } else {
            CSDialog.error("Error", "No se pudo registrar el empleado.");
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
        txtTelefono.clear();
        txtDireccion.clear();
        generarId();
    }

}