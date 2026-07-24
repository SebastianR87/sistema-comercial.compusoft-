package pe.utp.service;

import pe.utp.Conexion.ConexionDB;
import pe.utp.dao.CompraDAO;
import pe.utp.dao.LoteDAO;
import pe.utp.model.DetalleCompra;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Lógica de negocio de Compra que orquesta varios DAO dentro de una
 * misma transacción. Los DAO (CompraDAO, LoteDAO) solo saben ejecutar
 * SQL puntual; aquí es donde se decide el flujo completo y se abre/
 * cierra la transacción real (setAutoCommit/commit/rollback).
 */
public class CompraService {

    private final Connection conexion;
    private final CompraDAO compraDAO;
    private final LoteDAO loteDAO;

    public CompraService() {
        this.conexion = ConexionDB.getConexion();
        this.compraDAO = new CompraDAO();
        this.loteDAO = new LoteDAO(conexion);
    }

    /**
     * Anula una compra ya registrada: valida que ningún lote generado
     * por ella se haya usado todavía, revierte el stock del producto
     * y anula los lotes. No borra ningún registro.
     */
    public String anularCompra(String idCompra) {
        try {
            conexion.setAutoCommit(false);

            String estadoActual = compraDAO.obtenerEstado(idCompra);
            if (estadoActual == null) {
                conexion.rollback();
                return "NO_EXISTE";
            }
            if ("ANULADA".equalsIgnoreCase(estadoActual)) {
                conexion.rollback();
                return "YA_ANULADA";
            }

            List<DetalleCompra> detalles = compraDAO.listarDetalle(idCompra);

            // Valida que ningún lote generado por esta compra se haya
            // consumido ya (nada vendido) antes de tocar nada más
            for (DetalleCompra d : detalles) {
                int[] cantidades = compraDAO.obtenerCantidadesLote(d.getIdDetalleCompra());
                if (cantidades != null) {
                    int restante = cantidades[0];
                    int original = cantidades[1];
                    if (restante < original) {
                        conexion.rollback();
                        return "STOCK_INSUFICIENTE:" + d.getProducto().getNombre() + ":" + restante;
                    }
                }
            }

            for (DetalleCompra d : detalles) {
                compraDAO.restarStockProducto(d.getProducto().getIdProducto(), d.getCantidad());
            }
            loteDAO.anularLotesDeCompra(idCompra);

            compraDAO.marcarAnulada(idCompra);

            conexion.commit();
            return "OK";

        } catch (SQLException e) {
            try { conexion.rollback(); } catch (SQLException ex) {
                System.out.println("Error en rollback: " + ex.getMessage());
            }
            System.out.println("Error al anular compra: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        } finally {
            try { conexion.setAutoCommit(true); } catch (SQLException e) {
                System.out.println("Error al restaurar autocommit: " + e.getMessage());
            }
        }
    }
}
