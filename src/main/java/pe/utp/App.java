package pe.utp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pe.utp.dialog.VentanaPrincipal;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        VentanaPrincipal.registrar(stage);

        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/Login.fxml")
        );
        Scene scene = new Scene(root);
        stage.setTitle("COMPUSOFT - Sistema Comercial");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}