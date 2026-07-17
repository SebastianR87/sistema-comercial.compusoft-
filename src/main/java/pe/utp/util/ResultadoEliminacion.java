package pe.utp.util;

import pe.utp.dialog.CSDialog;

public final class ResultadoEliminacion {

    private final boolean permitido;
    private final String titulo;
    private final String detalle;

    private ResultadoEliminacion(boolean permitido, String titulo, String detalle) {
        this.permitido = permitido;
        this.titulo = titulo;
        this.detalle = detalle;
    }

    public static ResultadoEliminacion permitir() {
        return new ResultadoEliminacion(true, null, null);
    }

    public static ResultadoEliminacion bloquear(String titulo, String detalle) {
        return new ResultadoEliminacion(false, titulo, detalle);
    }

    public boolean isPermitido() {
        return permitido;
    }

    // Otro punto centralizado (usado por todos los "Eliminar" que
    // primero validan si el registro tiene movimientos asociados):
    // el Alert de 3 partes (título de ventana + header + contenido)
    // se reduce a los 2 campos de CSDialog (titulo/mensaje) -- el
    // título de ventana genérico "No se puede eliminar" no aportaba
    // nada que el propio header no dijera ya.
    public void mostrarAlerta() {
        if (permitido) return;
        CSDialog.warning(titulo, detalle);
    }
}
