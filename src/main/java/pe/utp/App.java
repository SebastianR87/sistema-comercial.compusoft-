package pe.utp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pe.utp.controller.MainController;
import pe.utp.model.Empleado;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Main.fxml")
        );
        Parent root = loader.load();

        // Empleado de prueba hasta que hagas el Login
        MainController controller = loader.getController();
        Empleado empleado = new Empleado();
        empleado.setNombre("Sebastian Rondo");
        empleado.setCargo("empleado");
        controller.setEmpleado(empleado);

        Scene scene = new Scene(root);
        stage.setTitle("COMPUSOFT - Sistema Comercial");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}