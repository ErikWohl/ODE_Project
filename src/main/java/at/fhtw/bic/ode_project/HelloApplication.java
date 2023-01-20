package at.fhtw.bic.ode_project;

import at.fhtw.bic.ode_project.Controller.HelloController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL applicationFXML = HelloApplication.class.getResource("hello-view.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(applicationFXML);


        Parent root = fxmlLoader.load();
        HelloController controller = fxmlLoader.getController();


        Scene scene = new Scene(root);
        stage.setScene(scene);

        controller.setStage(stage);
        controller.initUI(stage);
    }
    //todo: https://docs.oracle.com/javafx/2/events/filters.htm
    // https://stackoverflow.com/questions/46649406/custom-javafx-events
    // https://docs.oracle.com/javafx/2/events/processing.htm

    public static void main(String[] args) {
        launch();
    }
}