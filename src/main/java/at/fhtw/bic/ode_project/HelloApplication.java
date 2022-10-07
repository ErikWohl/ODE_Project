package at.fhtw.bic.ode_project;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {


        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        initCanvas(stage);
    }

    public void initCanvas(Stage stage) {

        var scene = stage.getScene();

        var canvas = (Canvas) scene.lookup("#canvas1");

        var gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);

        //todo: https://docs.oracle.com/javafx/2/events/filters.htm
        // https://stackoverflow.com/questions/46649406/custom-javafx-events
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                gc.beginPath();

                String msg =
                        "(x: "       + mouseEvent.getX()      + ", y: "       + mouseEvent.getY()       + ") --\n" +
                        "(sceneX: "  + mouseEvent.getSceneX() + ", sceneY: "  + mouseEvent.getSceneY()  + ") --\n" +
                        "(screenX: " + mouseEvent.getScreenX()+ ", screenY: " + mouseEvent.getScreenY() + ")";
                //System.out.printf("mouse was pressed!\n%s\n", msg);
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
                //System.out.printf("mouse was dragged!\n%s\n", msg);
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
                //System.out.printf("mouse was released!\n%s\n", msg);
            }
        });

        var colorPicker = (ColorPicker) scene.lookup("#colorPicker");

        // create a event handler
        EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e)
            {
                // color
                Color c = colorPicker.getValue();
                gc.setStroke(c);
            }
        };

        // set listener
        colorPicker.setOnAction(event);

        stage.setTitle("Lines");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}