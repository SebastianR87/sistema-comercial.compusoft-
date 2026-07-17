package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.Conexion.QueryHelper;
import pe.utp.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CotizacionDAO {

    private Connection conexion;

    public CotizacionDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    /**
     * Registra la cotización y su detalle en una sola transacción.
     * NO afecta el stock porque es solo una proforma.
     */
    public boolean registrarCotizacion(Cotizacion cot,
                                       List<DetalleCotizacion> detalles) {
        try {
            conexion.setAutoCommit(false);

            String sqlCot = "INSERT INTO cotizacion " +
                    "(id_cotizacion, id_cliente, id_empleado, " +
                    "fecha, descuento, total, estado) " +
                    "VALUES (?,?,?,?,?,?,?)";
            PreparedStatement psCot = conexion.prepareStatement(sqlCot);
            psCot.setString(1, cot.getIdCotizacion());
            psCot.setString(2, cot.getCliente().getIdCliente());
            psCot.setString(3, cot.getEmpleado().getIdEmpleado());
            psCot.setTimestamp(4, Timestamp.valueOf(cot.getFecha()));
            psCot.setDouble(5, cot.getDescuento());
            psCot.setDouble(6, cot.getTotal());
            psCot.setString(7, cot.getEstado());
            psCot.executeUpdate();

            String sqlDet = "INSERT INTO detallecotizacion " +
                    "(id_detalle, id_cotizacion, id_producto, cantidad, precio) " +
                    "VALUES (?,?,?,?,?)";
            PreparedStatement psDet = conexion.prepareStatement(sqlDet);
            for (DetalleCotizacion d : detalles) {
                psDet.setString(1, d.getIdDetalle());
                psDet.setString(2, cot.getIdCotizacion());
                psDet.setString(3, d.getProducto().getIdProducto());
                psDet.setInt(4, d.getCantidad());
                psDet.setDouble(5, d.getPrecio());
                psDet.executeUpdate();
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Rollback error: " + ex.getMessage());
            }
            System.out.println("Error al registrar cotización: " + e.getMessage());
            return false;
        } finally {
            try { conexion.setAutoCommit(true); }
            catch (SQLException e) {
                System.out.println("Error autocommit: " + e.getMessage());
            }
        }
    }

    /**
     * Lista todas las cotizaciones con JOIN a cliente y empleado.
     */
    public List<Cotizacion> listar() {
        List<Cotizacion> lista = new ArrayList<>();
        String sql = "SELECT ct.*, " +
                "c.nombre as nombre_cliente, " +
                "e.nombre as nombre_empleado " +
                "FROM cotizacion ct " +
                "INNER JOIN cliente c  ON ct.id_cliente  = c.id_cliente " +
                "INNER JOIN empleado e ON ct.id_empleado = e.id_empleado " +
                "ORDER BY ct.fecha DESC";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.out.println("Error al listar cotizaciones: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Trae el detalle de una cotización específica.
     * Se usa para mostrar el detalle y para convertir a venta.
     */
    public List<DetalleCotizacion> listarDetalle(String idCotizacion) {
        List<DetalleCotizacion> lista = new ArrayList<>();
        String sql = "SELECT dc.*, p.nombre as nombre_producto, " +
                "p.precio_venta, p.stock " +
                "FROM detallecotizacion dc " +
                "INNER JOIN producto p ON dc.id_producto = p.id_producto " +
                "WHERE dc.id_cotizacion = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idCotizacion);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetalleCotizacion d = new DetalleCotizacion();
                d.setIdDetalle(rs.getString("id_detalle"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecio(rs.getDouble("precio"));

                Producto prod = new Producto();
                prod.setIdProducto(rs.getString("id_producto"));
                prod.setNombre(rs.getString("nombre_producto"));
                prod.setPrecioVenta(rs.getDouble("precio_venta"));
                prod.setStock(rs.getInt("stock"));
                d.setProducto(prod);
                lista.add(d);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar detalle: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Cambia el estado de una cotización.
     * PENDIENTE → ACEPTADA o RECHAZADA.
     */
    public boolean cambiarEstado(String idCotizacion, String nuevoEstado) {
        String sql = "UPDATE cotizacion SET estado = ? WHERE id_cotizacion = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, nuevoEstado);
            ps.setString(2, idCotizacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al cambiar estado: " + e.getMessage());
            return false;
        }
    }

    public String obtenerUltimoId() {
        String sql = QueryHelper.limitar(
                "SELECT id_cotizacion FROM cotizacion " +
                        "ORDER BY id_cotizacion DESC", 1
        );
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id_cotizacion");
        } catch (SQLException e) {
            System.out.println("Error al obtener último id: " + e.getMessage());
        }
        return null;
    }

    /** ELIMINACIÓN DE COTIZACIONES
     * Limpia cotizaciones vencidas automáticamente.
     * - PENDIENTE sin respuesta en 30 días → RECHAZADA
     * - RECHAZADA de más de 6 meses → eliminar
     * - CONVERTIDA de más de 1 año → eliminar
     */
    public void limpiarCotizacionesVencidas() {
        try {
            // 1. Marca como RECHAZADA las pendientes sin respuesta en los últimos 30 días
            String sqlVencer =
                    "UPDATE cotizacion SET estado = 'RECHAZADA' " +
                            "WHERE estado = 'PENDIENTE' " +
                            "AND fecha < " + QueryHelper.restarDias(30);
            conexion.prepareStatement(sqlVencer).executeUpdate();

            // 2. Antes de eliminar cotizaciones, elimina sus detalles por la restricción de clave foránea
            String sqlDetRechazadas =
                    "DELETE FROM detallecotizacion " +
                            "WHERE id_cotizacion IN (" +
                            "    SELECT id_cotizacion FROM cotizacion " +
                            "    WHERE estado = 'RECHAZADA' " +
                            "    AND fecha < " + QueryHelper.restarDias(180) +
                            ")";
            conexion.prepareStatement(sqlDetRechazadas).executeUpdate();

            // 3. Elimina cotizaciones RECHAZADAS de más de 6 meses
            String sqlRechazadas =
                    "DELETE FROM cotizacion " +
                            "WHERE estado = 'RECHAZADA' " +
                            "AND fecha < " + QueryHelper.restarDias(180);
            conexion.prepareStatement(sqlRechazadas).executeUpdate();

            // 4. Elimina detalles de CONVERTIDAS de más de 1 año
            String sqlDetConvertidas =
                    "DELETE FROM detallecotizacion " +
                            "WHERE id_cotizacion IN (" +
                            "    SELECT id_cotizacion FROM cotizacion " +
                            "    WHERE estado = 'CONVERTIDA' " +
                            "    AND fecha < " + QueryHelper.restarDias(365) +
                            ")";
            conexion.prepareStatement(sqlDetConvertidas).executeUpdate();

            // 5. Elimina cotizaciones CONVERTIDAS de más de 1 año
            String sqlConvertidas =
                    "DELETE FROM cotizacion " +
                            "WHERE estado = 'CONVERTIDA' " +
                            "AND fecha < " + QueryHelper.restarDias(365);
            conexion.prepareStatement(sqlConvertidas).executeUpdate();

            System.out.println("Limpieza de cotizaciones completada.");
        } catch (SQLException e) {
            System.out.println("Error en limpieza: " + e.getMessage());
        }
    }

    private Cotizacion mapear(ResultSet rs) throws SQLException {
        Cotizacion c = new Cotizacion();
        c.setIdCotizacion(rs.getString("id_cotizacion"));
        c.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        c.setDescuento(rs.getDouble("descuento"));
        c.setTotal(rs.getDouble("total"));
        c.setEstado(rs.getString("estado"));

        Cliente cl = new Cliente();
        cl.setIdCliente(rs.getString("id_cliente"));
        cl.setNombre(rs.getString("nombre_cliente"));
        c.setCliente(cl);

        Empleado e = new Empleado();
        e.setIdEmpleado(rs.getString("id_empleado"));
        e.setNombre(rs.getString("nombre_empleado"));
        c.setEmpleado(e);

        return c;
    }
}