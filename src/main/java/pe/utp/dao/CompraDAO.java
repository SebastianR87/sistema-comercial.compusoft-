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

    /** Registra la compra completa en una sola transacción. Una transacción garantiza que si algo falla en el medio,
     ningún cambio queda guardado a medias.
     */
    public boolean registrarCompra(Compra compra,
                                   List<DetalleCompra> detalles) {
        try {
            // Desactiva el autocommit para manejar la transacción manualmente
            // Sin esto cada INSERT haría su propio commit automáticamente
            conexion.setAutoCommit(false);
            avisos.clear();

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

            // Paso 2 y 3: para cada producto en el detalle
            String sqlDetalle = "INSERT INTO detallecompra " +
                    "(id_detallecompra, id_compra, " +
                    "id_producto, cantidad, precio) " +
                    "VALUES (?,?,?,?,?)";

            // Antes de actualizar necesitamos leer stock y precio_compra
            // actuales del producto para calcular el Costo Promedio Ponderado
            String sqlLeerProducto = "SELECT stock, precio_compra, " +
                    "stock_maximo, nombre " +
                    "FROM producto WHERE id_producto = ?";

            String sqlActualizarProducto = "UPDATE producto " +
                    "SET stock = ?, precio_compra = ? " +
                    "WHERE id_producto = ?";

            PreparedStatement psDetalle =
                    conexion.prepareStatement(sqlDetalle);
            PreparedStatement psLeer =
                    conexion.prepareStatement(sqlLeerProducto);
            PreparedStatement psActualizar =
                    conexion.prepareStatement(sqlActualizarProducto);

            for (DetalleCompra detalle : detalles) {
                // Inserta la línea del detalle
                psDetalle.setString(1, detalle.getIdDetalleCompra());
                psDetalle.setString(2, compra.getIdCompra());
                psDetalle.setString(3, detalle.getProducto().getIdProducto());
                psDetalle.setInt(4, detalle.getCantidad());
                psDetalle.setDouble(5, detalle.getPrecio());
                psDetalle.executeUpdate();

                // Lee el stock y precio_compra actuales del producto
                psLeer.setString(1, detalle.getProducto().getIdProducto());
                ResultSet rsProd = psLeer.executeQuery();

                int stockActual   = 0;
                double precioActual = 0;
                int stockMaximo   = 0;
                String nombreProd = "";
                if (rsProd.next()) {
                    stockActual   = rsProd.getInt("stock");
                    precioActual  = rsProd.getDouble("precio_compra");
                    stockMaximo   = rsProd.getInt("stock_maximo");
                    nombreProd    = rsProd.getString("nombre");
                }
                rsProd.close();

                int cantidadNueva  = detalle.getCantidad();
                double precioNuevo = detalle.getPrecio();
                int stockTotal     = stockActual + cantidadNueva;

                // Costo Promedio Ponderado (CPP):
                // promedia el costo de lo que ya tenías con lo que
                // compraste ahora, pesando cada precio por su cantidad
                double nuevoCosto = stockTotal == 0
                        ? precioNuevo
                        : ((stockActual * precioActual) +
                           (cantidadNueva * precioNuevo)) / stockTotal;

                // Actualiza stock total y nuevo costo promedio
                psActualizar.setInt(1, stockTotal);
                psActualizar.setDouble(2, nuevoCosto);
                psActualizar.setString(3, detalle.getProducto().getIdProducto());
                psActualizar.executeUpdate();

                // Verifica si el nuevo stock supera el máximo definido.
                // stockMaximo = 0 significa que no se definió un límite,
                // así que solo alerta si hay un máximo definido (> 0).
                // La compra NO se cancela, solo se registra el aviso.
                if (stockMaximo > 0 && stockTotal > stockMaximo) {
                    avisos.add("📦 \"" + nombreProd + "\": " +
                            "stock actual " + stockTotal +
                            " supera el máximo definido (" + stockMaximo + ")");
                }
            }

            // todoo salió bien, confirma todos los cambios
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
     * hace clic en Ver de una compra existente.
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
     * Usa QueryHelper para compatibilidad MySQL/SQL Server.
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