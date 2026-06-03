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
import pe.utp.model.MetodoPago;
import pe.utp.model.TipoComprobante;
import pe.utp.model.TipoDocumento;

public class ConfiguracionController {

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

    private TipoDocumentoDAO tipoDocDAO = new TipoDocumentoDAO();
    private TipoComprobanteDAO tipoCompDAO = new TipoComprobanteDAO();
    private MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

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
        String id = txtIdTipoDoc.getText().trim();
        String documento = txtDocumento.getText().trim();
        if (id.isEmpty() || documento.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
        }
        TipoDocumento td = new TipoDocumento(id, documento);
        if (tipoDocSeleccionado == null) tipoDocDAO.insertar(td);
        else tipoDocDAO.actualizar(td);
        cargarTipoDoc();
        limpiarTipoDoc();
    }

    @FXML
    private void guardarTipoComp() {
        String id = txtIdTipoComp.getText().trim();
        String nombre = txtNombreComp.getText().trim();
        if (id.isEmpty() || nombre.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
        }
        TipoComprobante tc = new TipoComprobante(id, nombre);
        if (tipoCompSeleccionado == null) tipoCompDAO.insertar(tc);
        else tipoCompDAO.actualizar(tc);
        cargarTipoComp();
        limpiarTipoComp();
    }

    @FXML
    private void guardarMetodoPago() {
        String id = txtIdMetodoPago.getText().trim();
        String metodo = txtMetodoPago.getText().trim();
        if (id.isEmpty() || metodo.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completa todos los campos").show();
            return;
        }
        MetodoPago mp = new MetodoPago(id, metodo);
        if (metodoPagoSeleccionado == null) metodoPagoDAO.insertar(mp);
        else metodoPagoDAO.actualizar(mp);
        cargarMetodoPago();
        limpiarMetodoPago();
    }

    // ===== LIMPIAR =====
    @FXML
    private void limpiarTipoDoc() {
        txtDocumento.clear();
        tipoDocSeleccionado = null;
        generarIdTipoDoc();
    }

    @FXML
    private void limpiarTipoComp() {
        txtNombreComp.clear();
        tipoCompSeleccionado = null;
        generarIdTipoComp();
    }

    @FXML
    private void limpiarMetodoPago() {
        txtMetodoPago.clear();
        metodoPagoSeleccionado = null;
        generarIdMetodoPago();
    }

    // ===== CONFIGURAR ACCIONES =====
    private <T> void configurarAcciones(TableColumn<T, Void> col, String tipo) {
        col.setCellFactory(c -> new TableCell<>() {
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
                    Object item = getTableView().getItems().get(getIndex());
                    switch (tipo) {
                        case "tipoDoc" -> {
                            TipoDocumento td = (TipoDocumento) item;
                            tipoDocSeleccionado = td;
                            txtIdTipoDoc.setText(td.getIdTipoDocumento());
                            txtIdTipoDoc.setDisable(true);
                            txtDocumento.setText(td.getDocumento());
                        }
                        case "tipoComp" -> {
                            TipoComprobante tc = (TipoComprobante) item;
                            tipoCompSeleccionado = tc;
                            txtIdTipoComp.setText(tc.getIdTipoComprobante());
                            txtIdTipoComp.setDisable(true);
                            txtNombreComp.setText(tc.getNombre());
                        }
                        case "metodoPago" -> {
                            MetodoPago mp = (MetodoPago) item;
                            metodoPagoSeleccionado = mp;
                            txtIdMetodoPago.setText(mp.getIdMetodoPago());
                            txtIdMetodoPago.setDisable(true);
                            txtMetodoPago.setText(mp.getMetodoDePago());
                        }
                    }
                });

                btnEliminar.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Eliminar este registro?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            switch (tipo) {
                                case "tipoDoc" -> {
                                    tipoDocDAO.eliminar(((TipoDocumento) item).getIdTipoDocumento());
                                    cargarTipoDoc();
                                }
                                case "tipoComp" -> {
                                    tipoCompDAO.eliminar(((TipoComprobante) item).getIdTipoComprobante());
                                    cargarTipoComp();
                                }
                                case "metodoPago" -> {
                                    metodoPagoDAO.eliminar(((MetodoPago) item).getIdMetodoPago());
                                    cargarMetodoPago();
                                }
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(new HBox(6, btnEditar, btnEliminar));
            }
        });
    }
}