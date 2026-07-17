package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.CompatibilidadDAO;
import pe.utp.dao.ProductoDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Compatibilidad;
import pe.utp.model.Producto;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

import java.util.List;

public class CompatibilidadController implements AccesoControlable {

    // Formulario
    @FXML private VBox panelFormulario;
    @FXML private Label lblTituloForm;
    @FXML private Button btnGuardarRegla;
    @FXML private ComboBox<Producto> cbProducto1;
    @FXML private ComboBox<Producto> cbProducto2;
    @FXML private ComboBox<String>  cbEstado;
    @FXML private TextField txtRestriccion;

    // Verificador rápido
    @FXML private ComboBox<Producto> cbVerificarA;
    @FXML private ComboBox<Producto> cbVerificarB;
    @FXML private VBox panelResultado;
    @FXML private Label lblResultadoIcono;
    @FXML private Label lblResultadoEstado;
    @FXML private Label lblResultadoCodigo;
    @FXML private Label lblResultadoDesc;
    @FXML private Label lblResultadoNota;

    // Tabla
    @FXML private TextField txtFiltro;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private Label lblContadorReglas;
    @FXML private TableView<Compatibilidad> tablaCompatibilidad;
    @FXML private TableColumn<Compatibilidad, String> colProductoBase;
    @FXML private TableColumn<Compatibilidad, String> colTipoBase;
    @FXML private TableColumn<Compatibilidad, String> colProductoObj;
    @FXML private TableColumn<Compatibilidad, String> colTipoObj;
    @FXML private TableColumn<Compatibilidad, String> colEstadoTabla;
    @FXML private TableColumn<Compatibilidad, String> colRestriccion;
    @FXML private TableColumn<Compatibilidad, Void>   colAcciones;

    private CompatibilidadDAO dao = new CompatibilidadDAO();
    private ProductoDAO productoDAO = new ProductoDAO();

    private ObservableList<Compatibilidad> listaReglas =
            FXCollections.observableArrayList();
    private FilteredList<Compatibilidad>   listaFiltrada;

    // Regla seleccionada para edición
    private Compatibilidad reglaSeleccionada = null;

    @FXML
    public void initialize() {
        configurarCombosEstado();
        configurarComboProductos();
        configurarTabla();
        cargarTabla();
    }

    @Override
    public void aplicarPermisos() {
        // Administrador: acceso completo
        // Vendedor: solo verificador y vista de tabla (sin editar)
        boolean esAdmin = PermisoService.puedeAcceder(
                pe.utp.security.Modulo.CONFIGURACION
        );
        // Si no es admin oculta el botón Nueva Regla
        // Se maneja desde el FXML con visible/managed
    }

