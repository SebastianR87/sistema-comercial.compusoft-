package pe.utp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.utp.dao.ProductoDAO;
import pe.utp.model.Producto;

public class ProductoModalController {

    public static final String MODO_VER    = "VER";
    public static final String MODO_EDITAR = "EDITAR";

    // Cabecera
    @FXML private VBox panelCabecera;
    @FXML private Label lblTituloCabecera;
    @FXML private Label lblNombreCabecera;
    @FXML private Label lblCategoriaCabecera;

    // Campos
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecioCompra;
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

    @FXML
    public void initialize() {
        // Carga las opciones del estado
        cbEstado.setItems(FXCollections.observableArrayList(
                "Activo", "Inactivo"
        ));
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
        lblCategoriaCabecera.setText(
                p.getCategoria() != null
                        ? p.getCategoria().getNombre()
                        : ""
        );

        txtId.setText(p.getIdProducto());
        txtNombre.setText(p.getNombre());
        txtDescripcion.setText(
                p.getDescripcion() != null ? p.getDescripcion() : ""
        );
        // Si precio_compra es 0, el producto aún no tiene
        // ninguna compra registrada
        if (p.getPrecioCompra() == 0) {
            txtPrecioCompra.setText("Sin compras registradas");
        } else {
            txtPrecioCompra.setText(
                    String.format("%.2f", p.getPrecioCompra())
            );
        }
        txtPrecioVenta.setText(
                String.format("%.2f", p.getPrecioVenta())
        );
        txtStock.setText(String.valueOf(p.getStock()));
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        txtStockMaximo.setText(String.valueOf(p.getStockMaximo()));
        cbEstado.setValue(p.getEstado());
    }

    /**
     * Modo VER: campos de solo lectura, sin botón guardar.
     * Cabecera azul.
     */
    private void configurarModoVer() {
        panelCabecera.setStyle(
                "-fx-background-color: #4361ee; -fx-padding: 20 24;"
        );
        lblTituloCabecera.setText("DETALLE DE PRODUCTO");

        // Deshabilita todos los campos
        txtNombre.setEditable(false);
        txtDescripcion.setEditable(false);
        txtPrecioCompra.setEditable(false);
        txtPrecioVenta.setEditable(false);
        txtStock.setEditable(false);
        txtStockMinimo.setEditable(false);
        txtStockMaximo.setEditable(false);
        cbEstado.setDisable(true);

        // Oculta el botón guardar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);
        btnCancelar.setText("✖ Cerrar");
    }

    /**
     * Modo EDITAR: campos editables, botón guardar visible.
     * Cabecera verde.
     */
    private void configurarModoEditar() {
        panelCabecera.setStyle(
                "-fx-background-color: #2dc653; -fx-padding: 20 24;"
        );
        lblTituloCabecera.setText("EDITAR PRODUCTO");

        txtNombre.setEditable(true);
        txtDescripcion.setEditable(true);
        txtPrecioVenta.setEditable(true);
        txtStockMinimo.setEditable(true);
        txtStockMaximo.setEditable(true);
        cbEstado.setDisable(false);
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);
        btnCancelar.setText("✖ Cancelar");
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cbEstado.getValue();

        // Validación 1: campos obligatorios
        // txtPrecioCompra NO se valida porque es solo lectura
        // (lo actualiza el CPP desde CompraDAO, no el usuario)
        if (nombre.isEmpty() || txtPrecioVenta.getText().isEmpty()
                || estado == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Completa todos los campos obligatorios").showAndWait();
            return;
        }

        try {
            double precioVenta = Double.parseDouble(
                    txtPrecioVenta.getText().trim().replace(",", ".")
            );

            // Validación 2: precio no negativo
            if (precioVenta < 0) {
                new Alert(Alert.AlertType.WARNING,
                        "El precio de venta no puede ser negativo")
                        .showAndWait();
                return;
            }

            // Stock mínimo y máximo: valores de alerta definidos
            // por el administrador, no calculados automáticamente
            int stockMinimo = 0;
            int stockMaximo = 0;
            try {
                stockMinimo = Integer.parseInt(
                        txtStockMinimo.getText().trim());
                stockMaximo = Integer.parseInt(
                        txtStockMaximo.getText().trim());
            } catch (NumberFormatException ignored) {
                // Si no ingresaron número válido queda en 0
            }

            // Validación 3: mínimo no mayor que máximo
            if (stockMaximo > 0 && stockMinimo > stockMaximo) {
                new Alert(Alert.AlertType.WARNING,
                        "El stock mínimo no puede ser mayor al máximo.")
                        .showAndWait();
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
                new Alert(Alert.AlertType.INFORMATION,
                        "Producto actualizado correctamente").showAndWait();
                cerrarModal();
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "No se pudo actualizar el producto").showAndWait();
            }

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING,
                    "El precio debe ser un número válido.\n" +
                            "Ejemplo: 150.50").showAndWait();
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