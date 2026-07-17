package pe.utp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pe.utp.dao.EmpleadoDAO;
import pe.utp.model.Empleado;
import pe.utp.security.Rol;
import pe.utp.security.Sesion;

public class LoginController {
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblError;

    private EmpleadoDAO dao = new EmpleadoDAO();

    @FXML
    private void login() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError("Completa todos los campos");
            return;
        }
        Empleado empleado = dao.login(usuario, password);

        if (empleado != null) {
            // Antes esta limpieza corría ANTES de saber si el login
            // fue exitoso, así que cualquier intento fallido (incluso
            // spam de credenciales incorrectas) disparaba de todas
            // formas un mantenimiento real sobre la BD (borra
            // cotizaciones vencidas). Ahora solo corre tras un login
            // válido.
            new pe.utp.dao.CotizacionDAO().limpiarCotizacionesVencidas();

            if (Rol.desdeCargo(empleado.getCargo()) == null) {
                mostrarError("Cargo no reconocido. Contacta al administrador.");
                return;
            }
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/Main.fxml"));
                Parent root = loader.load();

                MainController mainController = loader.getController();
                mainController.setEmpleado(empleado);

                Stage stage = (Stage) txtUsuario.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (Exception e) {
                System.out.println("Error al cargar main" + e.getMessage());
            }
        } else {
            mostrarError("Usuario o contraseña incorrecto");
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

}
