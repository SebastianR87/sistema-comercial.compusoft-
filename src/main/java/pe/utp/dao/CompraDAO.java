package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
import pe.utp.model.Compra;
import pe.utp.model.DetalleCompra;
import pe.utp.model.Empleado;
import pe.utp.model.Proveedor;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompraDAO {

    private Connection conexion;

    public CompraDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    // Lista de avisos generados durante la última compra registrada.
    // El controlador la consulta después de registrarCompra() para mostrar alertas sin interrumpir la transacción.
    private List<String> avisos = new ArrayList<>();

    public List<String> getAvisos() {
        return avisos;
    }

    /**
     * Registra la compra completa en una sola transacción. Una transacción
     * garantiza que si algo falla en el medio, ningún cambio queda guardado
     * a medias.
     * Costeo por lotes (FIFO): cada línea de compra crea su propio lote
     * en lote_compra, con su costo real e independiente -- ya no se
     * calcula un Costo Promedio Ponderado (CPP) que mezcle precios de
     * compras distintas. Esto evita que una diferencia fuerte de precio
     * entre dos compras del mismo producto distorsione el costo real
     * usado luego para decidir el precio de venta.
     */
    public boolean registrarCompra(Compra compra,
                                   List<DetalleCompra> detalles) {
        try {
            // Desactiva el autocommit para manejar la transacción manualmente
            // Sin esto cada INSERT haría su propio commit automáticamente
            conexion.setAutoCommit(false);
            avisos.clear();
            LoteDAO loteDAO = new LoteDAO(conexion);

            // Paso 1: inserta la cabecera de la compra
            String sqlCompra = "INSERT INTO compra " +
                    "(id_compra, id_proveedor, id_empleado, " +
                    "numero_comprobante, fecha, total) " +
                    "VALUES (?,?,?,?,?,?)";

            PreparedStatement psCompra = conexion.prepareStatement(sqlCompra);
            psCompra.setString(1, compra.getIdCompra());
            psCompra.setString(2, compra.getProveedor().getIdProveedor());
            psCompra.setString(3, compra.getEmpleado().getIdEmpleado());
            psCompra.setString(4, compra.getNumeroComprobante());
            // Convierte LocalDateTime a Timestamp que entiende JDBC
            psCompra.setTimestamp(5, Timestamp.valueOf(compra.getFecha()));
            psCompra.setDouble(6, compra.getTotal());
            psCompra.executeUpdate();

            // Paso 2: por cada producto comprado, inserta el detalle
            // y crea su propio lote independiente
            String sqlDetalle = "INSERT INTO detallecompra " +
                    "(id_detallecompra, id_compra, " +
                    "id_producto, cantidad, precio) " +
                    "VALUES (?,?,?,?,?)";

            String sqlLeerProducto = "SELECT stock, stock_maximo, nombre " +
                    "FROM producto WHERE id_producto = ?";

            // Antes esto era "SET stock = ?" (absoluto): se leía el
            // stock, se sumaba en Java y se sobrescribía. Si dos
            // compras del mismo producto corrían casi al mismo tiempo,
            // ambas podían leer el mismo stockActual y la segunda
            // escritura pisaba a la primera (se perdía un incremento).
            // Ahora es relativo a nivel SQL ("stock = stock + ?"), así
            // que cada compra suma sobre el valor real que haya en ese
            // instante, sin importar el orden de ejecución.
            String sqlActualizarStock = "UPDATE producto " +
                    "SET stock = stock + ? WHERE id_producto = ?";

            PreparedStatement psDetalle = conexion.prepareStatement(sqlDetalle);
            PreparedStatement psLeer = conexion.prepareStatement(sqlLeerProducto);
            PreparedStatement psActualizar = conexion.prepareStatement(sqlActualizarStock);

            for (DetalleCompra detalle : detalles) {
                // Lee el stock actual solo para el nombre/máximo (aviso
                // informativo); ya no se usa para calcular el nuevo stock
                psLeer.setString(1, detalle.getProducto().getIdProducto());
                ResultSet rsProd = psLeer.executeQuery();

                int stockMaximo = 0;
                String nombreProd = "";
                if (rsProd.next()) {
                    stockMaximo = rsProd.getInt("stock_maximo");
                    nombreProd  = rsProd.getString("nombre");
                }
                rsProd.close();

                // Inserta la línea del detalle
                psDetalle.setString(1, detalle.getIdDetalleCompra());
                psDetalle.setString(2, compra.getIdCompra());
                psDetalle.setString(3, detalle.getProducto().getIdProducto());
                psDetalle.setInt(4, detalle.getCantidad());
                psDetalle.setDouble(5, detalle.getPrecio());
                psDetalle.executeUpdate();

                // Crea el lote de esta línea de compra: costo propio,
                // sin mezclarse con el costo de compras anteriores
                String idLote = "LOTE-" + detalle.getIdDetalleCompra();
                loteDAO.crearLote(idLote, detalle.getProducto().getIdProducto(),
                        detalle.getIdDetalleCompra(),
                        detalle.getCantidad(), detalle.getPrecio());

                // El stock total del producto sigue siendo la suma de
                // todos sus lotes; se mantiene como columna actualizada
                // en cada transacción para no recalcular con SUM() en
                // cada lectura (Venta, alertas de stock mínimo/máximo, etc.)
                psActualizar.setInt(1, detalle.getCantidad());
                psActualizar.setString(2, detalle.getProducto().getIdProducto());
                psActualizar.executeUpdate();

                // Vuelve a leer el stock ya actualizado (dentro de la
                // misma transacción) solo para el mensaje de aviso
                psLeer.setString(1, detalle.getProducto().getIdProducto());
                ResultSet rsProdActualizado = psLeer.executeQuery();
                int stockTotal = 0;
                if (rsProdActualizado.next()) {
                    stockTotal = rsProdActualizado.getInt("stock");
                }
                rsProdActualizado.close();

                // Verifica si el nuevo stock supera el máximo definido.
                if (stockMaximo > 0 && stockTotal > stockMaximo) {
                    avisos.add("📦 \"" + nombreProd + "\": " +
                            "stock actual " + stockTotal +
                            " supera el máximo definido (" + stockMaximo + ")");
                }
            }

            // todo salió bien, confirma todos los cambios
            conexion.commit();
            return true;

        } catch (SQLException e) {
            // Algo falló, deshace TODOS los cambios
            // como si nada hubiera ocurrido
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Error en rollback: " +
                        ex.getMessage());
            }
            System.out.println("Error al registrar compra: " +
                    e.getMessage());
            return false;
        } finally {
            // Siempre restaura el autocommit al terminar
            // para no afectar otras operaciones del sistema
            try { conexion.setAutoCommit(true); }
            catch (SQLException e) {
                System.out.println("Error al restaurar autocommit: " +
                        e.getMessage());
            }
        }
    }

    /**
     * Anula una compra ya registrada: anula el/los lotes que generó y
     * revierte el stock del producto, marcando la compra como ANULADA.
     * No borra ningún registro.
     *
     * Solo se permite anular si el/los lotes generados por esta compra
     * siguen completos (cantidad_restante == cantidad_original), es decir,
     * que no se haya vendido ya nada de ese lote específico. A diferencia
     * del esquema anterior con CPP, la validación ya no depende del stock
     * general del producto sino del lote puntual de esta compra.
     *
     * Flujo:
     * 1. Verifica que la compra exista y no esté ya anulada
     * 2. Trae el detalle de la compra
     * 3. Valida que cada lote generado siga intacto
     * 4. Revierte el stock del producto y anula los lotes
     * 5. Marca la compra como ANULADA
     * 6. COMMIT si todo salió bien, ROLLBACK si algo falló
     */
    // ===== Métodos de soporte para anular una compra =====
    // NOTA: al igual que en VentaDAO/LoteDAO, estos métodos NO manejan
    // su propia transacción (no hacen setAutoCommit/commit/rollback) y
    // lanzan SQLException hacia arriba. La orquestación de la anulación
    // vive en CompraService, que es quien abre y cierra la transacción.

    /** Estado actual de una compra, o null si no existe. */
    public String obtenerEstado(String idCompra) throws SQLException {
        String sql = "SELECT estado FROM compra WHERE id_compra = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idCompra);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String estado = rs.getString("estado");
            rs.close();
            return estado;
        }
        rs.close();
        return null;
    }

    /**
     * Cantidad restante y original de un lote generado por una línea
     * de compra. Se usa para validar que no se haya vendido nada de
     * ese lote antes de permitir anular la compra.
     */
    public int[] obtenerCantidadesLote(String idDetalleCompra) throws SQLException {
        String sql = "SELECT cantidad_restante, cantidad_original " +
                "FROM lote_compra WHERE id_detallecompra = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idDetalleCompra);
        ResultSet rs = ps.executeQuery();
        int[] resultado = null;
        if (rs.next()) {
            resultado = new int[] { rs.getInt("cantidad_restante"), rs.getInt("cantidad_original") };
        }
        rs.close();
        return resultado;
    }

    /** Resta cantidad al stock total (columna resumen) de un producto. */
    public void restarStockProducto(String idProducto, int cantidad) throws SQLException {
        String sql = "UPDATE producto SET stock = stock - ? WHERE id_producto = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setInt(1, cantidad);
        ps.setString(2, idProducto);
        ps.executeUpdate();
    }

    /** Marca una compra como ANULADA. No borra ningún registro. */
    public void marcarAnulada(String idCompra) throws SQLException {
        String sql = "UPDATE compra SET estado = 'ANULADA' WHERE id_compra = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idCompra);
        ps.executeUpdate();
    }

    /** Lista todas las compras con JOIN a proveedor y empleado para mostrar sus nombres en la tabla. */
    public List<Compra> listar() {
        List<Compra> lista = new ArrayList<>();
        String sql = "SELECT c.*, " +
                "p.nombre as nombre_proveedor, " +
                "e.nombre as nombre_empleado " +
                "FROM compra c " +
                "INNER JOIN proveedor p " +
                "ON c.id_proveedor = p.id_proveedor " +
                "INNER JOIN empleado e " +
                "ON c.id_empleado = e.id_empleado " +
                "ORDER BY c.fecha DESC";
        try {
            PreparedStatement ps =
                    conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearCompra(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar compras: " +
                    e.getMessage());
        }
        return lista;
    }

    /** Trae el detalle de una compra específica por su ID. Se usa para mostrar el detalle cuando el usuario
     * hace clic en Ver de una compra existente, y también para anular (ya no necesita costo_anterior).
     */
    public List<DetalleCompra> listarDetalle(String idCompra) {
        List<DetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT dc.*, " +
                "pr.nombre as nombre_producto " +
                "FROM detallecompra dc " +
                "INNER JOIN producto pr " +
                "ON dc.id_producto = pr.id_producto " +
                "WHERE dc.id_compra = ?";
        try {
            PreparedStatement ps =
                    conexion.prepareStatement(sql);
            ps.setString(1, idCompra);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetalleCompra d = new DetalleCompra();
                d.setIdDetalleCompra(
                        rs.getString("id_detallecompra"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecio(rs.getDouble("precio"));

                // Construye el producto solo con los datos
                // que necesitamos mostrar
                pe.utp.model.Producto prod =
                        new pe.utp.model.Producto();
                prod.setIdProducto(rs.getString("id_producto"));
                prod.setNombre(
                        rs.getString("nombre_producto"));
                d.setProducto(prod);
                lista.add(d);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar detalle: " +
                    e.getMessage());
        }
        return lista;
    }

    /**
     * Genera el siguiente ID correlativo para la compra.
     * Usa QueryHelper para generar sintaxis compatible con SQL Server.
     */
    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_compra FROM compra " +
                        "ORDER BY id_compra DESC", 1
        );
        try {
            PreparedStatement ps =
                    conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_compra");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " +
                    e.getMessage());
        }
        return null;
    }

    /** Verifica si ya existe una compra con ese número de comprobante para evitar duplicados.
     */
    public boolean existeComprobante(String numero) {
        String sql = "SELECT COUNT(*) FROM compra " +
                "WHERE numero_comprobante = ?";
        try {
            PreparedStatement ps =
                    conexion.prepareStatement(sql);
            ps.setString(1, numero);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Error al verificar comprobante: " +
                    e.getMessage());
        }
        return false;
    }

    private Compra mapearCompra(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setIdCompra(rs.getString("id_compra"));
        c.setNumeroComprobante(
                rs.getString("numero_comprobante"));
        c.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        c.setTotal(rs.getDouble("total"));

        // Construye el proveedor con los datos del JOIN
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getString("id_proveedor"));
        p.setNombre(rs.getString("nombre_proveedor"));
        c.setProveedor(p);

        // Construye el empleado con los datos del JOIN
        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre_empleado"));
        c.setEstado(rs.getString("estado"));
        c.setEmpleado(e);

        return c;
    }

    public String generarNumeroComprobante() {
        String sql = "SELECT numero_comprobante FROM compra " +
                "WHERE numero_comprobante LIKE 'F001-%'";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int maxCorrelativo = 0;

            while (rs.next()) {
                String num = rs.getString("numero_comprobante");
                try {
                    String[] partes = num.split("-");
                    int correlativo = Integer.parseInt(partes[1]);
                    if (correlativo > maxCorrelativo) {
                        maxCorrelativo = correlativo;
                    }
                } catch (Exception ignored) {}
            }
            return String.format("F001-%05d", maxCorrelativo + 1);

        } catch (SQLException e) {
            System.out.println("Error al generar comprobante: " +
                    e.getMessage());
        }
        return "F001-00001";
    }

}
