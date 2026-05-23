package pe.utp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.utp.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("JavaFX funcionando!");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);
        stage.setTitle("PC Configurador");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // Probar conexión e insertar categoría
        try {
            Connection conn = ConexionDB.getConexion();
            if (conn != null) {
                String sql = "INSERT INTO categoria VALUES ('CAT001', 'Procesador')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.executeUpdate();
                System.out.println("Categoria insertada correctamente!");
                conn.close();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        launch(args);
    }
}