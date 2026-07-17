package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.utp.dao.CategoriaDAO;
import pe.utp.dao.ValidacionEliminacionDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Categoria;
import pe.utp.util.ResultadoEliminacion;
import pe.utp.security.PermisoService;
import pe.utp.security.PermisoUtil;

public class CategoriaController implements AccesoControlable {

    @FXML private Label lblModoConsulta;
    @FXML private Button btnNuevaCategoria;
    @FXML private TableView<Categoria> tablaCategoria;
    @FXML private TableColumn<Categoria, String> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Label lblContador;

    private CategoriaDAO dao = new CategoriaDAO();
    private ValidacionEliminacionDAO validacion = new ValidacionEliminacionDAO();
    private ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Categoria> listaFiltrada;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCategoria"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        configurarColumnaAcciones();
        cargarTabla();
        aplicarPermisos();
    }

    @Override
    public void aplicarPermisos() {
        // Antes se ocultaba el panel de "Nueva Categoría" completo
        // (panelFormulario); ahora ese panel ya no existe como tal --
        // crear es un botón, así que se oculta el botón en su lugar.
        if (!PermisoService.puedeEditarCategoria()) {
            lblModoConsulta.setVisible(true);
            lblModoConsulta.setManaged(true);
            PermisoUtil.ocultar(btnNuevaCategoria);
            PermisoUtil.ocultarColumna(colAcciones);
        }
    }

    private void cargarTabla() {
        listaCategorias.setAll(dao.Listar());
        listaFiltrada = new javafx.collections.transformation
                .FilteredList<>(listaCategorias, c -> true);
        tablaCategoria.setItems(listaFiltrada);
        actualizarContador();
    }

    private void actualizarContador() {
        int total = listaFiltrada != null
                ? (int) listaFiltrada.stream().count()
                : listaCategorias.size();
        lblContador.setText(total + " registros");
    }

    @FXML
    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        listaFiltrada.setPredicate(c ->
                texto.isEmpty() ||
                        c.getNombre().toLowerCase().contains(texto) ||
                        c.getIdCategoria().toLowerCase().contains(texto)
        );
        actualizarContador();
    }

    @FXML
    private void abrirModalCrear() {
        if (!PermisoService.puedeEditarCategoria()) {
            PermisoUtil.denegado();
            return;
        }
        abrirModal(null, CategoriaModalController.MODO_CREAR);
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.getStyleClass().add("btn-table-edit");
                btnEliminar.getStyleClass().add("btn-table-delete");

                btnEditar.setOnAction(e -> {
                    if (!PermisoService.puedeEditarCategoria()) {
                        PermisoUtil.denegado();
                        return;
                    }
                    Categoria c = getTableView().getItems().get(getIndex());
                    abrirModal(c, CategoriaModalController.MODO_EDITAR);
                });

                btnEliminar.setOnAction(e -> {
                    Categoria c = getTableView().getItems().get(getIndex());
                    ResultadoEliminacion validacionElim = validacion.validarCategoria(
                            c.getIdCategoria(), c.getNombre());
                    if (!validacionElim.isPermitido()) {
                        validacionElim.mostrarAlerta();
                        return;
                    }
                    boolean confirmado = CSDialog.confirm(
                            "Eliminar categoría",
                            "¿Eliminar " + c.getNombre() + "?",
                            "Eliminar", "Cancelar", true, true);
                    if (confirmado) {
                        if (dao.eliminar(c.getIdCategoria())) {
                            CSDialog.success("Categoría eliminada", "La categoría fue eliminada correctamente.");
                            cargarTabla();
                            filtrar();
                        } else {
                            CSDialog.error("Error", "No se pudo eliminar la categoría.");
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

    /**
     * Abre el modal de Categoría (Crear o Editar), igual patrón que
     * ProveedorController.abrirModal(). Para Crear, c es null: el
     * propio modal genera el ID y arranca con un objeto Categoria vacío.
     */
    private void abrirModal(Categoria c, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CategoriaModal.fxml")
            );
            Parent root = loader.load();

            CategoriaModalController ctrl = loader.getController();
            ctrl.setModo(modo, c);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/style.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(modo.equals(CategoriaModalController.MODO_CREAR)
                    ? "Nueva Categoría"
                    : "Editar Categoría");
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
}
