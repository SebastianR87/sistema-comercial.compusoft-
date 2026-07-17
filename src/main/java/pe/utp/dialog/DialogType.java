package pe.utp.dialog;

/**
 * Tipos de diálogo soportados por CSDialog. Cada uno define su
 * propio color de marca e icono (ver DialogController.colorDe()/
 * iconoDe()) y cuántos botones muestra: CONFIRM es el único con dos
 * (primario + secundario); el resto solo tiene el botón primario.
 */
public enum DialogType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    CONFIRM
}
