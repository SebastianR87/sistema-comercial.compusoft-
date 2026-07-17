package pe.utp.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import pe.utp.dao.ProductoDAO;
import pe.utp.dialog.CSDialog;
import pe.utp.model.Producto;
import javafx.beans.property.SimpleStringProperty;
import pe.utp.dao.LoteDAO;
import pe.utp.model.Lote;
import pe.utp.Conexion.ConexionDB;
import java.time.format.DateTimeFormatter;

public class ProductoModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera
    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;

    // Campos
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TableView<Lote> tablaLotes;
    @FXML private TableColumn<Lote, String> colLoteFecha;
    @FXML private TableColumn<Lote, String> colLoteCantidad;
    @FXML private TableColumn<Lote, String> colLoteCosto;
    @FXML private TableColumn<Lote, String> colLoteSubtotal;
    @FXML private Label lblValorInventario;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtStockMaximo;
    @FXML private ComboBox<String> cbEstado;

    // Botones
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private ProductoDAO dao = new ProductoDAO();
    private Producto producto;

    @FXML public void initialize() {
        cbEstado.setItems(FXCollections.observableArrayList(
                "Activo", "Inactivo"
        ));

        colLoteFecha.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getFecha().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ));
        colLoteCantidad.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getCantidadRestante())));
        colLoteCosto.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("S/ %.2f", data.getValue().getCostoUnitario())));
        colLoteSubtotal.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("S/ %.2f", data.getValue().getValorRestante())));
    }

    public void setModo(String modo, Producto p) {
        this.producto = p;
        cargarDatos(p);

        if (modo.equals(MODO_VER)) {
            configurarModoVer();
        } else {
            configurarModoEditar();
        }
    }

    private void cargarDatos(Producto p) {
        lblNombreCabecera.setText(p.getNombre());

        txtId.setText(p.getIdProducto());
        txtNombre.setText(p.getNombre());
        txtDescripcion.setText(
                p.getDescripcion() != null ? p.getDescripcion() : ""
        );
        txtPrecioVenta.setText(
                String.format("%.2f", p.getPrecioVenta())
        );
        txtStock.setText(String.valueOf(p.getStock()));
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        txtStockMaximo.setText(String.valueOf(p.getStockMaximo()));
        cbEstado.setValue(p.getEstado());

        cargarLotes(p.getIdProducto());
    }

    /** Carga los lotes activos del producto (orden FIFO) y el valor total en inventario. */
    private void cargarLotes(String idProducto) {
        LoteDAO loteDAO = new LoteDAO(ConexionDB.getConexion());
        var lotes = loteDAO.listarLotesDisponibles(idProducto);
        tablaLotes.setItems(FXCollections.observableArrayList(lotes));

        // Altura dinámica: crece según la cantidad real de lotes,
        // con un tope de 5 filas visibles (de ahí en adelante, scroll)
        double alturaFila = 28;
        double alturaHeader = 28;
        int filasVisibles = Math.min(Math.max(lotes.size(), 1), 5);
        tablaLotes.setPrefHeight(alturaHeader + (filasVisibles * alturaFila));
        tablaLotes.setFixedCellSize(alturaFila);

        // Mensaje cuando no hay lotes (producto sin compras registradas)
        if (lotes.isEmpty()) {
            tablaLotes.setPlaceholder(new Label("Sin lotes disponibles"));
        }

        double valorTotal = lotes.stream().mapToDouble(Lote::getValorRestante).sum();
        lblValorInventario.setText(String.format("Valor total en inventario: S/ %.2f", valorTotal));
    }

    /**
     * Modo VER: deshabilita los campos editables (quedan grises,
     * de solo lectura) mediante setDisable, apoyado en el estilo
     * :disabled de la clase "campo-input" en el CSS global.
     */
    private void configurarModoVer() {
        lblTituloCabecera.setText("DETALLE DE PRODUCTO");

        txtNombre.setDisable(true);
        txtDescripcion.setDisable(true);
        txtPrecioVenta.setDisable(true);
        txtStockMinimo.setDisable(true);
        txtStockMaximo.setDisable(true);
        cbEstado.setDisable(true);

        // Oculta el botón guardar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        // En modo Ver, el único botón visible (Cerrar) toma el
        // color de marca, ya que es la acción principal disponible
        btnCancelar.setText("Cerrar");
        btnCancelar.getStyleClass().setAll("btn-primary");
        btnCancelar.setStyle("");
    }

    /**
     * Modo EDITAR: habilita los campos editables, muestra el
     * botón Guardar.
     */
    private void configurarModoEditar() {
        lblTituloCabecera.setText("EDITAR PRODUCTO");

        txtNombre.setDisable(false);
        txtDescripcion.setDisable(false);
        txtPrecioVenta.setDisable(false);
        txtStockMinimo.setDisable(false);
        txtStockMaximo.setDisable(false);
        cbEstado.setDisable(false);

        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);

        // En modo Editar, Cancelar vuelve a su estilo neutro
        // (blanco con borde), y Guardar es el que lleva el color
        btnCancelar.setText("Cancelar");
        btnCancelar.getStyleClass().remove("btn-primary");
        btnCancelar.setStyle(
                "-fx-background-color: white; -fx-text-fill: #334155;" +
                        "-fx-border-color: #CBD5E1; -fx-border-radius: 8;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-padding: 9 18; -fx-font-size: 13px;");
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cbEstado.getValue();

        // Validación 1: campos obligatorios
        if (nombre.isEmpty() || txtPrecioVenta.getText().isEmpty()
                || estado == null) {
            CSDialog.warning("Campos incompletos", "Completa todos los campos obligatorios.");
            return;
        }

        try {
            double precioVenta = Double.parseDouble(
                    txtPrecioVenta.getText().trim().replace(",", ".")
            );

            // Validación 2: precio no negativo
            if (precioVenta < 0) {
                CSDialog.warning("Precio inválido", "El precio de venta no puede ser negativo.");
                return;
            }

            // Stock mínimo y máximo: valores de alerta definidos
            // por el administrador, no calculados automáticamente.
            // Antes un texto no numérico aquí (ej. dejar "abc" o un
            // espacio) se atrapaba en silencio y el producto quedaba
            // guardado con mínimo/máximo en 0 sin avisar al usuario --
            // en un producto YA EXISTENTE esto podía borrar valores de
            // alerta configurados antes, sin que nadie lo notara.
            int stockMinimo;
            int stockMaximo;
            try {
                stockMinimo = Integer.parseInt(
                        txtStockMinimo.getText().trim());
                stockMaximo = Integer.parseInt(
                        txtStockMaximo.getText().trim());
            } catch (NumberFormatException ex) {
                CSDialog.warning("Stock inválido",
                        "El stock mínimo y máximo deben ser números enteros válidos.");
                return;
            }

            if (stockMinimo < 0 || stockMaximo < 0) {
                CSDialog.warning("Stock inválido",
                        "El stock mínimo y máximo no pueden ser negativos.");
                return;
            }

            // Validación 3: mínimo no mayor que máximo
            if (stockMaximo > 0 && stockMinimo > stockMaximo) {
                CSDialog.warning("Stock inválido", "El stock mínimo no puede ser mayor al máximo.");
                return;
            }

            // Verifica que el nombre no esté repetido con otro
            // producto (excluyendo al propio, ver ProductoDAO.existeNombre)
            if (dao.existeNombre(nombre, producto.getIdProducto())) {
                CSDialog.warning("Producto duplicado", "Ya existe otro producto con ese nombre.");
                return;
            }

            // Actualiza solo los campos que el usuario puede modificar
            producto.setNombre(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecioVenta(precioVenta);
            producto.setEstado(estado);
            producto.setStockMinimo(stockMinimo);
            producto.setStockMaximo(stockMaximo);
            // precio_compra y stock NO se tocan desde aquí

            boolean exito = dao.actualizar(producto);
            if (exito) {
                // Cierra el modal y difiere el mensaje con Platform.runLater
                // (ver comentario detallado en ClienteModalController.guardar()):
                // cerrar este modal y abrir el diálogo en el mismo pulso hacía
                // que el diálogo quedara "abierto" pero sin pintarse.
                cerrarModal();
                Platform.runLater(() ->
                        CSDialog.success("Producto actualizado", "El producto fue actualizado correctamente."));
            } else {
                CSDialog.error("Error", "No se pudo actualizar el producto.");
            }

        } catch (NumberFormatException e) {
            CSDialog.warning("Precio inválido", "El precio debe ser un número válido. Ejemplo: 150.50");
        }
    }

    @FXML
    private void cancelar() {
        cerrarModal();
    }

    private void cerrarModal() {
        Stage stage = (Stage) txtId.getScene().getWindow();
        stage.close();
    }
}
