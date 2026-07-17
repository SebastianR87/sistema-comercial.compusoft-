package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas para el módulo de Reportes: resúmenes y detalle de
 * Ventas y Compras por rango de fechas. Es una mirada "de negocio"
 * (totales, tendencias, top productos) -- distinta del Kárdex, que
 * es una mirada por producto individual con costeo FIFO.
 */
public class ReporteDAO {

    private Connection conexion;

    public ReporteDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    // MODELOS SIMPLES
    public static class Resumen {
        public double total;
        public int cantidadOperaciones;
        public double promedio;
    }

    public static class PuntoTendencia {
        public String fecha;   // dd/MM
        public double total;
    }

    public static class ProductoTop {
        public String nombre;
        public int cantidad;
        public double total;
    }

    public static class FilaDetalleVenta {
        public String fecha;
        public String comprobante;
        public String cliente;
        public String producto;
        public int cantidad;
        public double precio;
        public double subtotal;
    }

    public static class FilaDetalleCompra {
        public String fecha;
        public String comprobante;
        public String proveedor;
        public String producto;
        public int cantidad;
        public double precio;
        public double subtotal;
    }

    // VENTAS
    public Resumen obtenerResumenVentas(LocalDate desde, LocalDate hasta) {
        Resumen r = new Resumen();
        String sql = "SELECT COALESCE(SUM(total),0) AS total, COUNT(*) AS cant " +
                "FROM venta WHERE fecha BETWEEN ? AND ? " +
                "AND (estado IS NULL OR estado <> 'ANULADA')";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                r.total = rs.getDouble("total");
                r.cantidadOperaciones = rs.getInt("cant");
                r.promedio = r.cantidadOperaciones > 0 ? r.total / r.cantidadOperaciones : 0;
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener resumen de ventas: " + e.getMessage());
        }
        return r;
    }

    public List<PuntoTendencia> obtenerVentasPorDia(LocalDate desde, LocalDate hasta) {
        List<PuntoTendencia> lista = new ArrayList<>();
        String dia = QueryHelper.fechaSolo("fecha");
        String sql = "SELECT " + dia + " AS dia, SUM(total) AS total " +
                "FROM venta WHERE fecha BETWEEN ? AND ? " +
                "AND (estado IS NULL OR estado <> 'ANULADA') " +
                "GROUP BY " + dia + " ORDER BY " + dia;
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PuntoTendencia p = new PuntoTendencia();
                p.fecha = rs.getDate("dia").toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                p.total = rs.getDouble("total");
                lista.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener tendencia de ventas: " + e.getMessage());
        }
        return lista;
    }

    public List<ProductoTop> obtenerTopProductosVendidos(LocalDate desde, LocalDate hasta, int limite) {
        List<ProductoTop> lista = new ArrayList<>();
        String sql = QueryHelper.limitar(
                "SELECT p.nombre AS nombre, SUM(dv.cantidad) AS cant, " +
                        "SUM(dv.cantidad * dv.precio) AS total " +
                        "FROM detalleventa dv " +
                        "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                        "INNER JOIN producto p ON dv.id_producto = p.id_producto " +
                        "WHERE v.fecha BETWEEN ? AND ? " +
                        "AND (v.estado IS NULL OR v.estado <> 'ANULADA') " +
                        "GROUP BY p.nombre ORDER BY cant DESC", limite
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ProductoTop pt = new ProductoTop();
                pt.nombre = rs.getString("nombre");
                pt.cantidad = rs.getInt("cant");
                pt.total = rs.getDouble("total");
                lista.add(pt);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener top productos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Costo REAL de las unidades vendidas en el periodo, según el
     * costo exacto del lote FIFO consumido en cada venta (no un
     * promedio) -- viene de detalleventa_lote.costo_unitario_momento.
     * Se usa para calcular la Utilidad Bruta real del periodo.
     */
    public double obtenerCostoRealVentas(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(dvl.cantidad * dvl.costo_unitario_momento),0) AS costo " +
                "FROM detalleventa_lote dvl " +
                "INNER JOIN detalleventa dv ON dvl.id_detalleventa = dv.id_detalleventa " +
                "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                "WHERE v.fecha BETWEEN ? AND ? " +
                "AND (v.estado IS NULL OR v.estado <> 'ANULADA')";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("costo");
        } catch (SQLException e) {
            System.out.println("Error al calcular costo real de ventas: " + e.getMessage());
        }
        return 0;
    }

    public static class CategoriaTotal {
        public String nombre;
        public double total;
    }

    public List<CategoriaTotal> obtenerVentasPorCategoria(LocalDate desde, LocalDate hasta) {
        List<CategoriaTotal> lista = new ArrayList<>();
        String sql = "SELECT cat.nombre AS categoria, SUM(dv.cantidad * dv.precio) AS total " +
                "FROM detalleventa dv " +
                "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                "INNER JOIN producto p ON dv.id_producto = p.id_producto " +
                "INNER JOIN categoria cat ON p.id_categoria = cat.id_categoria " +
                "WHERE v.fecha BETWEEN ? AND ? " +
                "AND (v.estado IS NULL OR v.estado <> 'ANULADA') " +
                "GROUP BY cat.nombre ORDER BY total DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CategoriaTotal c = new CategoriaTotal();
                c.nombre = rs.getString("categoria");
                c.total = rs.getDouble("total");
                lista.add(c);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener ventas por categoría: " + e.getMessage());
        }
        return lista;
    }

    public List<CategoriaTotal> obtenerComprasPorCategoria(LocalDate desde, LocalDate hasta) {
        List<CategoriaTotal> lista = new ArrayList<>();
        String sql = "SELECT cat.nombre AS categoria, SUM(dc.cantidad * dc.precio) AS total " +
                "FROM detallecompra dc " +
                "INNER JOIN compra c ON dc.id_compra = c.id_compra " +
                "INNER JOIN producto p ON dc.id_producto = p.id_producto " +
                "INNER JOIN categoria cat ON p.id_categoria = cat.id_categoria " +
                "WHERE c.fecha BETWEEN ? AND ? " +
                "AND (c.estado IS NULL OR c.estado <> 'ANULADA') " +
                "GROUP BY cat.nombre ORDER BY total DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CategoriaTotal c = new CategoriaTotal();
                c.nombre = rs.getString("categoria");
                c.total = rs.getDouble("total");
                lista.add(c);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener compras por categoría: " + e.getMessage());
        }
        return lista;
    }

    public List<FilaDetalleVenta> listarDetalleVentas(LocalDate desde, LocalDate hasta) {
        List<FilaDetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT v.fecha, v.numero_comprobante, c.nombre AS cliente, " +
                "p.nombre AS producto, dv.cantidad, dv.precio " +
                "FROM detalleventa dv " +
                "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                "INNER JOIN cliente c ON v.id_cliente = c.id_cliente " +
                "INNER JOIN producto p ON dv.id_producto = p.id_producto " +
                "WHERE v.fecha BETWEEN ? AND ? " +
                "AND (v.estado IS NULL OR v.estado <> 'ANULADA') " +
                "ORDER BY v.fecha";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FilaDetalleVenta f = new FilaDetalleVenta();
                f.fecha = rs.getTimestamp("fecha").toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                f.comprobante = rs.getString("numero_comprobante");
                f.cliente = rs.getString("cliente");
                f.producto = rs.getString("producto");
                f.cantidad = rs.getInt("cantidad");
                f.precio = rs.getDouble("precio");
                f.subtotal = f.cantidad * f.precio;
                lista.add(f);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar detalle de ventas: " + e.getMessage());
        }
        return lista;
    }

    //COMPRAS
    public Resumen obtenerResumenCompras(LocalDate desde, LocalDate hasta) {
        Resumen r = new Resumen();
        String sql = "SELECT COALESCE(SUM(total),0) AS total, COUNT(*) AS cant " +
                "FROM compra WHERE fecha BETWEEN ? AND ? " +
                "AND (estado IS NULL OR estado <> 'ANULADA')";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                r.total = rs.getDouble("total");
                r.cantidadOperaciones = rs.getInt("cant");
                r.promedio = r.cantidadOperaciones > 0 ? r.total / r.cantidadOperaciones : 0;
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener resumen de compras: " + e.getMessage());
        }
        return r;
    }

    public List<PuntoTendencia> obtenerComprasPorDia(LocalDate desde, LocalDate hasta) {
        List<PuntoTendencia> lista = new ArrayList<>();
        String dia = QueryHelper.fechaSolo("fecha");
        String sql = "SELECT " + dia + " AS dia, SUM(total) AS total " +
                "FROM compra WHERE fecha BETWEEN ? AND ? " +
                "AND (estado IS NULL OR estado <> 'ANULADA') " +
                "GROUP BY " + dia + " ORDER BY " + dia;
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PuntoTendencia p = new PuntoTendencia();
                p.fecha = rs.getDate("dia").toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                p.total = rs.getDouble("total");
                lista.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener tendencia de compras: " + e.getMessage());
        }
        return lista;
    }

    public List<ProductoTop> obtenerTopProductosComprados(LocalDate desde, LocalDate hasta, int limite) {
        List<ProductoTop> lista = new ArrayList<>();
        String sql = QueryHelper.limitar(
                "SELECT p.nombre AS nombre, SUM(dc.cantidad) AS cant, " +
                        "SUM(dc.cantidad * dc.precio) AS total " +
                        "FROM detallecompra dc " +
                        "INNER JOIN compra c ON dc.id_compra = c.id_compra " +
                        "INNER JOIN producto p ON dc.id_producto = p.id_producto " +
                        "WHERE c.fecha BETWEEN ? AND ? " +
                        "AND (c.estado IS NULL OR c.estado <> 'ANULADA') " +
                        "GROUP BY p.nombre ORDER BY cant DESC", limite
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ProductoTop pt = new ProductoTop();
                pt.nombre = rs.getString("nombre");
                pt.cantidad = rs.getInt("cant");
                pt.total = rs.getDouble("total");
                lista.add(pt);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener top productos comprados: " + e.getMessage());
        }
        return lista;
    }

    public List<FilaDetalleCompra> listarDetalleCompras(LocalDate desde, LocalDate hasta) {
        List<FilaDetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT c.fecha, c.numero_comprobante, pr.nombre AS proveedor, " +
                "p.nombre AS producto, dc.cantidad, dc.precio " +
                "FROM detallecompra dc " +
                "INNER JOIN compra c ON dc.id_compra = c.id_compra " +
                "INNER JOIN proveedor pr ON c.id_proveedor = pr.id_proveedor " +
                "INNER JOIN producto p ON dc.id_producto = p.id_producto " +
                "WHERE c.fecha BETWEEN ? AND ? " +
                "AND (c.estado IS NULL OR c.estado <> 'ANULADA') " +
                "ORDER BY c.fecha";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(23, 59, 59)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FilaDetalleCompra f = new FilaDetalleCompra();
                f.fecha = rs.getTimestamp("fecha").toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                f.comprobante = rs.getString("numero_comprobante");
                f.proveedor = rs.getString("proveedor");
                f.producto = rs.getString("producto");
                f.cantidad = rs.getInt("cantidad");
                f.precio = rs.getDouble("precio");
                f.subtotal = f.cantidad * f.precio;
                lista.add(f);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar detalle de compras: " + e.getMessage());
        }
        return lista;
    }
}
