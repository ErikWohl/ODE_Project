package at.fhtw.bic.ode_project;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        /*
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
         */
        initUI(stage);
    }

    public void initUI(Stage stage) {

        var root = new Pane();

        var canvas = new Canvas(300, 300);
        root.getChildren().add(canvas);

        var gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);

        var scene = new Scene(root, 300, 250, Color.WHITESMOKE);


        //todo: https://docs.oracle.com/javafx/2/events/filters.htm
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                gc.beginPath();

                String msg =
                        "(x: "       + mouseEvent.getX()      + ", y: "       + mouseEvent.getY()       + ") --\n" +
                        "(sceneX: "  + mouseEvent.getSceneX() + ", sceneY: "  + mouseEvent.getSceneY()  + ") --\n" +
                        "(screenX: " + mouseEvent.getScreenX()+ ", screenY: " + mouseEvent.getScreenY() + ")";
                System.out.printf("mouse was pressed!\n%s\n", msg);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {


                gc.lineTo(mouseEvent.getX(), mouseEvent.getY());
                gc.stroke();
                gc.moveTo(mouseEvent.getX(), mouseEvent.getY());

                String msg =
                        "(x: "       + mouseEvent.getX()      + ", y: "       + mouseEvent.getY()       + ") --\n" +
                        "(sceneX: "  + mouseEvent.getSceneX() + ", sceneY: "  + mouseEvent.getSceneY()  + ") --\n" +
                        "(screenX: " + mouseEvent.getScreenX()+ ", screenY: " + mouseEvent.getScreenY() + ")";
                System.out.printf("mouse was dragged!\n%s\n", msg);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                gc.closePath();

                String msg =
                        "(x: "       + mouseEvent.getX()      + ", y: "       + mouseEvent.getY()       + ") --\n" +
                        "(sceneX: "  + mouseEvent.getSceneX() + ", sceneY: "  + mouseEvent.getSceneY()  + ") --\n" +
                        "(screenX: " + mouseEvent.getScreenX()+ ", screenY: " + mouseEvent.getScreenY() + ")";
                System.out.printf("mouse was released!\n%s\n", msg);
            }
        });
        stage.setTitle("Lines");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}