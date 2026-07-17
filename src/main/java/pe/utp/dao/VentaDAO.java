package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
import pe.utp.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    private Connection conexion;

    public VentaDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    /**
     * Registra la venta completa en una sola transacción.
     * A diferencia de Compra, aquí CONSUMIMOS stock: se descuenta
     * siguiendo orden FIFO (lote más antiguo primero) en vez de
     * restar directamente sobre producto.stock, para que el costo
     * de cada venta quede asociado al costo real del lote de origen
     * y no a un promedio (ver LoteDAO.consumirFIFO).
     *
     * Flujo:
     * 1. Inserta la cabecera en tabla venta
     * 2. Inserta cada línea en detalleventa, descuenta el stock de
     *    forma atómica/condicional (el propio UPDATE actúa como
     *    chequeo de stock suficiente), consume lotes FIFO y registra
     *    en detalleventa_lote de dónde salió cada cantidad
     * 3. COMMIT si todo salió bien, ROLLBACK si algo falló
     *
     * Antes había un "Paso 1" que verificaba el stock disponible con
     * un SELECT separado, ANTES de insertar nada, y solo más abajo
     * (Paso 4 original) restaba el stock sin condición. Ese chequeo
     * separado del descuento real dejaba una ventana: si dos ventas
     * del mismo producto se registraban casi al mismo tiempo, ambas
     * podían leer el mismo stock disponible en el SELECT, pasar la
     * validación, y luego ambas restar -- vendiendo más unidades de
     * las que realmente había (stock quedaba negativo). Ahora el
     * chequeo y el descuento son la MISMA operación SQL
     * ("UPDATE ... WHERE stock >= ?"), así que no hay ventana entre
     * verificar y actuar.
     */
    public String registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        try {
            conexion.setAutoCommit(false);

            // Paso 1: inserta la cabecera de la venta
            String sqlVenta = "INSERT INTO venta " +
                    "(id_venta, id_cliente, id_empleado, id_tipo_comprobante, " +
                    "id_metodopago, numero_comprobante, fecha, descuento, total, " +
                    "monto_pagado, vuelto) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement psVenta = conexion.prepareStatement(sqlVenta);
            psVenta.setString(1, venta.getIdVenta());
            psVenta.setString(2, venta.getCliente().getIdCliente());
            psVenta.setString(3, venta.getEmpleado().getIdEmpleado());
            psVenta.setString(4, venta.getTipoComprobante().getIdTipoComprobante());
            psVenta.setString(5, venta.getMetodoPago().getIdMetodoPago());
            psVenta.setString(6, venta.getNumeroComprobante());
            psVenta.setTimestamp(7, Timestamp.valueOf(venta.getFecha()));
            psVenta.setDouble(8, venta.getDescuento());
            psVenta.setDouble(9, venta.getTotal());
            psVenta.setDouble(10, venta.getMontoPagado());
            psVenta.setDouble(11, venta.getVuelto());
            psVenta.executeUpdate();

            // Paso 2: inserta detalle, descuenta stock de forma
            // atómica/condicional, consume lotes FIFO, y registra de
            // qué lote(s) salió cada línea
            String sqlDetalle = "INSERT INTO detalleventa " +
                    "(id_detalleventa, id_venta, id_producto, cantidad, precio) " +
                    "VALUES (?,?,?,?,?)";
            // El "AND stock >= ?" es el chequeo de stock suficiente:
            // si no hay filas afectadas, es porque no había stock (ver
            // comentario en el javadoc de este método)
            String sqlRestarStock = "UPDATE producto " +
                    "SET stock = stock - ? WHERE id_producto = ? AND stock >= ?";
            String sqlStockActual = "SELECT stock FROM producto WHERE id_producto = ?";
            String sqlDetalleLote = "INSERT INTO detalleventa_lote " +
                    "(id_detalleventa_lote, id_detalleventa, id_lote, cantidad, costo_unitario_momento) " +
                    "VALUES (?,?,?,?,?)";

            PreparedStatement psDetalle = conexion.prepareStatement(sqlDetalle);
            PreparedStatement psRestar = conexion.prepareStatement(sqlRestarStock);
            PreparedStatement psStockActual = conexion.prepareStatement(sqlStockActual);
            PreparedStatement psDetalleLote = conexion.prepareStatement(sqlDetalleLote);
            LoteDAO loteDAO = new LoteDAO(conexion);

            for (DetalleVenta d : detalles) {
                psDetalle.setString(1, d.getIdDetalleVenta());
                psDetalle.setString(2, venta.getIdVenta());
                psDetalle.setString(3, d.getProducto().getIdProducto());
                psDetalle.setInt(4, d.getCantidad());
                psDetalle.setDouble(5, d.getPrecio());
                psDetalle.executeUpdate();

                // El stock total del producto se mantiene como columna
                // resumen, igual que en Compra -- se sigue actualizando
                // aquí aunque el detalle real viva ahora en los lotes.
                // La condición "stock >= cantidad" hace que este UPDATE
                // sea el chequeo Y el descuento en una sola operación
                // atómica (ver javadoc del método).
                psRestar.setInt(1, d.getCantidad());
                psRestar.setString(2, d.getProducto().getIdProducto());
                psRestar.setInt(3, d.getCantidad());
                int filasAfectadas = psRestar.executeUpdate();

                if (filasAfectadas == 0) {
                    conexion.rollback();
                    conexion.setAutoCommit(true);
                    // No hubo stock suficiente. Se consulta aparte
                    // solo para reportar cuánto había disponible --
                    // el formato "STOCK_INSUFICIENTE:nombre:stock" se
                    // mantiene igual para no romper a quienes llaman
                    // este método (CompraController/VentaController
                    // hacen .split(":") sobre este mismo formato).
                    psStockActual.setString(1, d.getProducto().getIdProducto());
                    ResultSet rsStock = psStockActual.executeQuery();
                    int stockDisponible = 0;
                    if (rsStock.next()) stockDisponible = rsStock.getInt("stock");
                    rsStock.close();
                    return "STOCK_INSUFICIENTE:" + d.getProducto().getNombre() +
                            ":" + stockDisponible;
                }

                // Consume del/los lote(s) más antiguos primero (FIFO).
                // Puede devolver varias filas si esta línea cruza
                // más de un lote (ej: pide 12, el lote más viejo solo
                // tenía 10 -- ahí toma 10 de uno y 2 del siguiente).
                List<LoteDAO.Consumo> consumos =
                        loteDAO.consumirFIFO(d.getProducto().getIdProducto(), d.getCantidad());

                int contador = 1;
                for (LoteDAO.Consumo c : consumos) {
                    psDetalleLote.setString(1, "DVL-" + d.getIdDetalleVenta() + "-" + contador);
                    psDetalleLote.setString(2, d.getIdDetalleVenta());
                    psDetalleLote.setString(3, c.getIdLote());
                    psDetalleLote.setInt(4, c.getCantidad());
                    psDetalleLote.setDouble(5, c.getCostoUnitario());
                    psDetalleLote.executeUpdate();
                    contador++;
                }
            }

            conexion.commit();
            return "OK";

        } catch (SQLException e) {
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Error en rollback: " + ex.getMessage());
            }
            System.out.println("Error al registrar venta: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        } finally {
            try { conexion.setAutoCommit(true); }
            catch (SQLException e) {
                System.out.println("Error al restaurar autocommit: " + e.getMessage());
            }
        }
    }

    /**
     * Anula una venta ya registrada: devuelve cada cantidad vendida
     * al lote EXACTO del que salió (usando detalleventa_lote), y
     * marca la venta como ANULADA. No borra ningún registro.
     *
     * Flujo:
     * 1. Verifica que la venta exista y no esté ya anulada
     * 2. Trae el detalle de la venta
     * 3. Por cada línea, devuelve la cantidad a su(s) lote(s) de
     *    origen y actualiza el stock total del producto
     * 4. Actualiza el estado de la venta a 'ANULADA'
     * 5. COMMIT si todo salió bien, ROLLBACK si algo falló
     */
    public String anularVenta(String idVenta) {
        try {
            conexion.setAutoCommit(false);

            // Paso 1: verifica estado actual de la venta
            String sqlEstado = "SELECT estado FROM venta WHERE id_venta = ?";
            PreparedStatement psEstado = conexion.prepareStatement(sqlEstado);
            psEstado.setString(1, idVenta);
            ResultSet rsEstado = psEstado.executeQuery();

            if (!rsEstado.next()) {
                conexion.rollback();
                conexion.setAutoCommit(true);
                return "NO_EXISTE";
            }
            String estadoActual = rsEstado.getString("estado");
            rsEstado.close();

            if ("ANULADA".equalsIgnoreCase(estadoActual)) {
                conexion.rollback();
                conexion.setAutoCommit(true);
                return "YA_ANULADA";
            }

            // Paso 2: trae el detalle para saber qué stock revertir
            List<DetalleVenta> detalles = listarDetalle(idVenta);

            // Paso 3: devuelve cada cantidad EXACTAMENTE al lote del
            // que salió (consultando detalleventa_lote), en vez de
            // sumar el stock del producto de forma genérica. Así cada
            // lote recupera su costo real sin mezclarse con otros.
            LoteDAO loteDAO = new LoteDAO(conexion);
            String sqlLotesConsumidos = "SELECT id_lote, cantidad " +
                    "FROM detalleventa_lote WHERE id_detalleventa = ?";
            PreparedStatement psLotesConsumidos = conexion.prepareStatement(sqlLotesConsumidos);

            String sqlDevolverStock = "UPDATE producto " +
                    "SET stock = stock + ? WHERE id_producto = ?";
            PreparedStatement psDevolver = conexion.prepareStatement(sqlDevolverStock);

            for (DetalleVenta d : detalles) {
                psLotesConsumidos.setString(1, d.getIdDetalleVenta());
                ResultSet rsLotes = psLotesConsumidos.executeQuery();
                while (rsLotes.next()) {
                    loteDAO.devolverALote(
                            rsLotes.getString("id_lote"),
                            rsLotes.getInt("cantidad"));
                }
                rsLotes.close();

                // El stock total del producto (columna resumen) se
                // sigue actualizando igual que antes
                psDevolver.setInt(1, d.getCantidad());
                psDevolver.setString(2, d.getProducto().getIdProducto());
                psDevolver.executeUpdate();
            }

            // Paso 4: marca la venta como anulada
            String sqlAnular = "UPDATE venta SET estado = 'ANULADA' WHERE id_venta = ?";
            PreparedStatement psAnular = conexion.prepareStatement(sqlAnular);
            psAnular.setString(1, idVenta);
            psAnular.executeUpdate();

            conexion.commit();
            return "OK";

        } catch (SQLException e) {
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Error en rollback: " + ex.getMessage());
            }
            System.out.println("Error al anular venta: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        } finally {
            try { conexion.setAutoCommit(true); }
            catch (SQLException e) {
                System.out.println("Error al restaurar autocommit: " + e.getMessage());
            }
        }
    }


    /**
     * Lista todas las ventas con JOIN a cliente, empleado,
     * tipo comprobante y método de pago.
     */
    public List<Venta> listar() {
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT v.*, " +
                "c.nombre as nombre_cliente, " +
                "c.numero_documento as documento_cliente, " +
                "e.nombre as nombre_empleado, " +
                "tc.nombre as nombre_tipo_comprobante, " +
                "mp.metodo_de_pago as nombre_metodo_pago " +
                "FROM venta v " +
                "INNER JOIN cliente c ON v.id_cliente = c.id_cliente " +
                "INNER JOIN empleado e ON v.id_empleado = e.id_empleado " +
                "INNER JOIN TipoComprobante tc ON v.id_tipo_comprobante = tc.id_tipo_comprobante " +
                "INNER JOIN metodopago mp ON v.id_metodopago = mp.id_metodopago " +
                "ORDER BY v.fecha DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearVenta(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar ventas: " + e.getMessage());
        }
        return lista;
    }

    /** Trae el detalle de una venta específica por su ID. */
    public List<DetalleVenta> listarDetalle(String idVenta) {
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT dv.*, p.nombre as nombre_producto " +
                "FROM detalleventa dv " +
                "INNER JOIN producto p ON dv.id_producto = p.id_producto " +
                "WHERE dv.id_venta = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetalleVenta d = new DetalleVenta();
                d.setIdDetalleVenta(rs.getString("id_detalleventa"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecio(rs.getDouble("precio"));

                Producto prod = new Producto();
                prod.setIdProducto(rs.getString("id_producto"));
                prod.setNombre(rs.getString("nombre_producto"));
                d.setProducto(prod);
                lista.add(d);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar detalle venta: " + e.getMessage());
        }
        return lista;
    }

    /** Genera el siguiente ID correlativo para la venta. */
    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_venta FROM venta ORDER BY id_venta DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_venta");
        } catch (SQLException e) {
            System.out.println("Error al obtener ultimo id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Genera el número de comprobante según el tipo (Boleta=B001, Factura=F001).
     * idTipoComprobante: "TC001" (Boleta) o "TC002" (Factura) según tu data inicial.
     */
    public String generarNumeroComprobante(String prefijo) {
        String sql = QueryHelper.limitar(
                "SELECT numero_comprobante FROM venta " +
                        "WHERE numero_comprobante LIKE '" + prefijo + "-%' " +
                        "ORDER BY numero_comprobante DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String ultimo = rs.getString("numero_comprobante");
                String[] partes = ultimo.split("-");
                int correlativo = Integer.parseInt(partes[1]) + 1;
                return String.format("%s-%05d", prefijo, correlativo);
            }
        } catch (SQLException e) {
            System.out.println("Error al generar comprobante: " + e.getMessage());
        }
        return prefijo + "-00001";
    }

    private Venta mapearVenta(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getString("id_venta"));
        v.setNumeroComprobante(rs.getString("numero_comprobante"));
        v.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        v.setDescuento(rs.getDouble("descuento"));
        v.setTotal(rs.getDouble("total"));

        Cliente c = new Cliente();
        c.setIdCliente(rs.getString("id_cliente"));
        c.setNombre(rs.getString("nombre_cliente"));
        c.setNumeroDocumento(rs.getString("documento_cliente"));
        v.setCliente(c);

        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre_empleado"));
        v.setEmpleado(e);

        TipoComprobante tc = new TipoComprobante();
        tc.setIdTipoComprobante(rs.getString("id_tipo_comprobante"));
        tc.setNombre(rs.getString("nombre_tipo_comprobante"));
        v.setTipoComprobante(tc);

        MetodoPago mp = new MetodoPago();
        mp.setIdMetodoPago(rs.getString("id_metodopago"));
        mp.setMetodoDePago(rs.getString("nombre_metodo_pago"));
        v.setMetodoPago(mp);

        v.setMontoPagado(rs.getDouble("monto_pagado"));
        v.setVuelto(rs.getDouble("vuelto"));
        v.setEstado(rs.getString("estado"));

        return v;
    }
}
