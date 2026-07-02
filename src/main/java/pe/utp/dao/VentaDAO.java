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
     * A diferencia de Compra, aquí RESTAMOS stock y validamos
     * que haya suficiente stock antes de confirmar.
     *
     * Flujo:
     * 1. Verifica stock disponible de cada producto
     * 2. Inserta la cabecera en tabla venta
     * 3. Inserta cada línea en detalle_venta
     * 4. Resta el stock de cada producto
     * 5. COMMIT si todo salió bien, ROLLBACK si algo falló
     */
    public String registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        try {
            conexion.setAutoCommit(false);

            // Paso 1: verifica stock ANTES de hacer cualquier cambio
            String sqlStockActual = "SELECT stock FROM producto WHERE id_producto = ?";
            PreparedStatement psStockActual = conexion.prepareStatement(sqlStockActual);

            for (DetalleVenta d : detalles) {
                psStockActual.setString(1, d.getProducto().getIdProducto());
                ResultSet rs = psStockActual.executeQuery();
                int stockDisponible = 0;
                if (rs.next()) stockDisponible = rs.getInt("stock");
                rs.close();

                if (d.getCantidad() > stockDisponible) {
                    conexion.rollback();
                    conexion.setAutoCommit(true);
                    // Retorna mensaje de error específico con el producto
                    return "STOCK_INSUFICIENTE:" + d.getProducto().getNombre() +
                            ":" + stockDisponible;
                }
            }

            // Paso 2: inserta la cabecera de la venta
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

            // Paso 3 y 4: inserta detalle y resta stock
            String sqlDetalle = "INSERT INTO detalleventa " +
                    "(id_detalleventa, id_venta, id_producto, cantidad, precio) " +
                    "VALUES (?,?,?,?,?)";
            String sqlRestarStock = "UPDATE producto " +
                    "SET stock = stock - ? WHERE id_producto = ?";

            PreparedStatement psDetalle = conexion.prepareStatement(sqlDetalle);
            PreparedStatement psRestar = conexion.prepareStatement(sqlRestarStock);

            for (DetalleVenta d : detalles) {
                psDetalle.setString(1, d.getIdDetalleVenta());
                psDetalle.setString(2, venta.getIdVenta());
                psDetalle.setString(3, d.getProducto().getIdProducto());
                psDetalle.setInt(4, d.getCantidad());
                psDetalle.setDouble(5, d.getPrecio());
                psDetalle.executeUpdate();

                psRestar.setInt(1, d.getCantidad());
                psRestar.setString(2, d.getProducto().getIdProducto());
                psRestar.executeUpdate();
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
     * Anula una venta ya registrada: revierte el stock de cada producto
     * vendido y marca la venta como ANULADA. No borra ningún registro, para mantener consistencia
     *
     * Flujo:
     * 1. Verifica que la venta exista y no esté ya anulada
     * 2. Trae el detalle de la venta (para saber qué stock revertir)
     * 3. Devuelve el stock de cada producto
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

            // Paso 3: devuelve el stock de cada producto
            String sqlDevolverStock = "UPDATE producto " +
                    "SET stock = stock + ? WHERE id_producto = ?";
            PreparedStatement psDevolver = conexion.prepareStatement(sqlDevolverStock);

            for (DetalleVenta d : detalles) {
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
