package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pe.utp.model.DetalleVenta;
import pe.utp.model.Venta;


import java.time.format.DateTimeFormatter;
import java.util.List;

public class VentaDetalleModalController {

    @FXML private VBox panelImprimible;

    // Fuente monoespaciada para alineación perfecta en impresión
    private static final String F  =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 11px; -fx-text-fill: black;";
    private static final String FB =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: black;";
    private static final String FB12 =
            "-fx-font-family: 'Courier New'; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;";

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
        addMontoLinea("OP. GRAVADA",   String.format("%.2f", base),   false);
        addMontoLinea("I.G.V.",        String.format("%.2f", igv),    false);
        addMontoLinea("DESCUENTOS",    String.format("%.2f", descuento), false);
        addSepSimple();
        //Importe Total en negrita con el mismo estilo de línea punteada
        addMontoLinea("IMPORTE TOTAL", String.format("%.2f", total),  true);
        addSepSimple();

        //SON: CENTRADO
        addCentrado("SON: " + montoEnLetras(total) + " SOLES.", 11, true);
        addSepSimple();

        //CANCELÓ CON: CENTRADO
        String metodo = venta.getMetodoPago() != null
                ? venta.getMetodoPago().getMetodoDePago().toUpperCase() : "";

        //Efectivo: muestra redondeo, total y texto
        if (metodo.equals("EFECTIVO") && venta.getMontoPagado() > 0) {
            addMonto("REDONDEO S/", "",
                    String.format("%.2f", 0.00));
            addMonto("TOTAL S/", "",
                    String.format("%.2f", total));
            addCentrado(
                    "EFECTIVO S/: " +
                            String.format("%.2f", venta.getMontoPagado()) + " SOLES",
                    11, false);
            addMontoBold("Vuelto", "S/",
                    String.format("%.2f", venta.getVuelto()));
        } else {
            //Tarjeta/Yape/Transferencia: formato "ONLINE VISA S/: X.XX SOLES"
            String cancelo = metodo.contains("TARJETA")
                    ? metodo.replace("TARJETA DE ", "") + " S/: " +
                    String.format("%.2f", total) + " SOLES"
                    : metodo + " S/: " +
                    String.format("%.2f", total) + " SOLES";
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
        Label cod  = col("Código",   55, true);
        Label desc = new Label("Descripción");
        desc.setStyle(FB);
        HBox.setHgrow(desc, Priority.ALWAYS);
        Label cant  = col("Cant.",    60, true);
        Label punit = col("P.Unit.", 60, true);
        Label imp   = col("Importe", 65, true);

        HBox f = new HBox(4, cod, desc, cant, punit, imp);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Fila de producto: Código | Descripción | Cant. | P.Unit. | Importe */
    private void addFilaProducto(DetalleVenta d) {
        Label cod = col(
                d.getProducto().getIdProducto() != null
                        ? d.getProducto().getIdProducto() : "", 55, false);

        Label desc = new Label(d.getProducto().getNombre());
        desc.setStyle(F);
        desc.setWrapText(true);
        HBox.setHgrow(desc, Priority.ALWAYS);

        Label cant  = col(String.valueOf(d.getCantidad()), 38, false);
        Label punit = col(String.format("%.3f", d.getPrecio()), 60, false);
        Label imp   = col(String.format("%.3f", d.getSubtotal()), 65, true);

        HBox f = new HBox(4, cod, desc, cant, punit, imp);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Fila de monto: texto izq, "S/" centro-izq, valor a la derecha.
    Replica exactamente el formato de la imagen de referencia. */
    private void addMonto(String texto, String moneda, String valor) {
        Label lbl = new Label(texto);
        lbl.setStyle(F);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Label mon = new Label(moneda);
        mon.setStyle(F);
        mon.setPrefWidth(20);
        mon.setAlignment(Pos.CENTER_RIGHT);

        Label val = new Label(valor);
        val.setStyle(F);
        val.setPrefWidth(70);
        val.setAlignment(Pos.CENTER_RIGHT);

        HBox f = new HBox(4, lbl, mon, val);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Igual que addMonto pero en negrita. */
    private void addMontoBold(String texto, String moneda, String valor) {
        Label lbl = new Label(texto);
        lbl.setStyle(FB12);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Label mon = new Label(moneda);
        mon.setStyle(FB12);
        mon.setPrefWidth(20);
        mon.setAlignment(Pos.CENTER_RIGHT);

        Label val = new Label(valor);
        val.setStyle(FB12);
        val.setPrefWidth(70);
        val.setAlignment(Pos.CENTER_RIGHT);

        HBox f = new HBox(4, lbl, mon, val);
        f.setAlignment(Pos.CENTER_LEFT);
        panelImprimible.getChildren().add(f);
    }

    /** Fila de monto con línea de puntos entre el texto y el valor */
    private void addMontoLinea(String texto, String valor, boolean bold) {
        // Construye el relleno de puntos dinámicamente
        // El ancho total aproximado en caracteres monoespaciados es 42

        Label lbl = new Label(texto);
        lbl.setStyle(bold ? FB12 : F);

        //Relleno de puntos entre etiqueta y valor
        Label puntos = new Label();
        puntos.setStyle(F);
        HBox.setHgrow(puntos, Priority.ALWAYS);
        // Alineación del relleno: genera una línea punteada visual
        puntos.setMaxWidth(Double.MAX_VALUE);
        puntos.setText(" "); // espacio base, los puntos se ven por el separator

        Label mon = new Label("S/");
        mon.setStyle(bold ? FB12 : F);
        mon.setPrefWidth(20);
        mon.setAlignment(Pos.CENTER_RIGHT);

        Label val = new Label(valor);
        val.setStyle(bold ? FB12 : F);
        val.setPrefWidth(70);
        val.setAlignment(Pos.CENTER_RIGHT);

        //Separador punteado que se estira
        Region relleno = new Region();
        relleno.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(relleno, Priority.ALWAYS);

        HBox f = new HBox(4, lbl, relleno, mon, val);
        f.setAlignment(Pos.BOTTOM_LEFT);
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

        PageLayout layout = job.getPrinter().createPageLayout(
                Paper.A4,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM
        );

        // Guarda el tamaño original del panel
        double anchoOriginal = panelImprimible.getPrefWidth();

        // Ajusta temporalmente el ancho al de la página imprimible
        // para que el contenido se escale correctamente al imprimir
        double anchoPagina = layout.getPrintableWidth();
        panelImprimible.setPrefWidth(anchoPagina);
        panelImprimible.applyCss();
        panelImprimible.layout();

        // Imprime directamente el panel sin moverlo de su contenedor
        // Esto evita que la ventana quede en blanco
        boolean exito = job.printPage(layout, panelImprimible);

        // Restaura el ancho original para que la ventana siga bien
        panelImprimible.setPrefWidth(anchoOriginal);
        panelImprimible.applyCss();
        panelImprimible.layout();

        if (exito) job.endJob();
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
        return p + " CON " + String.format("%02d", c) + "/100";
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