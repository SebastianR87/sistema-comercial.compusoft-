package pe.utp.dao;

import pe.utp.Conexion.ConexionDB;
import pe.utp.model.Categoria;
import pe.utp.model.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KardexDAO {

    private Connection conexion;

    public KardexDAO() {
        this.conexion = ConexionDB.getConexion();
    }

    // Modelo de un movimiento del Kárdex
    public static class MovimientoKardex {
        public String fecha;
        public String tipo;// COMPRA o VENTA
        public String documento; // número de comprobante
        public int cantidad;    // cantidad bruta
        public double precio;      // precio del movimiento
        // Entradas
        public int entradaCant;
        public double entradaCosto;
        public double entradaTotal;
        // Salidas
        public int salidaCant;
        public double salidaCosto;
        public double salidaTotal;
        // Existencias (saldo acumulado)
        public int    saldoCant;
        public double saldoCosto;
        public double saldoTotal;
    }

    /**
     * Lista todos los movimientos de un producto ordenados por fecha.
     * Usa DOS consultas simples en vez de un UNION complejo
     * para no sobrecargar la BD (consejo del profesor).
     * El CPP de las salidas se calcula en Java fila por fila.
     */
    public List<MovimientoKardex> listarMovimientos(String idProducto) {
        List<MovimientoKardex> lista = new ArrayList<>();

        // Consulta 1: compras — simple y directa
        String sqlCompras =
                "SELECT c.fecha, c.numero_comprobante AS documento, " +
                        "dc.cantidad, dc.precio " +
                        "FROM detallecompra dc " +
                        "INNER JOIN compra c ON dc.id_compra = c.id_compra " +
                        "WHERE dc.id_producto = ?";

        // Consulta 2: ventas — simple y directa
        String sqlVentas =
                "SELECT v.fecha, v.numero_comprobante AS documento, " +
                        "dv.cantidad, dv.precio " +
                        "FROM detalleventa dv " +
                        "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                        "WHERE dv.id_producto = ?";

        try {
            // Lee compras
            PreparedStatement psC =
                    conexion.prepareStatement(sqlCompras);
            psC.setString(1, idProducto);
            ResultSet rsC = psC.executeQuery();
            while (rsC.next()) {
                MovimientoKardex m = new MovimientoKardex();
                m.tipo = "COMPRA";
                m.fecha = rsC.getTimestamp("fecha")
                        .toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("dd/MM/yyyy HH:mm"));
                m.documento = rsC.getString("documento");
                m.cantidad  = rsC.getInt("cantidad");
                m.precio    = rsC.getDouble("precio");
                lista.add(m);
            }

            // Lee ventas
            PreparedStatement psV =
                    conexion.prepareStatement(sqlVentas);
            psV.setString(1, idProducto);
            ResultSet rsV = psV.executeQuery();
            while (rsV.next()) {
                MovimientoKardex m = new MovimientoKardex();
                m.tipo = "VENTA";
                m.fecha = rsV.getTimestamp("fecha")
                        .toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("dd/MM/yyyy HH:mm"));
                m.documento = rsV.getString("documento");
                m.cantidad  = rsV.getInt("cantidad");
                m.precio    = rsV.getDouble("precio");
                lista.add(m);
            }

            // Ordena todos los movimientos por fecha en Java
            // más simple y eficiente que ORDER BY en UNION
            lista.sort((a, b) -> a.fecha.compareTo(b.fecha));

            // Calcula saldo acumulado y CPP fila por fila
            int saldoCant  = 0;
            double saldoTotal = 0;

            for (MovimientoKardex m : lista) {
                if ("COMPRA".equals(m.tipo)) {
                    // Entrada: suma al saldo
                    m.entradaCant = m.cantidad;
                    m.entradaCosto = m.precio;
                    m.entradaTotal = m.cantidad * m.precio;
                    m.salidaCant = 0;
                    m.salidaCosto = 0;
                    m.salidaTotal = 0;
                    saldoCant  += m.cantidad;
                    saldoTotal += m.entradaTotal;
                } else {
                    // Salida: el costo unitario es el CPP acumulado
                    // hasta ese momento, no el precio de venta al cliente
                    double cppActual = saldoCant > 0
                            ? saldoTotal / saldoCant : 0;
                    m.entradaCant  = 0;
                    m.entradaCosto = 0;
                    m.entradaTotal = 0;
                    m.salidaCant   = m.cantidad;
                    m.salidaCosto  = cppActual;
                    m.salidaTotal  = m.cantidad * cppActual;
                    saldoCant  -= m.cantidad;
                    saldoTotal -= m.salidaTotal;
                    // Evita saldo negativo por redondeo
                    if (saldoTotal < 0) saldoTotal = 0;
                }
                // Existencias acumuladas
                m.saldoCant  = saldoCant;
                m.saldoCosto = saldoCant > 0
                        ? saldoTotal / saldoCant : 0;
                m.saldoTotal = saldoTotal;
            }

        } catch (SQLException e) {
            System.out.println("Error en Kárdex: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Trae los datos completos del producto para la ficha del Kárdex.
     * Incluye categoría, stock mínimo y máximo.
     */
    public Producto obtenerProducto(String idProducto) {
        String sql =
                "SELECT p.*, c.nombre AS nombre_categoria " +
                        "FROM producto p " +
                        "INNER JOIN categoria c " +
                        "ON p.id_categoria = c.id_categoria " +
                        "WHERE p.id_producto = ?";
        try {
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, idProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Producto p = new Producto();
                p.setIdProducto(rs.getString("id_producto"));
                p.setNombre(rs.getString("nombre"));
                p.setStock(rs.getInt("stock"));
                p.setPrecioCompra(rs.getDouble("precio_compra"));
                p.setPrecioVenta(rs.getDouble("precio_venta"));
                p.setStockMinimo(rs.getInt("stock_minimo"));
                p.setStockMaximo(rs.getInt("stock_maximo"));

                Categoria cat = new Categoria();
                cat.setNombre(rs.getString("nombre_categoria"));
                p.setCategoria(cat);
                return p;
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener producto: " +
                    e.getMessage());
        }
        return null;
    }
}