    // COMBOS
    private void configurarCombosEstado() {
        ObservableList<String> estados = FXCollections.observableArrayList(
                Compatibilidad.COMPATIBLE,
                Compatibilidad.NO_COMPATIBLE,
                Compatibilidad.CONDICIONAL
        );
        cbEstado.setItems(estados);
        cbEstado.setValue(Compatibilidad.COMPATIBLE);

        // Filtro de estado en la tabla
        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "Todos", Compatibilidad.COMPATIBLE,
                Compatibilidad.NO_COMPATIBLE, Compatibilidad.CONDICIONAL
        ));
        cbFiltroEstado.setValue("Todos");
    }

    private void configurarComboProductos() {
        List<Producto> todos = productoDAO.listar().stream()
                .filter(p -> "Activo".equals(p.getEstado()))
                .toList();
        ObservableList<Producto> lista =
                FXCollections.observableArrayList(todos);

        // Configura los 4 combos con el mismo StringConverter
        for (ComboBox<Producto> cb : List.of(
                cbProducto1, cbProducto2, cbVerificarA, cbVerificarB)) {
            cb.setEditable(true);
            cb.setItems(FXCollections.observableArrayList(todos));
            cb.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Producto p) {
                    return p == null ? "" : p.getNombre();
                }
                @Override public Producto fromString(String s) {
                    return lista.stream()
                            .filter(p -> p.getNombre().equals(s))
                            .findFirst().orElse(null);
                }
            });

            // Listener de búsqueda en tiempo real
            cb.getEditor().textProperty().addListener((obs, old, newVal) -> {
                Producto sel = null;
                try { Object v = cb.getValue();
                    if (v instanceof Producto pv) sel = pv; }
                catch (Exception ignored) {}
                if (sel != null && sel.getNombre().equals(newVal)) return;
                if (sel != null) cb.setValue(null);
                String texto = newVal == null ? "" : newVal.toLowerCase();
                cb.setItems(FXCollections.observableArrayList(
                        todos.stream()
                                .filter(p -> texto.isEmpty()
                                        || p.getNombre().toLowerCase().contains(texto)
                                        || p.getIdProducto().toLowerCase().contains(texto))
                                .toList()
                ));
                if (!cb.isShowing() && !texto.isEmpty()) cb.show();
            });
        }
    }


    @FXML
    private void guardar() {
        Object v1 = cbProducto1.getValue();
        Object v2 = cbProducto2.getValue();
        Producto p1 = (v1 instanceof Producto) ? (Producto) v1 : null;
        Producto p2 = (v2 instanceof Producto) ? (Producto) v2 : null;
        String estado = cbEstado.getValue();

        // Validación 1: ambos productos seleccionados
        if (p1 == null || p2 == null) {
            CSDialog.warning("Componentes no seleccionados", "Selecciona ambos componentes de la lista.");
            return;
        }

        // Validación 2: no puede ser el mismo producto
        if (p1.getIdProducto().equals(p2.getIdProducto())) {
            CSDialog.warning("Componentes inválidos", "Los dos componentes no pueden ser el mismo producto.");
            return;
        }

        // Validación 3: estado obligatorio
        if (estado == null) {
            CSDialog.warning("Estado requerido", "Selecciona el estado de compatibilidad.");
            return;
        }

        if (reglaSeleccionada == null) {
            // Validación 4: no duplicar reglas
            if (dao.existeRegla(p1.getIdProducto(), p2.getIdProducto())) {
                CSDialog.warning("Regla duplicada",
                        "Ya existe una regla de compatibilidad entre estos dos componentes. " +
                                "Usa Editar en la tabla para modificarla.");
                return;
            }

            String id = generarId();
            Compatibilidad c = new Compatibilidad(
                    id, p1, p2, estado, txtRestriccion.getText().trim()
            );
            if (dao.insertar(c)) {
                CSDialog.success("Regla registrada", "La regla fue registrada correctamente.");
                cargarTabla();
                ocultarFormulario();
            } else {
                CSDialog.error("Error", "No se pudo registrar la regla.");
            }
        } else {
            // Edición
            reglaSeleccionada.setEstado(estado);
            reglaSeleccionada.setRestriccion(txtRestriccion.getText().trim());
            if (dao.actualizar(reglaSeleccionada)) {
                CSDialog.success("Regla actualizada", "La regla fue actualizada correctamente.");
                cargarTabla();
                ocultarFormulario();
            } else {
                CSDialog.error("Error", "No se pudo actualizar la regla.");
            }
        }
    }

    // ── VERIFICADOR RÁPIDO
    @FXML
    private void verificarCompatibilidad() {
        Object va = cbVerificarA.getValue();
        Object vb = cbVerificarB.getValue();
        Producto a = (va instanceof Producto) ? (Producto) va : null;
        Producto b = (vb instanceof Producto) ? (Producto) vb : null;

        if (a == null || b == null) {
            CSDialog.warning("Componentes no seleccionados", "Selecciona ambos componentes para verificar.");
            return;
        }
        if (a.getIdProducto().equals(b.getIdProducto())) {
            CSDialog.warning("Componentes inválidos", "Selecciona dos componentes diferentes.");
            return;
        }

        Compatibilidad resultado = dao.verificar(
                a.getIdProducto(), b.getIdProducto()
        );

        panelResultado.setVisible(true);
        panelResultado.setManaged(true);

        if (resultado == null) {
            // Sin regla definida
            aplicarEstiloResultado(
                    "❓", "SIN REGLA",
                    "#6c757d", "#f8f9fa",
                    a.getNombre() + " y " + b.getNombre() +
                            " no tienen una regla de compatibilidad definida.",
                    "Puedes agregar una regla en la Matriz Global."
            );
            return;
        }

        switch (resultado.getEstado()) {
            case Compatibilidad.COMPATIBLE -> aplicarEstiloResultado(
                    "✅", "COMPATIBLE",
                    "#2dc653", "#eef8f0",
                    a.getNombre() + " es totalmente compatible con " + b.getNombre() + ".",
                    resultado.getRestriccion() != null &&
                            !resultado.getRestriccion().isEmpty()
                            ? "Nota: " + resultado.getRestriccion() : ""
            );
            case Compatibilidad.NO_COMPATIBLE -> aplicarEstiloResultado(
                    "❌", "NO COMPATIBLE",
                    "#e94560", "#fff0f2",
                    a.getNombre() + " NO es compatible con " + b.getNombre() + ".",
                    resultado.getRestriccion() != null &&
                            !resultado.getRestriccion().isEmpty()
                            ? "Razón: " + resultado.getRestriccion() : ""
            );
            case Compatibilidad.CONDICIONAL -> aplicarEstiloResultado(
                    "⚠", "CONDICIONAL",
                    "#e67e00", "#fff8f0",
                    a.getNombre() + " puede ser compatible con " + b.getNombre() +
                            " bajo ciertas condiciones.",
                    resultado.getRestriccion() != null &&
                            !resultado.getRestriccion().isEmpty()
                            ? "Condición: " + resultado.getRestriccion() : ""
            );
        }
    }

    /**
     * Aplica el estilo visual al panel de resultado
     * según el estado de compatibilidad.
     */
    private void aplicarEstiloResultado(String icono, String estado,
                                        String colorTexto, String colorFondo,
                                        String descripcion, String nota) {
        panelResultado.setStyle(
                "-fx-background-color: " + colorFondo + ";" +
                        "-fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: " + colorTexto + "40;" +
                        "-fx-border-width: 1; -fx-padding: 12;"
        );
        lblResultadoIcono.setText(icono);
        lblResultadoEstado.setText(estado);
        lblResultadoEstado.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + colorTexto + ";" +
                        "-fx-text-fill: white; -fx-background-radius: 4;" +
                        "-fx-padding: 2 8;"
        );
        lblResultadoCodigo.setText("");
        lblResultadoDesc.setText(descripcion);
        lblResultadoNota.setText(nota);
        lblResultadoNota.setVisible(!nota.isEmpty());
        lblResultadoNota.setManaged(!nota.isEmpty());
    }

    // ── TABLA Y FILTROS
    private void configurarTabla() {
        colProductoBase.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto1() != null
                                ? d.getValue().getProducto1().getNombre() : ""
                )
        );
        colTipoBase.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto1() != null &&
                                d.getValue().getProducto1().getCategoria() != null
                                ? d.getValue().getProducto1().getCategoria().getNombre()
                                : ""
                )
        );
        colProductoObj.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto2() != null
                                ? d.getValue().getProducto2().getNombre() : ""
                )
        );
        colTipoObj.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto2() != null &&
                                d.getValue().getProducto2().getCategoria() != null
                                ? d.getValue().getProducto2().getCategoria().getNombre()
                                : ""
                )
        );
        colRestriccion.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getRestriccion() != null
                                ? d.getValue().getRestriccion() : ""
                )
        );

        // Columna Estado con color según valor
        colEstadoTabla.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setText(null); setStyle(""); return;
                }
                String estado = getTableView().getItems()
                        .get(getIndex()).getEstado();
                setText(estado);
                String color = switch (estado) {
                    case Compatibilidad.COMPATIBLE -> "#2dc653";
                    case Compatibilidad.NO_COMPATIBLE -> "#e94560";
                    case Compatibilidad.CONDICIONAL -> "#e67e00";
                    default -> "#6c757d";
                };
                setStyle("-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;");
            }
        });

        // Columna acciones: Ver, Editar y Eliminar
        // (mismos estilos y disposición que Producto/Cliente: Ver abre
        // un modal de solo lectura, Editar reutiliza la tarjeta "Nueva
        // Regla" ya visible en pantalla, Eliminar pide una confirmación
        // pequeña con Alert en vez de un modal propio)
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnVer      = new Button("Ver");
            final Button btnEditar   = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");
            {
                btnVer.getStyleClass().add("btn-table-view");
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                btnVer.setOnAction(e -> {
                    Compatibilidad c = getTableView()
                            .getItems().get(getIndex());
                    abrirModalVer(c);
                });
                btnEditar.setOnAction(e -> {
                    Compatibilidad c = getTableView()
                            .getItems().get(getIndex());
                    editarRegla(c);
                });
                btnEliminar.setOnAction(e -> {
                    Compatibilidad c = getTableView()
                            .getItems().get(getIndex());
                    eliminarRegla(c);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox hbox = new HBox(5, btnVer, btnEditar, btnEliminar);
                hbox.setAlignment(Pos.CENTER);
                setGraphic(hbox);
            }
        });

        listaFiltrada = new FilteredList<>(listaReglas, c -> true);
        tablaCompatibilidad.setItems(listaFiltrada);
    }

    /**
     * Abre el modal de solo lectura ("Ver") con el mismo estilo visual
     * del módulo Productos. No permite editar nada: para modificar la
     * regla el usuario debe usar "Editar" en la tabla, que reutiliza
     * la tarjeta "Nueva Regla" ya presente en esta misma pantalla.
     */
    private void abrirModalVer(Compatibilidad c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CompatibilidadVerModal.fxml")
            );
            Parent root = loader.load();

            CompatibilidadVerModalController ctrl = loader.getController();
            ctrl.setDatos(c);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle("Detalle de la Regla de Compatibilidad");
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (Exception ex) {
            CSDialog.error("Error", "No se pudo abrir la ventana: " + ex.getMessage());
        }
    }

    private void editarRegla(Compatibilidad c) {
        reglaSeleccionada = c;
        lblTituloForm.setText("✏️ Editar Regla de Compatibilidad");
        btnGuardarRegla.setText("💾 Actualizar Regla");

        // Pre-carga los productos (solo edita estado y restricción)
        cbProducto1.setValue(c.getProducto1());
        cbProducto2.setValue(c.getProducto2());
        // Deshabilita cambio de productos al editar
        cbProducto1.setDisable(true);
        cbProducto2.setDisable(true);

        cbEstado.setValue(c.getEstado());
        txtRestriccion.setText(
                c.getRestriccion() != null ? c.getRestriccion() : ""
        );

        panelFormulario.setVisible(true);
        panelFormulario.setManaged(true);
    }

    private void eliminarRegla(Compatibilidad c) {
        boolean confirmado = CSDialog.confirm(
                "Eliminar regla",
                "¿Eliminar la regla entre:\n" +
                        c.getProducto1().getNombre() + "\n" +
                        c.getProducto2().getNombre() + "?",
                "Eliminar", "Cancelar", true, true);
        if (confirmado) {
            if (dao.eliminar(c.getIdDetalle())) {
                CSDialog.success("Regla eliminada", "La regla fue eliminada correctamente.");
                cargarTabla();
            } else {
                CSDialog.error("Error", "No se pudo eliminar la regla.");
            }
        }
    }

    @FXML
    private void filtrar() {
        String texto  = txtFiltro.getText().trim().toLowerCase();
        String estado = cbFiltroEstado.getValue();
        listaFiltrada.setPredicate(c -> {
            boolean matchTexto = texto.isEmpty()
                    || c.getProducto1().getNombre().toLowerCase().contains(texto)
                    || c.getProducto2().getNombre().toLowerCase().contains(texto);
            boolean matchEstado = "Todos".equals(estado)
                    || estado == null
                    || estado.equals(c.getEstado());
            return matchTexto && matchEstado;
        });
        actualizarContador();
    }

    private void cargarTabla() {
        listaReglas.setAll(dao.listar());
        actualizarContador();
        // Restaura combos al cargar
        cbProducto1.setDisable(false);
        cbProducto2.setDisable(false);
    }

    private void actualizarContador() {
        long total = listaFiltrada != null
                ? listaFiltrada.stream().count()
                : listaReglas.size();
        lblContadorReglas.setText(
                "Mostrando " + total + " de " + listaReglas.size() + " reglas"
        );
    }

    private String generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) return "COMP001";
        String prefijo   = ultimo.replaceAll("[0-9]", "");
        String numeroStr = ultimo.replaceAll("[^0-9]", "");
        int numero = numeroStr.isEmpty()
                ? 1 : Integer.parseInt(numeroStr) + 1;
        return String.format("%s%03d", prefijo, numero);
    }

    //FORMULARIO
    @FXML private void mostrarFormulario() {
        reglaSeleccionada = null;
        lblTituloForm.setText("➕ Nueva Regla de Compatibilidad");
        btnGuardarRegla.setText("💾 Guardar Regla");
        limpiarFormulario();
        panelFormulario.setVisible(true);
        panelFormulario.setManaged(true);
    }

    @FXML private void ocultarFormulario() {
        panelFormulario.setVisible(false);
        panelFormulario.setManaged(false);
        limpiarFormulario();
    }

    @FXML private void limpiarFormulario() {
        cbProducto1.setValue(null);
        cbProducto1.getEditor().clear();
        cbProducto2.setValue(null);
        cbProducto2.getEditor().clear();
        cbEstado.setValue(Compatibilidad.COMPATIBLE);
        txtRestriccion.clear();
        reglaSeleccionada = null;
        // Siempre rehabilita los combos al limpiar
        // porque pueden haber quedado deshabilitados desde editarRegla()
        cbProducto1.setDisable(false);
        cbProducto2.setDisable(false);
        // Como reglaSeleccionada queda en null, la tarjeta vuelve a
        // representar "Nueva Regla" sin importar desde dónde se llamó
        // (Limpiar durante una edición no debe dejar el título/botón
        // en estado "Editar/Actualizar" de forma inconsistente).
        lblTituloForm.setText("➕ Nueva Regla de Compatibilidad");
        btnGuardarRegla.setText("💾 Guardar Regla");
    }
}