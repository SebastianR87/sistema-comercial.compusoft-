package pe.utp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.utp.Conexion.ConexionDB;
import pe.utp.dao.CategoriaDAO;
import pe.utp.model.Categoria;
import java.util.List;

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

        // PROBAR CategoriaDAO
        CategoriaDAO dao = new CategoriaDAO();

        // Insertar
        Categoria c = new Categoria("CAT010", "Monitor");
        boolean resultado = dao.insertar(c);
        System.out.println("Insertar: " + resultado);

        // Listar
        List<Categoria> lista = dao.Listar();
        for (Categoria cat : lista) {
            System.out.println(cat.getIdCategoria() + " - " + cat.getNombre());
        }

        launch(args);
    }
}