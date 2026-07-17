package pe.utp.dialog;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Punto de entrada único al sistema de diálogos propio de CompuSoft.
 * Reemplazo directo de {@code new Alert(Alert.AlertType...)}: mismo
 * nivel de sencillez de uso (una llamada estática, sin tener que
 * construir ni configurar nada a mano), pero con la apariencia
 * moderna del componente (Dialog.fxml + dialog.css) en vez del
 * diálogo nativo del sistema operativo.
 */
public final class CSDialog {

    private static final String FXML_PATH = "/fxml/Dialog.fxml";
    private static final String STYLE_CSS = "/styles/style.css";
    private static final String DIALOG_CSS = "/styles/dialog.css";

    private CSDialog() {
    }

    public static void success(String titulo, String mensaje) {
        mostrar(DialogType.SUCCESS, titulo, mensaje, "Aceptar", null, false, false);
    }

    public static void warning(String titulo, String mensaje) {
        mostrar(DialogType.WARNING, titulo, mensaje, "Entendido", null, false, false);
    }

    public static void error(String titulo, String mensaje) {
        mostrar(DialogType.ERROR, titulo, mensaje, "Cerrar", null, false, false);
    }

    public static void info(String titulo, String mensaje) {
        mostrar(DialogType.INFO, titulo, mensaje, "Aceptar", null, false, false);
    }

    /** Confirmación con botones por defecto ("Confirmar" / "Cancelar"), foco inicial en el primario. */
    public static boolean confirm(String titulo, String mensaje) {
        return confirm(titulo, mensaje, "Confirmar", "Cancelar");
    }

    /** Confirmación con texto de botón personalizado (ej. "Eliminar" / "Cancelar"). */
    public static boolean confirm(String titulo, String mensaje,
                                   String textoConfirmar, String textoCancelar) {
        return confirm(titulo, mensaje, textoConfirmar, textoCancelar, false);
    }

    /**
     * Variante completa: además permite que el foco inicial quede en
     * el botón secundario (Cancelar) en vez del primario -- útil para
     * confirmaciones destructivas (ej. eliminar), donde conviene que
     * un Enter reflejo no dispare la acción irreversible. Enter sigue
     * activando siempre el botón primario (defaultButton); este flag
     * solo mueve el foco visual/de teclado inicial, no cambia qué
     * botón dispara Enter.
     *
     * Usa el color navy (info) por defecto para el botón/ícono. Para
     * confirmaciones realmente destructivas (eliminar, anular) usa la
     * variante de 6 argumentos con {@code peligroso = true}, que los
     * pinta en coral -- el mismo color que ya usa el resto de la app
     * para "eliminar" (ver dialog.css).
     */
    public static boolean confirm(String titulo, String mensaje,
                                   String textoConfirmar, String textoCancelar,
                                   boolean focoEnCancelar) {
        return confirm(titulo, mensaje, textoConfirmar, textoCancelar, focoEnCancelar, false);
    }

    /**
     * Variante completa con control del color de peligro: cuando
     * {@code peligroso} es true, el ícono y el botón primario se
     * pintan en coral (#E94560) en vez del navy por defecto -- para
     * que una acción irreversible (eliminar, anular) se distinga a
     * simple vista de una confirmación neutra (ej. cerrar sesión).
     */
    public static boolean confirm(String titulo, String mensaje,
                                   String textoConfirmar, String textoCancelar,
                                   boolean focoEnCancelar, boolean peligroso) {
        DialogResult resultado = mostrar(DialogType.CONFIRM, titulo, mensaje,
                textoConfirmar, textoCancelar, focoEnCancelar, peligroso);
        return resultado == DialogResult.PRIMARY;
    }

    private static DialogResult mostrar(DialogType tipo, String titulo, String mensaje,
                                         String textoPrimario, String textoSecundario,
                                         boolean focoEnSecundario, boolean peligroso) {
        if (!Platform.isFxApplicationThread()) {
            // Toda la app es síncrona sobre el hilo de UI (no hay
            // llamadas a estos diálogos desde hilos de fondo hoy),
            // así que esto solo debería dispararse si alguien integra
            // CSDialog mal en el futuro. Mejor un mensaje claro que un
            // IllegalStateException genérico de JavaFX más abajo.
            throw new IllegalStateException(
                    "CSDialog debe invocarse desde el hilo de JavaFX (Platform.isFxApplicationThread())");
        }
        try {
            return mostrarInterno(tipo, titulo, mensaje, textoPrimario, textoSecundario, focoEnSecundario, peligroso);
        } catch (Exception e) {
            // Si el propio componente falla al cargar (FXML/CSS no
            // encontrado, etc.) no debe dejar al usuario sin ningún
            // aviso ni tumbar el controller que lo llamó: se degrada
            // al Alert nativo como red de seguridad.
            System.out.println("CSDialog: no se pudo mostrar el diálogo propio, usando Alert nativo. " + e);
            return mostrarFallback(tipo, titulo, mensaje, textoPrimario, textoSecundario);
        }
    }

