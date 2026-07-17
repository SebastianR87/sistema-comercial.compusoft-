package pe.utp.dialog;

import javafx.stage.Stage;

/**
 * Referencia a la ventana raíz de la aplicación: el {@link Stage} que
 * {@code App.start(Stage)} recibe de JavaFX y que persiste durante toda
 * la sesión (Login y luego Main reutilizan ese mismo Stage, solo le
 * cambian el Scene -- ver LoginController).
 *
 * CSDialog la usa para que el overlay semitransparente cubra siempre
 * TODA la aplicación, incluso cuando el diálogo se muestra sobre un
 * modal hijo (ej. una advertencia de validación dentro del modal de
 * editar Cliente). Sin esto, CSDialog se dimensionaba según la
 * "ventana activa" (el modal, cuando hay uno abierto), y el overlay
 * solo oscurecía ese modal, dejando el resto de la app visible detrás.
 */
public final class VentanaPrincipal {

    private static Stage instancia;

    private VentanaPrincipal() {
    }

    /** Llamado una sola vez, desde App.start(). */
    public static void registrar(Stage stage) {
        instancia = stage;
    }

    /** Puede ser null si se llama antes de que App.start() haya corrido. */
    public static Stage obtener() {
        return instancia;
    }
}
