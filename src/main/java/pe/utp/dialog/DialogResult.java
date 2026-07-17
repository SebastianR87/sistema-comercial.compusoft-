package pe.utp.dialog;

/**
 * Qué botón cerró el diálogo. Se usa internamente entre
 * DialogController y CSDialog; CSDialog.confirm(...) lo traduce a un
 * boolean simple (PRIMARY -> true) para no exponer este detalle a
 * los controllers que ya llaman a la API pública.
 *
 * NONE cubre el cierre "neutro" (ESC o clic fuera de la tarjeta) en
 * diálogos de un solo botón (success/warning/error/info), donde no
 * hay una acción "cancelada" real que distinguir de "aceptada".
 */
public enum DialogResult {
    PRIMARY,
    SECONDARY,
    NONE
}
