package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import pe.utp.model.DetalleVenta;
import pe.utp.model.Venta;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class VentaDetalleModalController {

    @FXML private VBox panelImprimible;

    // Fuente monoespaciada para alineación perfecta en impresión.
    // Tamaño original, tal como estaba antes de tocar nada.
    private static final String F  =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 11px; -fx-text-fill: black;";
    private static final String FB =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: black;";
    private static final String FB12 =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;";

    // Anchos fijos de columna de la tabla de productos, compartidos
    // entre addEncabezadoTabla() y addFilaProducto() para que el
    // encabezado y las filas siempre queden alineados. Antes "Cant."
    // media 60px en el encabezado pero la columna de datos medía
    // solo 38px -- un descuadre real que corría "P.Unit."/"Importe"
    // del encabezado respecto a los datos de cada fila. Se dejan
    // unificados aquí (esto no afecta el tamaño de letra).
    private static final double COL_CODIGO  = 55;
    private static final double COL_CANT    = 45;
    private static final double COL_PUNIT   = 60;
    private static final double COL_IMPORTE = 65;

    // Ancho angosto tipo "ticket" que se usa al imprimir en vez de
    // estirar el contenido a todo el ancho de la hoja (ver imprimir()):
    // coincide con el ancho de contenido de la vista previa en pantalla
    // (modal de 400px menos el padding de 20px a cada lado). El diseño
    // sigue pensado para una impresora térmica real, así que en una
    // hoja A4 de prueba se va a ver margen blanco a los costados --
    // eso es esperado, no un error.
    private static final double ANCHO_TICKET_IMPRESION = 360;

    public void cargarDatos(Venta venta, List<DetalleVenta> detalle, double igvTasa) {
        panelImprimible.getChildren().clear();

        //ENCABEZADO EMPRESA
        addCentrado("COMPUSOFT S.A.C.", 13, true);
        addCentrado("R.U.C. 20123456789", 11, false);
        addCentrado("AV. JUAN VELAZCO NRO. 347", 11, false);
        addCentrado("LIMA - VILLA EL SALVADOR", 11, false);
        addSepSimple();

        //TIPO Y NÚMERO
        String tipo = venta.getTipoComprobante() != null
                ? venta.getTipoComprobante().getNombre().toUpperCase()
                : "COMPROBANTE";
        addCentrado(tipo + " ELECTRÓNICA", 12, true);
        addCentrado("Nro. " + venta.getNumeroComprobante(), 11, true);
        addSepSimple();

        //DATOS GENERALES
        String fecha = venta.getFecha()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora  = venta.getFecha()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        addDato("FECHA DE EMISIÓN:", fecha + "  " + hora);
        addDato("TIPO DE MONEDA:", "PEN");
        addDato("CAJERO:", enmascararNombre(venta.getEmpleado().getNombre()));

        String tipoDoc = venta.getCliente().getTipoDocumento() != null
                ? venta.getCliente().getTipoDocumento()
                .getDocumento().toUpperCase()
                : "DOCUMENTO";
        addDato(tipoDoc + " CLIENTE:",
                enmascararDocumento(venta.getCliente().getNumeroDocumento()));
        addDato("CLIENTE:", venta.getCliente().getNombre().toUpperCase());
        addSepSimple();

        //ENCABEZADO TABLA
        //Código | Descripción | Cant. | P.Unit. | Dcto | Importe
        addEncabezadoTabla();
        addSepSimple();

        //FILAS DE PRODUCTOS
        for (DetalleVenta d : detalle) {
            addFilaProducto(d);
        }
        addSepSimple();

        //MONTOS
        double total     = venta.getTotal();
        double descuento = venta.getDescuento();
        double subtotal  = total + descuento;
        double base      = subtotal / (1 + igvTasa);
        double igv       = subtotal - base;

        //Líneas de montos con relleno punteado y valor alineado a la derecha
        //Mismo estilo que la imagen de referencia
        addMontoLinea("OP. EXONERADA", "0.00",                        false);
        addMontoLinea("OP. INAFECTA",  "0.00",                        false);
        addMontoLinea("OP. GRAVADA",   String.format(Locale.US, "%.2f", base),   false);
        addMontoLinea("I.G.V.",        String.format(Locale.US, "%.2f", igv),    false);
        addMontoLinea("DESCUENTOS",    String.format(Locale.US, "%.2f", descuento), false);
        addSepSimple();
        //Importe Total en negrita con el mismo estilo de línea punteada
        addMontoLinea("IMPORTE TOTAL", String.format(Locale.US, "%.2f", total),  true);
        addSepSimple();

        //SON: CENTRADO
        addCentrado("SON: " + montoEnLetras(total) + " SOLES.", 11, true);
        addSepSimple();

        //CANCELÓ CON: CENTRADO
        String metodo = venta.getMetodoPago() != null
                ? venta.getMetodoPago().getMetodoDePago().toUpperCase() : "";

        //Efectivo: muestra redondeo, total y texto
        if (metodo.equals("EFECTIVO") && venta.getMontoPagado() > 0) {
            addMontoLinea("REDONDEO", "0.00", false);
            addCentrado(
                    "EFECTIVO S/: " +
                            String.format(Locale.US, "%.2f", venta.getMontoPagado()) + " SOLES",
                    11, false);
            addMontoLinea("VUELTO", String.format(Locale.US, "%.2f", venta.getVuelto()), true);
        } else {
            //Tarjeta/Yape/Transferencia: formato "ONLINE VISA S/: X.XX SOLES"
            String cancelo = metodo.contains("TARJETA")
                    ? metodo.replace("TARJETA DE ", "") + " S/: " +
                    String.format(Locale.US, "%.2f", total) + " SOLES"
                    : metodo + " S/: " +
                    String.format(Locale.US, "%.2f", total) + " SOLES";
            addCentrado(cancelo, 11, false);

            if (metodo.contains("TARJETA")) {
                addSepSimple();
                addCentrado(
                        "EL IMPORTE ANOTADO EN ESTE TÍTULO\n" +
                                "HA SIDO CARGADO A SU CUENTA.", 10, false);
            }
        }

        addCentrado("Gracias por comprar en COMPUSOFT S.A.C.", 11, false);
    }

    //HELPERS
    /** Texto centrado. */
    private void addCentrado(String texto, int size, boolean bold) {
        Label lbl = new Label(texto);
        lbl.setStyle(
                (bold
                        ? "-fx-font-weight: bold; "
                        : "") +
                        "-fx-font-family: 'Courier New'; " +
                        "-fx-font-size: " + size + "px; -fx-text-fill: black;"
        );
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        lbl.setWrapText(true);
        panelImprimible.getChildren().add(lbl);
    }

    /** Fila datos: etiqueta izquierda, valor pegado a la derecha. */
    private void addDato(String etiqueta, String valor) {
        Label e = new Label(etiqueta);
        e.setStyle(FB);
        e.setPrefWidth(130);
        e.setMinWidth(130);

        Label v = new Label(valor);
        v.setStyle(F);
        v.setWrapText(true);
        HBox.setHgrow(v, Priority.ALWAYS);

        HBox f = new HBox(4, e, v);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /**
     * Encabezado de la tabla de productos.
     * Mismo ancho fijo que las columnas de datos.
     */
    private void addEncabezadoTabla() {
        // Antes "Cant." medía 60px aquí pero la columna de cantidad
        // real en addFilaProducto media solo 38px -- las columnas
        // P.Unit./Importe del encabezado quedaban corridas 22px a la
        // derecha respecto a los datos de cada fila, un descuadre que
        // también hacía ver la boleta menos legible. Ahora ambos
        // métodos usan las mismas constantes de ancho (ver COL_*).
        Label cod  = col("Código",   COL_CODIGO, true);
        Label desc = new Label("Descripción");
        desc.setStyle(FB);
        HBox.setHgrow(desc, Priority.ALWAYS);
        Label cant  = col("Cant.",    COL_CANT, true);
        Label punit = col("P.Unit.", COL_PUNIT, true);
        Label imp   = col("Importe", COL_IMPORTE, true);

        HBox f = new HBox(4, cod, desc, cant, punit, imp);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Fila de producto: Código | Descripción | Cant. | P.Unit. | Importe */
    private void addFilaProducto(DetalleVenta d) {
        Label cod = col(
                d.getProducto().getIdProducto() != null
                        ? d.getProducto().getIdProducto() : "", COL_CODIGO, false);

        Label desc = new Label(d.getProducto().getNombre());
        desc.setStyle(F);
        desc.setWrapText(true);
        HBox.setHgrow(desc, Priority.ALWAYS);

        Label cant  = col(String.valueOf(d.getCantidad()), COL_CANT, false);
        Label punit = col(String.format(Locale.US, "%.2f", d.getPrecio()), COL_PUNIT, false);
        Label imp   = col(String.format(Locale.US, "%.2f", d.getSubtotal()), COL_IMPORTE, true);

        HBox f = new HBox(4, cod, desc, cant, punit, imp);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Fila de monto con línea de puntos entre el texto y el valor */
    private void addMontoLinea(String texto, String valor, boolean bold) {
        Label lbl = new Label(texto);
        lbl.setStyle(bold ? FB12 : F);

        Label mon = new Label("S/");
        mon.setStyle(bold ? FB12 : F);
        mon.setPrefWidth(20);
        mon.setAlignment(Pos.CENTER_RIGHT);

        Label val = new Label(valor);
        val.setStyle(bold ? FB12 : F);
        val.setPrefWidth(70);
        val.setAlignment(Pos.CENTER_RIGHT);

        Region relleno = new Region();
        HBox.setHgrow(relleno, Priority.ALWAYS);

        HBox f = new HBox(4, lbl, relleno, mon, val);
        f.setAlignment(Pos.BOTTOM_LEFT);
        f.setMaxWidth(Double.MAX_VALUE);
        panelImprimible.getChildren().add(f);
    }
    /** Columna con ancho fijo, alineada a la derecha. */
    private Label col(String texto, double ancho, boolean bold) {
        Label lbl = new Label(texto);
        lbl.setStyle(bold ? FB : F);
        lbl.setPrefWidth(ancho);
        lbl.setMinWidth(ancho);
        lbl.setMaxWidth(ancho);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        return lbl;
    }

    /** Separador simple ─────── */
    private void addSepSimple() {
        Separator sep = new Separator();
        sep.setStyle(
                "-fx-border-color: black;" +
                        "-fx-background-color: black;"
        );
        VBox.setMargin(sep, new Insets(2, 0, 2, 0));
        panelImprimible.getChildren().add(sep);
    }

    private String enmascararDocumento(String doc) {
        if (doc == null || doc.length() <= 4) return doc;
        return doc.substring(0, 4) + "*".repeat(doc.length() - 4);
    }

    private String enmascararNombre(String nombre) {
        if (nombre == null || nombre.length() <= 4) return nombre;
        String visible1 = nombre.substring(0, 2);
        String visible2 = nombre.substring(nombre.length() - 2);
        String oculto   = "*".repeat(nombre.length() - 4);
        return visible1 + oculto + visible2;
    }

    @FXML
    private void imprimir() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;

        boolean ok = job.showPrintDialog(
                panelImprimible.getScene().getWindow());
        if (!ok) return;

        // Se determina el papel DESPUÉS del diálogo, usando la
        // impresora que el usuario efectivamente eligió (puede no
        // ser la impresora por defecto). Intenta un papel angosto
        // tipo ticket/rollo térmico si esa impresora lo soporta
        // (impresoras térmicas reales sí lo tienen). Si no (como
        // "Microsoft Print to PDF", que solo ofrece tamaños
        // estándar), cae de vuelta a A4 -- pero el contenido NO se
        // estira a todo el ancho de la hoja, se mantiene angosto.
        Paper papel = elegirPapelTicket(job.getPrinter());
        PageLayout layout = job.getPrinter().createPageLayout(
                papel,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM
        );

        // Guarda el tamaño y las transformaciones originales del panel
        double anchoOriginal = panelImprimible.getPrefWidth();

        // Antes el ancho se estiraba a TODO el ancho de la página
        // imprimible (A4 completa, ~595pt) sin agrandar la letra --
        // el resultado era texto chico flotando en una hoja casi
        // vacía, muy distinto a como se ve una boleta real entregada
        // en una tienda. Ahora se mantiene un ancho angosto tipo
        // ticket (pensado para una impresora térmica real, aunque hoy
        // se pruebe en A4/Carta): la legibilidad ya la da el tamaño de
        // letra del propio diseño (ver F/FB/FB12 más arriba), así que
        // aquí solo hace falta mantenerlo angosto y centrado.
        double anchoTicket = Math.min(ANCHO_TICKET_IMPRESION, layout.getPrintableWidth());
        panelImprimible.setPrefWidth(anchoTicket);
        panelImprimible.applyCss();
        panelImprimible.layout();

        // Si el contenido es más alto que la hoja (boletas largas con
        // muchos productos), se escala hacia abajo lo necesario para
        // que quepa completo en una sola página, en vez de cortarse.
        double altoContenido = panelImprimible.prefHeight(anchoTicket);
        double escala = 1.0;
        if (altoContenido > layout.getPrintableHeight()) {
            escala = layout.getPrintableHeight() / altoContenido;
        }

        // Centra el ticket horizontalmente en la hoja (si se imprime
        // en A4/Carta completa, va a quedar margen blanco a los
        // costados -- es esperado, el diseño sigue pensado para papel
        // térmico angosto) en vez de dejarlo pegado al margen izquierdo.
        double offsetX = Math.max(0,
                (layout.getPrintableWidth() - anchoTicket * escala) / 2);

        Scale transformEscala = new Scale(escala, escala, 0, 0);
        Translate transformCentrado = new Translate(offsetX, 0);
        panelImprimible.getTransforms().addAll(transformEscala, transformCentrado);

        // Imprime directamente el panel sin moverlo de su contenedor
        // Esto evita que la ventana quede en blanco
        boolean exito = job.printPage(layout, panelImprimible);

        // Restaura el tamaño y quita las transformaciones para que la
        // ventana en pantalla se vea exactamente igual que antes de imprimir
        panelImprimible.getTransforms().removeAll(transformEscala, transformCentrado);
        panelImprimible.setPrefWidth(anchoOriginal);
        panelImprimible.applyCss();
        panelImprimible.layout();

        if (exito) job.endJob();
    }

    /**
     * Busca en la impresora seleccionada un papel angosto tipo
     * ticket/rollo térmico (ancho <= ~250pt, unos 88mm). Si la
     * impresora no ofrece ninguno (caso típico de "Microsoft Print
     * to PDF", que solo trae A4/Carta), usa el papel por defecto
     * -- el ancho del contenido se controla aparte, no aquí.
     */
    private Paper elegirPapelTicket(Printer printer) {
        for (Paper p : printer.getPrinterAttributes().getSupportedPapers()) {
            if (p.getWidth() > 0 && p.getWidth() <= 250) {
                return p;
            }
        }
        return printer.getDefaultPageLayout().getPaper();
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) panelImprimible.getScene().getWindow();
        stage.close();
    }

    //NÚMERO A LETRAS
    private static final String[] UNIDADES = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS",
            "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE",
            "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };
    private static final String[] DECENAS = {
            "", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
            "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
            "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS",
            "NOVECIENTOS"
    };

    private String montoEnLetras(double monto) {
        long e = (long) monto;
        int c = (int) Math.round((monto - e) * 100);
        String p = e == 0 ? "CERO" : convertirEntero(e);
        return p + " CON " + String.format(Locale.US, "%02d", c) + "/100";
    }

    private String convertirEntero(long n) {
        if (n == 0)   return "";
        if (n == 100) return "CIEN";
        if (n < 21)   return UNIDADES[(int) n];
        if (n < 100) {
            int d = (int)(n/10), u = (int)(n%10);
            return u == 0 ? DECENAS[d] : DECENAS[d] + " Y " + UNIDADES[u];
        }
        if (n < 1000) {
            int c2 = (int)(n/100); long r = n%100;
            return r > 0
                    ? CENTENAS[c2] + " " + convertirEntero(r) : CENTENAS[c2];
        }
        if (n < 1_000_000) {
            long m = n/1000, r = n%1000;
            String t = m == 1 ? "MIL" : convertirEntero(m) + " MIL";
            return r > 0 ? t + " " + convertirEntero(r) : t;
        }
        return String.valueOf(n);
    }
}