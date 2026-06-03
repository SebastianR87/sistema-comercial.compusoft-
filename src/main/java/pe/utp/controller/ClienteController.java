package pe.utp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import pe.utp.dao.ClienteDAO;
import pe.utp.dao.TipoDocumentoDAO;
import pe.utp.model.Cliente;
import pe.utp.model.TipoDocumento;

public class ClienteController {

    @FXML private TableView<Cliente> tablaCliente;
    @FXML private TableColumn<Cliente, String> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTipoDoc;
    @FXML private TableColumn<Cliente, String> colNumDoc;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, Void> colAcciones;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<TipoDocumento> cbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtBuscar;

    private ClienteDAO dao = new ClienteDAO();
    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private Cliente clienteSeleccionado = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNumDoc.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));

        colTipoDoc.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTipoDocumento().getDocumento()
                )
        );

        cbTipoDocumento.setItems(
                FXCollections.observableArrayList(tipoDocDAO.listar())
        );
        generarId();

        configurarColumnaAcciones();
        cargarTabla();
    }

    private void cargarTabla() {
        ObservableList<Cliente> lista =
                FXCollections.observableArrayList(dao.listar());
        tablaCliente.setItems(lista);
    }

    @FXML
    private void buscar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            cargarTabla();
        } else {
            ObservableList<Cliente> lista =
                    FXCollections.observableArrayList(dao.buscarPorNombre(texto));
            tablaCliente.setItems(lista);
        }
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.setStyle(
                        "-fx-background-color: #4361ee; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
                btnEliminar.setStyle(
                        "-fx-background-color: #ef233c; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");

                btnEditar.setOnAction(e -> {
                    Cliente c = getTableView().getItems().get(getIndex());
                    clienteSeleccionado = c;
                    txtId.setText(c.getIdCliente());
                    txtId.setDisable(true);
                    txtNombre.setText(c.getNombre());
                    cbTipoDocumento.setValue(c.getTipoDocumento());
                    txtNumeroDocumento.setText(c.getNumeroDocumento());
                    txtTelefono.setText(c.getTelefono());
                    txtCorreo.setText(c.getCorreo());
                    txtDireccion.setText(c.getDireccion());
                });

                btnEliminar.setOnAction(e -> {
                    Cliente c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar cliente " + c.getNombre() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            dao.eliminar(c.getIdCliente());
                            cargarTabla();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(6, btnEditar, btnEliminar);
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void guardar() {
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        TipoDocumento tipoDoc = cbTipoDocumento.getValue();
        String numDoc = txtNumeroDocumento.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (id.isEmpty() || nombre.isEmpty() || tipoDoc == null || numDoc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa los campos obligatorios").show();
            return;
        }

        Cliente c = new Cliente(id, nombre, telefono, correo,
                direccion, numDoc, tipoDoc);

        if (clienteSeleccionado == null) {
            dao.insertar(c);
        } else {
            dao.actualizar(c);
        }

        cargarTabla();
        limpiar();
    }

    @FXML
    private void limpiar() {
        txtId.clear();
        txtId.setDisable(false);
        txtNombre.clear();
        cbTipoDocumento.setValue(null);
        txtNumeroDocumento.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        txtDireccion.clear();
        clienteSeleccionado = null;
        generarId();
    }

    private void generarId() {
        String ultimo = dao.obtenerUltimoId();
        if (ultimo == null) {
            txtId.setText("CLI001");
        } else {
            String prefijo = ultimo.replaceAll("[0-9]", "");
            String numeroStr = ultimo.replaceAll("[^0-9]", "");
            int numero = Integer.parseInt(numeroStr) + 1;
            txtId.setText(String.format("%s%03d", prefijo, numero));
        }
        txtId.setDisable(true);
    }
}