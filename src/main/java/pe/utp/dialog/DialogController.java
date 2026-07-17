package pe.utp.dialog;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller de Dialog.fxml. No se usa directamente desde los
 * controllers de la app -- CSDialog es la única puerta de entrada
 * (por eso configurar()/getResultado() son package-private: solo
 * CSDialog, en el mismo paquete, necesita llamarlos).
 *
 * Responsabilidades: pintar el tipo de diálogo (color + icono),
 * cablear botones/ESC/clic-afuera, animar la entrada, y devolver qué
 * botón cerró el diálogo.
 */
public class DialogController {

    @FXML private StackPane rootOverlay;
    @FXML private VBox card;
    @FXML private StackPane iconBadge;
    @FXML private Label lblTitulo;
    @FXML private Label lblMensaje;
    @FXML private HBox panelBotones;
    @FXML private Button btnSecundario;
    @FXML private Button btnPrimario;

    private Stage stage;
    private DialogResult resultado = DialogResult.NONE;

    /**
     * Termina de configurar el diálogo ya cargado por CSDialog:
     * textos, color/icono según el tipo, qué botones se muestran,
     * atajos de teclado y foco inicial.
     *
     * @param focoEnSecundario si es true, el botón secundario
     *        (Cancelar) recibe el foco visual inicial en vez del
     *        primario -- útil en confirmaciones destructivas, para
     *        que "sin querer" el usuario no confirme apretando Enter
     *        de reflejo. Enter SIEMPRE activa el botón primario
     *        (defaultButton), independientemente de este flag: son
     *        dos cosas distintas (foco visual vs. atajo de teclado).
     * @param peligroso si es true (solo tiene efecto con tipo CONFIRM),
     *        pinta SOLO el botón primario en coral en vez del navy por
     *        defecto -- para distinguir a simple vista una confirmación
     *        irreversible (eliminar, anular) de una neutra (ej. cerrar
     *        sesión). El ícono ("?") se queda siempre en el navy propio
     *        de CONFIRM: es el mismo símbolo de pregunta en los dos
     *        casos, lo que cambia es la gravedad de la acción del
     *        botón, no la pregunta en sí.
     */
    void configurar(DialogType tipo, String titulo, String mensaje,
                     String textoPrimario, String textoSecundario,
                     boolean focoEnSecundario, boolean peligroso, Stage stage) {
        this.stage = stage;

        lblTitulo.setText(titulo);
        lblMensaje.setText(mensaje);

        String colorIcono = colorDe(tipo, false);
        iconBadge.setStyle("-fx-background-color: " + colorIcono + ";");
        iconBadge.getChildren().setAll(iconoDe(tipo));

        String colorBoton = colorDe(tipo, peligroso);
        btnPrimario.setText(textoPrimario);
        btnPrimario.setStyle("-fx-background-color: " + colorBoton + ";");
        btnPrimario.setDefaultButton(true);
        btnPrimario.setOnAction(e -> cerrar(DialogResult.PRIMARY));

        boolean tieneSecundario = tipo == DialogType.CONFIRM && textoSecundario != null;
        btnSecundario.setVisible(tieneSecundario);
        btnSecundario.setManaged(tieneSecundario);
        if (tieneSecundario) {
            btnSecundario.setText(textoSecundario);
            btnSecundario.setOnAction(e -> cerrar(DialogResult.SECONDARY));
        }

        // El cierre "neutro" (ESC o clic fuera de la tarjeta) equivale
        // a cancelar en un CONFIRM (dos opciones reales) y a un simple
        // cierre sin acción en los demás tipos (una sola opción, no
        // hay nada que "cancelar").
        DialogResult resultadoAlCerrarFuera =
                tipo == DialogType.CONFIRM ? DialogResult.SECONDARY : DialogResult.NONE;

        rootOverlay.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                cerrar(resultadoAlCerrarFuera);
            }
        });
        rootOverlay.setOnMouseClicked(ev -> {
            if (ev.getTarget() == rootOverlay) {
                cerrar(resultadoAlCerrarFuera);
            }
        });

        stage.setOnShown(e -> {
            // El foco inicial se pone en un botón (no en rootOverlay):
            // los KeyEvent de un control enfocado igual burbujean hacia
            // arriba, así que el listener de ESC en rootOverlay los
            // sigue recibiendo sin necesidad de pelear por el foco.
            (focoEnSecundario && tieneSecundario ? btnSecundario : btnPrimario).requestFocus();
            animarEntrada();
        });
    }

    private void animarEntrada() {
        card.setOpacity(0);
        card.setScaleX(0.92);
        card.setScaleY(0.92);
        rootOverlay.setOpacity(0);

        FadeTransition overlayFade = new FadeTransition(Duration.millis(160), rootOverlay);
        overlayFade.setToValue(1);

        FadeTransition cardFade = new FadeTransition(Duration.millis(200), card);
        cardFade.setToValue(1);

        ScaleTransition cardScale = new ScaleTransition(Duration.millis(200), card);
        cardScale.setToX(1);
        cardScale.setToY(1);
        cardScale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(overlayFade, cardFade, cardScale).play();
    }

    private void cerrar(DialogResult resultado) {
        this.resultado = resultado;
        stage.close();
    }

    DialogResult getResultado() {
        return resultado;
    }

    /**
     * Mismos 4 colores de marca que ya usa el resto de la app
     * (ver dialog.css) -- ninguno es nuevo, todos ya representan el
     * mismo significado en otras pantallas (verde=positivo,
     * ámbar=atención, coral=peligro/eliminar, navy=info).
     *
     * Un CONFIRM marcado como {@code peligroso} (eliminar, anular)
     * toma el mismo coral que ERROR, en vez del navy que usan las
     * confirmaciones neutras (ej. cerrar sesión) -- así el color ya
     * anticipa qué tan reversible es la acción antes de leer el texto.
     */
    private String colorDe(DialogType tipo, boolean peligroso) {
        if (tipo == DialogType.CONFIRM && peligroso) {
            return "#E94560";
        }
        return switch (tipo) {
            case SUCCESS -> "#166534";
            case WARNING -> "#92400E";
            case ERROR -> "#E94560";
            case INFO, CONFIRM -> "#315E9E";
        };
    }

    /**
     * Iconos construidos con formas geométricas simples (Polyline /
     * Line) o un glifo de texto en vez de un path SVG escrito a mano:
     * un path SVG mal formado revienta en tiempo de ejecución (no en
     * compilación) y tumbaría CSDialog completo la primera vez que se
     * use ese tipo, en cualquier controller de la app. Con coordenadas
     * explícitas no hay nada que parsear ni que se pueda romper.
     */
    private Node iconoDe(DialogType tipo) {
        return switch (tipo) {
            case SUCCESS -> construirCheck();
            case ERROR -> construirX();
            case WARNING -> construirGlifo("!");
            case INFO -> construirGlifo("i");
            case CONFIRM -> construirGlifo("?");
        };
    }

    private Polyline construirCheck() {
        Polyline check = new Polyline(4.5, 12.5, 9.5, 18, 20, 5);
        check.setStroke(Color.WHITE);
        check.setStrokeWidth(2.6);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);
        check.setFill(null);
        return check;
    }

    private Group construirX() {
        Line l1 = new Line(6, 6, 18, 18);
        Line l2 = new Line(18, 6, 6, 18);
        for (Line l : new Line[]{l1, l2}) {
            l.setStroke(Color.WHITE);
            l.setStrokeWidth(2.6);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        return new Group(l1, l2);
    }

    private Label construirGlifo(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 800;");
        return lbl;
    }
}
