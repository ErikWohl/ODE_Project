package at.fhtw.bic.ode_project.Controller;

import at.fhtw.bic.ode_project.HelloApplication;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class IndexController {
    @FXML
    private TextField usernameTextField;
    @FXML
    private TextField hostTextField;
    @FXML
    private TextField portTextField;

    @FXML
    protected void onStartButtonClick(Event event) {
        String username = "example";
        String host = "localhost";
        int port = 8080;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if(!usernameTextField.getText().isEmpty() && !usernameTextField.getText().isBlank()) {
            username = usernameTextField.getText();
        }


        if(!hostTextField.getText().isEmpty() && !hostTextField.getText().isBlank()) {
            host = hostTextField.getText();
        }
        if(!portTextField.getText().isEmpty() && !portTextField.getText().isBlank()) {
            try {
                port = Integer.parseInt(portTextField.getText());
            } catch (NumberFormatException e) {
                alert.setContentText("PORT IS ERRONEOUS!");
                alert.show();
            }
            if(port < 1 || port > 65535) {
                alert.setContentText("PORT IS ERRONEOUS!");
                alert.show();
            }
        }

        try {
            URL applicationFXML = HelloApplication.class.getResource("hello-view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(applicationFXML);
            Parent root = fxmlLoader.load();
            HelloController controller = fxmlLoader.getController();

            Stage stage = new Stage();
            stage.setTitle("Skribbl.io");

            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            ((Node)(event.getSource())).getScene().getWindow().hide();

            controller.initUI(stage, username, host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