    private static DialogResult mostrarInterno(DialogType tipo, String titulo, String mensaje,
                                                 String textoPrimario, String textoSecundario,
                                                 boolean focoEnSecundario, boolean peligroso) throws Exception {
        FXMLLoader loader = new FXMLLoader(CSDialog.class.getResource(FXML_PATH));
        Parent root = loader.load();
        DialogController controller = loader.getController();

        Window owner = ventanaActiva();

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        Scene scene = new Scene(root);
        scene.setFill(null); // sin esto, detrás del overlay se ve un rectángulo blanco (el relleno por defecto de Scene)

        URL styleCss = CSDialog.class.getResource(STYLE_CSS);
        URL dialogCss = CSDialog.class.getResource(DIALOG_CSS);
        if (styleCss != null) scene.getStylesheets().add(styleCss.toExternalForm());
        if (dialogCss != null) scene.getStylesheets().add(dialogCss.toExternalForm());

        stage.setScene(scene);

        // El overlay se dimensiona según la ventana PRINCIPAL de la
        // app (registrada por App.start()), no según la ventana activa.
        // Si se usara la activa, un diálogo abierto sobre un modal hijo
        // (ej. una advertencia de validación dentro de "Editar
        // Cliente") solo oscurecería ese modal pequeño, dejando el
        // resto de la aplicación visible detrás. Con la ventana
        // principal, el overlay siempre cubre toda la app; el owner
        // (la ventana activa) se sigue usando para initOwner(), así el
        // diálogo se sigue apilando por encima del modal correctamente.
        Window ventanaBase = VentanaPrincipal.obtener();
        if (ventanaBase == null || !ventanaBase.isShowing()) {
            ventanaBase = owner;
        }

        if (ventanaBase != null) {
            stage.setX(ventanaBase.getX());
            stage.setY(ventanaBase.getY());
            stage.setWidth(ventanaBase.getWidth());
            stage.setHeight(ventanaBase.getHeight());
        } else {
            // Sin ninguna ventana detectable (caso borde, ej. primer
            // frame antes de mostrar Login): al menos que no quede en
            // 0x0, y que JavaFX lo centre en el monitor.
            stage.setWidth(480);
            stage.setHeight(320);
            stage.centerOnScreen();
        }

        controller.configurar(tipo, titulo, mensaje, textoPrimario, textoSecundario,
                focoEnSecundario, peligroso, stage);

        stage.showAndWait();
        return controller.getResultado();
    }

    /** Busca la ventana enfocada; si ninguna lo está (foco perdido momentáneamente), usa la última mostrada. */
    private static Window ventanaActiva() {
        List<Window> ventanas = Window.getWindows();
        Optional<Window> enfocada = ventanas.stream()
                .filter(Window::isShowing)
                .filter(Window::isFocused)
                .findFirst();
        if (enfocada.isPresent()) {
            return enfocada.get();
        }
        return ventanas.stream()
                .filter(Window::isShowing)
                .reduce((primera, ultima) -> ultima)
                .orElse(null);
    }

    private static DialogResult mostrarFallback(DialogType tipo, String titulo, String mensaje,
                                                  String textoPrimario, String textoSecundario) {
        Alert.AlertType tipoNativo = switch (tipo) {
            case SUCCESS, INFO -> Alert.AlertType.INFORMATION;
            case WARNING -> Alert.AlertType.WARNING;
            case ERROR -> Alert.AlertType.ERROR;
            case CONFIRM -> Alert.AlertType.CONFIRMATION;
        };

        if (tipo == DialogType.CONFIRM) {
            Alert alert = new Alert(tipoNativo, mensaje, ButtonType.YES, ButtonType.NO);
            alert.setTitle(titulo);
            Optional<ButtonType> resp = alert.showAndWait();
            return (resp.isPresent() && resp.get() == ButtonType.YES)
                    ? DialogResult.PRIMARY : DialogResult.SECONDARY;
        }

        Alert alert = new Alert(tipoNativo, mensaje);
        alert.setTitle(titulo);
        alert.showAndWait();
        return DialogResult.PRIMARY;
    }
}
