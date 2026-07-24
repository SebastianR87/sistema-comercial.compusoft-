package pe.utp.service;

import pe.utp.Conexion.ConexionDB;
import pe.utp.dao.LoteDAO;
import pe.utp.dao.VentaDAO;
import pe.utp.model.DetalleVenta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Lógica de negocio de Venta que orquesta varios DAO dentro de una
 * misma transacción. Los DAO (VentaDAO, LoteDAO) solo saben ejecutar
 * SQL puntual; aquí es donde se decide el flujo completo y se abre/
 * cierra la transacción real (setAutoCommit/commit/rollback).
 */
public class VentaService {

    private final Connection conexion;
    private final VentaDAO ventaDAO;
    private final LoteDAO loteDAO;

    public VentaService() {
        this.conexion = ConexionDB.getConexion();
        this.ventaDAO = new VentaDAO();
        this.loteDAO = new LoteDAO(conexion);
    }

    public String anularVenta(String idVenta) {
        try {
            conexion.setAutoCommit(false);

            String estadoActual = ventaDAO.obtenerEstado(idVenta);
            if (estadoActual == null) {
                conexion.rollback();
                return "NO_EXISTE";
            }
            if ("ANULADA".equalsIgnoreCase(estadoActual)) {
                conexion.rollback();
                return "YA_ANULADA";
            }

            List<DetalleVenta> detalles = ventaDAO.listarDetalle(idVenta);

            for (DetalleVenta d : detalles) {
                for (LoteDAO.Consumo consumo : loteDAO.listarLotesConsumidos(d.getIdDetalleVenta())) {
                    loteDAO.devolverALote(consumo.getIdLote(), consumo.getCantidad());
                }
                ventaDAO.devolverStockProducto(d.getProducto().getIdProducto(), d.getCantidad());
            }

            ventaDAO.marcarAnulada(idVenta);

            conexion.commit();
            return "OK";

        } catch (SQLException e) {
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Error en rollback: " + ex.getMessage());
            }
            System.out.println("Error al anular venta: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        } finally {
            try { conexion.setAutoCommit(true); } catch (SQLException e) {
                System.out.println("Error al restaurar autocommit: " + e.getMessage());
            }
        }
    }
}
