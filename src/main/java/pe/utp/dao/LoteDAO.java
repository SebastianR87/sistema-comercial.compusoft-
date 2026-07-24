package pe.utp.dao;

import pe.utp.model.Lote;
import pe.utp.model.Producto;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja los lotes de compra (costeo FIFO). Reemplaza el cálculo
 * de Costo Promedio Ponderado (CPP) que antes vivía en producto.precio_compra.
 * IMPORTANTE: los métodos que escriben (crearLote, descontarLote,
 * devolverALote, anularLotesDeCompra) NO manejan su propia transacción
 * (no hacen setAutoCommit/commit/rollback) y lanzan SQLException hacia
 * arriba. Siempre se llaman desde dentro de la transacción que ya abrió
 * CompraDAO o VentaDAO, para que todo el conjunto de cambios (cabecera +
 * detalle + lotes + stock) se confirme o se revierta como una sola unidad.
 */
public class LoteDAO {
    private Connection conexion;

    public LoteDAO(Connection conexion) {
        this.conexion = conexion;
    }

    /** Crea un lote nuevo a partir de una línea de compra. */
    public void crearLote(String idLote, String idProducto, String idDetalleCompra,
                           int cantidad, double costoUnitario) throws SQLException {
        String sql = "INSERT INTO lote_compra " +
                "(id_lote, id_producto, id_detallecompra, cantidad_original, " +
                "cantidad_restante, costo_unitario, fecha, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVO')";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idLote);
        ps.setString(2, idProducto);
        ps.setString(3, idDetalleCompra);
        ps.setInt(4, cantidad);
        ps.setInt(5, cantidad); // al crear, restante = original
        ps.setDouble(6, costoUnitario);
        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
        ps.executeUpdate();
    }

    /**
     * Lista los lotes ACTIVOS de un producto con stock disponible,
     * ordenados del más antiguo al más reciente (orden de consumo FIFO).
     * La usa VentaDAO para saber de dónde descontar, y el detalle de
     * producto para mostrar los lotes en pantalla.
     */
    public List<Lote> listarLotesDisponibles(String idProducto) {
        List<Lote> lista = new ArrayList<>();
        String sql = "SELECT * FROM lote_compra " +
                "WHERE id_producto = ? AND estado = 'ACTIVO' AND cantidad_restante > 0 " +
                "ORDER BY fecha ASC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearLote(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar lotes disponibles: " + e.getMessage());
        }
        return lista;
    }

    /** Todos los lotes de un producto (activos y anulados), para historial completo. */
    public List<Lote> listarTodosPorProducto(String idProducto) {
        List<Lote> lista = new ArrayList<>();
        String sql = "SELECT * FROM lote_compra WHERE id_producto = ? ORDER BY fecha ASC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearLote(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar lotes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Descuenta cantidad de un lote específico (consumo por venta).
     * No valida que alcance la cantidad -- esa validación la hace quien
     * orquesta el consumo FIFO (VentaDAO) antes de llamar a este método.
     */
    public void descontarLote(String idLote, int cantidad) throws SQLException {
        String sql = "UPDATE lote_compra " +
                "SET cantidad_restante = cantidad_restante - ? WHERE id_lote = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setInt(1, cantidad);
        ps.setString(2, idLote);
        ps.executeUpdate();
    }

    /** Devuelve cantidad a un lote específico (usado al anular una venta). */
    public void devolverALote(String idLote, int cantidad) throws SQLException {
        String sql = "UPDATE lote_compra " +
                "SET cantidad_restante = cantidad_restante + ? WHERE id_lote = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setInt(1, cantidad);
        ps.setString(2, idLote);
        ps.executeUpdate();
    }

    /**
     * Lista los lotes (id_lote, cantidad) que fueron consumidos por
     * una línea de venta específica, según detalleventa_lote. Se usa
     * al anular una venta para saber a qué lote(s) devolver stock.
     */
    public List<Consumo> listarLotesConsumidos(String idDetalleVenta) throws SQLException {
        List<Consumo> consumos = new ArrayList<>();
        String sql = "SELECT id_lote, cantidad FROM detalleventa_lote WHERE id_detalleventa = ?";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idDetalleVenta);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            consumos.add(new Consumo(rs.getString("id_lote"), rs.getInt("cantidad"), 0));
        }
        rs.close();
        return consumos;
    }

    /** Anula todos los lotes creados por una compra específica. */
    public void anularLotesDeCompra(String idCompra) throws SQLException {
        String sql = "UPDATE lote_compra SET estado = 'ANULADO' " +
                "WHERE id_detallecompra IN " +
                "(SELECT id_detallecompra FROM detallecompra WHERE id_compra = ?)";
        PreparedStatement ps = conexion.prepareStatement(sql);
        ps.setString(1, idCompra);
        ps.executeUpdate();
    }

    /**
     * Consume una cantidad de un producto siguiendo orden FIFO:
     * descuenta primero del lote más antiguo, y si no alcanza,
     * continúa con el siguiente, hasta cubrir la cantidad pedida.
     *
     * Usado por VentaDAO.registrarVenta() en vez de un simple
     * "UPDATE stock = stock - cantidad": así cada venta queda
     * asociada al costo REAL del lote de donde salió, sin promediar.
     *
     * @throws SQLException si el stock total en lotes activos no
     *         alcanza para cubrir la cantidad pedida (no debería
     *         pasar si quien llama ya validó contra producto.stock,
     *         pero se revisa igual por seguridad).
     */
    public List<Consumo> consumirFIFO(String idProducto, int cantidadRequerida) throws SQLException {
        List<Consumo> consumos = new ArrayList<>();
        List<Lote> disponibles = listarLotesDisponibles(idProducto);

        int pendiente = cantidadRequerida;
        for (Lote lote : disponibles) {
            if (pendiente <= 0) break;

            int aTomar = Math.min(pendiente, lote.getCantidadRestante());
            descontarLote(lote.getIdLote(), aTomar);
            consumos.add(new Consumo(lote.getIdLote(), aTomar, lote.getCostoUnitario()));
            pendiente -= aTomar;
        }

        if (pendiente > 0) {
            throw new SQLException("No hay lotes suficientes para cubrir la cantidad solicitada " +
                    "del producto " + idProducto + " (faltan " + pendiente + " unidades)");
        }

        return consumos;
    }

    /** Representa cuánto se tomó de un lote específico durante un consumo FIFO. */
    public static class Consumo {
        private final String idLote;
        private final int cantidad;
        private final double costoUnitario;

        public Consumo(String idLote, int cantidad, double costoUnitario) {
            this.idLote = idLote;
            this.cantidad = cantidad;
            this.costoUnitario = costoUnitario;
        }

        public String getIdLote() { return idLote; }
        public int getCantidad() { return cantidad; }
        public double getCostoUnitario() { return costoUnitario; }
    }

    /**
     * Costo promedio ponderado de los lotes ACTIVOS con stock
     * restante de un producto. Es un valor de REFERENCIA para
     * mostrar en pantalla (tabla de Producto, reportes) -- no se
     * usa para el consumo FIFO real, que sigue el costo exacto
     * de cada lote individual.
     */
    public double obtenerCostoPromedio(String idProducto) {
        String sql = "SELECT SUM(cantidad_restante * costo_unitario) AS valorTotal, " +
                "SUM(cantidad_restante) AS cantidadTotal " +
                "FROM lote_compra " +
                "WHERE id_producto = ? AND estado = 'ACTIVO' AND cantidad_restante > 0";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double cantidadTotal = rs.getDouble("cantidadTotal");
                if (cantidadTotal > 0) {
                    return rs.getDouble("valorTotal") / cantidadTotal;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al calcular costo promedio: " + e.getMessage());
        }
        return 0;
    }

    private Lote mapearLote(ResultSet rs) throws SQLException {
        Lote l = new Lote();
        l.setIdLote(rs.getString("id_lote"));
        l.setIdDetalleCompra(rs.getString("id_detallecompra"));
        l.setCantidadOriginal(rs.getInt("cantidad_original"));
        l.setCantidadRestante(rs.getInt("cantidad_restante"));
        l.setCostoUnitario(rs.getDouble("costo_unitario"));
        l.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        l.setEstado(rs.getString("estado"));

        Producto p = new Producto();
        p.setIdProducto(rs.getString("id_producto"));
        l.setProducto(p);

        return l;
    }
}
