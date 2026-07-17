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
        // fecha real para ordenar cronológicamente. "fecha" (String)
        // solo es para mostrar en pantalla/PDF -- antes se ordenaba
        // comparando ese String con dd/MM/yyyy, lo cual es una
        // comparación alfabética: solo daba el orden correcto dentro
        // del mismo mes y año (ej. "15/01/2026" > "20/12/2025"
        // alfabéticamente, aunque diciembre 2025 es anterior), y
        // corrompía el saldo acumulado FIFO en productos con
        // movimientos de varios meses/años.
        public java.time.LocalDateTime fechaOrden;
        public String fecha;
        public String tipo;      // COMPRA o VENTA
        public String documento; // número de comprobante
        public int cantidad;
        public double precio;
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
     * Lista los movimientos de un producto ordenados por fecha,
     * basados en el costeo por lotes (FIFO) real del sistema:
     *
     * - Entradas: vienen de lote_compra (cada lote = una compra real,
     *   con su costo exacto). Se excluyen los lotes ANULADOS, ya que
     *   una compra anulada nunca "entró" realmente en términos
     *   contables.
     * - Salidas: vienen de detalleventa_lote (cada fila indica de
     *   qué lote salió y a qué costo real, no un promedio). Se
     *   excluyen las ventas con estado ANULADA.
     *
     * El saldo (columna Existencias) se acumula fila por fila en
     * base a estos valores reales -- ya no se recalcula un CPP
     * "adivinado": los entrada/salida que se acumulan son el costo
     * verdadero de cada lote, así que el promedio resultante es
     * mucho más fiel que el esquema anterior.
     */
    public List<MovimientoKardex> listarMovimientos(String idProducto) {
        List<MovimientoKardex> lista = new ArrayList<>();

        // Entradas: un lote = una compra real con su costo exacto.
        // cantidad_original (no cantidad_restante) porque el Kárdex
        // muestra el histórico de lo que entró, no lo que queda hoy.
        String sqlEntradas =
                "SELECT c.fecha, c.numero_comprobante AS documento, " +
                        "lc.cantidad_original AS cantidad, lc.costo_unitario AS precio " +
                        "FROM lote_compra lc " +
                        "INNER JOIN detallecompra dc ON lc.id_detallecompra = dc.id_detallecompra " +
                        "INNER JOIN compra c ON dc.id_compra = c.id_compra " +
                        "WHERE lc.id_producto = ? AND lc.estado = 'ACTIVO'";

        // Salidas: cada fila de detalleventa_lote es el costo REAL
        // del lote consumido en esa venta (FIFO), no un promedio.
        String sqlSalidas =
                "SELECT v.fecha, v.numero_comprobante AS documento, " +
                        "dvl.cantidad AS cantidad, dvl.costo_unitario_momento AS precio " +
                        "FROM detalleventa_lote dvl " +
                        "INNER JOIN detalleventa dv ON dvl.id_detalleventa = dv.id_detalleventa " +
                        "INNER JOIN venta v ON dv.id_venta = v.id_venta " +
                        "WHERE dv.id_producto = ? AND (v.estado IS NULL OR v.estado <> 'ANULADA')";

        try {
            PreparedStatement psE = conexion.prepareStatement(sqlEntradas);
            psE.setString(1, idProducto);
            ResultSet rsE = psE.executeQuery();
            while (rsE.next()) {
                MovimientoKardex m = new MovimientoKardex();
                m.tipo = "COMPRA";
                m.fechaOrden = rsE.getTimestamp("fecha").toLocalDateTime();
                m.fecha = m.fechaOrden
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                m.documento = rsE.getString("documento");
                m.cantidad  = rsE.getInt("cantidad");
                m.precio    = rsE.getDouble("precio");
                lista.add(m);
            }

            PreparedStatement psS = conexion.prepareStatement(sqlSalidas);
            psS.setString(1, idProducto);
            ResultSet rsS = psS.executeQuery();
            while (rsS.next()) {
                MovimientoKardex m = new MovimientoKardex();
                m.tipo = "VENTA";
                m.fechaOrden = rsS.getTimestamp("fecha").toLocalDateTime();
                m.fecha = m.fechaOrden
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                m.documento = rsS.getString("documento");
                m.cantidad  = rsS.getInt("cantidad");
                m.precio    = rsS.getDouble("precio");
                lista.add(m);
            }

            // Ordena todos los movimientos por fecha real (no por el
            // String formateado -- ver comentario en fechaOrden)
            lista.sort((a, b) -> a.fechaOrden.compareTo(b.fechaOrden));

            // Acumula el saldo fila por fila, usando los costos
            // REALES de cada entrada/salida (no un CPP recalculado)
            int saldoCant  = 0;
            double saldoTotal = 0;

            for (MovimientoKardex m : lista) {
                if ("COMPRA".equals(m.tipo)) {
                    m.entradaCant  = m.cantidad;
                    m.entradaCosto = m.precio;
                    m.entradaTotal = m.cantidad * m.precio;
                    m.salidaCant   = 0;
                    m.salidaCosto  = 0;
                    m.salidaTotal  = 0;
                    saldoCant  += m.cantidad;
                    saldoTotal += m.entradaTotal;
                } else {
                    m.entradaCant  = 0;
                    m.entradaCosto = 0;
                    m.entradaTotal = 0;
                    m.salidaCant   = m.cantidad;
                    m.salidaCosto  = m.precio; // costo real del lote consumido
                    m.salidaTotal  = m.cantidad * m.precio;
                    saldoCant  -= m.cantidad;
                    saldoTotal -= m.salidaTotal;
                    if (saldoTotal < 0) saldoTotal = 0;
                }
                m.saldoCant  = saldoCant;
                m.saldoCosto = saldoCant > 0 ? saldoTotal / saldoCant : 0;
                m.saldoTotal = saldoTotal;
            }

        } catch (SQLException e) {
            System.out.println("Error en Kárdex: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Trae los datos completos del producto para la ficha del Kárdex.
     * El stock viene de la columna resumen de producto (ya se
     * mantiene sincronizada con la suma de lotes activos).
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
                p.setPrecioVenta(rs.getDouble("precio_venta"));
                p.setStockMinimo(rs.getInt("stock_minimo"));
                p.setStockMaximo(rs.getInt("stock_maximo"));

                Categoria cat = new Categoria();
                cat.setNombre(rs.getString("nombre_categoria"));
                p.setCategoria(cat);
                return p;
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener producto: " + e.getMessage());
        }
        return null;
    }
}